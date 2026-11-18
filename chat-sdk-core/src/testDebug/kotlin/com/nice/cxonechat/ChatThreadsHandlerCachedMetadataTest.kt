/*
 * Copyright (c) 2021-2026. NICE Ltd. All rights reserved.
 *
 * Licensed under the NICE License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/nice-devone/nice-cxone-mobile-sdk-android/blob/main/LICENSE
 *
 * TO THE EXTENT PERMITTED BY APPLICABLE LAW, THE CXONE MOBILE SDK IS PROVIDED ON
 * AN “AS IS” BASIS. NICE HEREBY DISCLAIMS ALL WARRANTIES AND CONDITIONS, EXPRESS
 * OR IMPLIED, INCLUDING (WITHOUT LIMITATION) WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT, AND TITLE.
 */

@file:Suppress("FunctionMaxLength")

package com.nice.cxonechat

import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.network.EventCaseStatusChanged.CaseStatus.Closed
import com.nice.cxonechat.model.makeAgent
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.model.makeMessageModel
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState
import com.nice.cxonechat.thread.ChatThreadState.Loaded
import com.nice.cxonechat.thread.ChatThreadState.Received
import com.nice.cxonechat.thread.ContactHandler.Cognigy
import kotlinx.coroutines.launch
import org.junit.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * The contact metadata (`contactId`, `contactHandler`, `threadState`) that the multi-thread [ChatThreadsHandler]'s
 * cache watcher carries into `threadsFlow`, and how it merges with the thread list's own values. The messages cache
 * itself lives in [ChatThreadsHandlerMessagesCacheTest].
 */
internal class ChatThreadsHandlerCachedMetadataTest : AbstractChatThreadsHandlerTest() {

    @Test
    fun `threadsFlow carries contactId and threadState from a MessageCreated for a cached thread`() {
        val thread = makeChatThread(
            id = UUID.fromString("cc000000-0000-0000-0000-00000000000c"),
            threadState = Received,
            contactId = null,
        )
        reconnect()
        val results = mutableListOf<List<ChatThread>>()
        val job = testScope.launch { threads.threadsFlow.collect { results.add(it) } }

        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf(thread)))
        threads.thread(thread)
        socketServer.sendServerMessage(
            ServerResponse.MessageCreated(thread, makeMessageModel(threadIdOnExternalPlatform = thread.id))
        )
        job.cancel()

        val updatedThread = results.last().find { it.id == thread.id }
        assertNotNull(updatedThread)
        assertEquals(TestContactId, updatedThread.contactId)
        assertEquals(ChatThreadState.Ready, updatedThread.threadState)
    }

    @Test
    fun `threadsFlow delivers Closed from a MessageCreated that closes a cached thread`() {
        val thread = makeChatThread(
            id = UUID.fromString("cc000000-0000-0000-0000-00000000000d"),
            threadState = Received,
            contactId = null,
        )
        reconnect()
        val results = mutableListOf<List<ChatThread>>()
        val job = testScope.launch { threads.threadsFlow.collect { results.add(it) } }

        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf(thread)))
        threads.thread(thread)
        socketServer.sendServerMessage(
            ServerResponse.MessageCreated(
                thread,
                makeMessageModel(threadIdOnExternalPlatform = thread.id),
                status = "closed",
            )
        )
        job.cancel()

        val updatedThread = results.last().find { it.id == thread.id }
        assertNotNull(updatedThread)
        assertEquals(ChatThreadState.Closed, updatedThread.threadState)
        assertEquals(1, updatedThread.messages.size)
    }

    @Test
    fun `threadsFlow keeps Closed from the thread list over an older cached state`() {
        val thread = makeChatThread(
            id = UUID.fromString("cc000000-0000-0000-0000-00000000000e"),
            threadState = Received,
            contactId = null,
        )
        reconnect()
        val results = mutableListOf<List<ChatThread>>()
        val job = testScope.launch { threads.threadsFlow.collect { results.add(it) } }

        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf(thread)))
        threads.thread(thread)
        socketServer.sendServerMessage(
            ServerResponse.MessageCreated(thread, makeMessageModel(threadIdOnExternalPlatform = thread.id))
        )
        socketServer.sendServerMessage(ServerResponse.CaseStatusChanged(thread, Closed))
        job.cancel()

        val updatedThread = results.last().find { it.id == thread.id }
        assertNotNull(updatedThread)
        assertEquals(ChatThreadState.Closed, updatedThread.threadState)
    }

    @Test
    fun `threadsFlow keeps Closed and the contact from a MessageCreated after the thread list is fetched again`() {
        val thread = makeChatThread(
            id = UUID.fromString("cc000000-0000-0000-0000-000000000020"),
            threadState = Received,
            contactId = null,
        )
        val results = collectAfterMessageCreated(thread, status = "closed")

        refetchThreadList(thread)

        val updatedThread = assertNotNull(results.last().find { it.id == thread.id })
        assertEquals(ChatThreadState.Closed, updatedThread.threadState)
        assertEquals(TestContactId, updatedThread.contactId)
    }

    @Test
    fun `threadsFlow keeps the state and contact of an open thread from a MessageCreated after the list is fetched again`() {
        val thread = makeChatThread(
            id = UUID.fromString("cc000000-0000-0000-0000-000000000021"),
            threadState = Received,
            contactId = null,
        )
        val results = collectAfterMessageCreated(thread)

        refetchThreadList(thread)

        val updatedThread = assertNotNull(results.last().find { it.id == thread.id })
        assertEquals(ChatThreadState.Ready, updatedThread.threadState)
        assertEquals(TestContactId, updatedThread.contactId)
    }

    @Test
    fun `threadsFlow keeps Closed from the thread list when a snapshot from before the close is opened`() {
        val thread = makeChatThread(
            id = UUID.fromString("cc000000-0000-0000-0000-000000000022"),
            threadState = Received,
            contactId = null,
        )
        reconnect()
        val results = mutableListOf<List<ChatThread>>()
        val job = testScope.launch { threads.threadsFlow.collect { results.add(it) } }
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf(thread)))
        val snapshot = assertNotNull(results.last().find { it.id == thread.id }).asCopyable().copy()
        socketServer.sendServerMessage(ServerResponse.CaseStatusChanged(thread, Closed))

        threads.thread(snapshot)
        job.cancel()

        val updatedThread = assertNotNull(results.last().find { it.id == thread.id })
        assertEquals(ChatThreadState.Closed, updatedThread.threadState)
    }

    @Test
    fun `threadsFlow carries the contactHandler from a MessageCreated for a cached thread`() {
        val thread = makeChatThread(
            id = UUID.fromString("cc000000-0000-0000-0000-000000000023"),
            threadState = Received,
            contactId = null,
        )
        val results = collectAfterMessageCreated(thread, handler = "cognigy")

        val updatedThread = assertNotNull(results.last().find { it.id == thread.id })
        assertEquals(Cognigy, updatedThread.contactHandler)
    }

    @Test
    fun `threadsFlow keeps the contactHandler from a MessageCreated after the thread list is fetched again`() {
        val thread = makeChatThread(
            id = UUID.fromString("cc000000-0000-0000-0000-000000000024"),
            threadState = Received,
            contactId = null,
        )
        val results = collectAfterMessageCreated(thread, handler = "cognigy")

        refetchThreadList(thread)

        val updatedThread = assertNotNull(results.last().find { it.id == thread.id })
        assertEquals(Cognigy, updatedThread.contactHandler)
    }

    @Test
    fun `threadsFlow keeps Closed from the thread list when metadata loads again for a snapshot from before the close`() {
        val thread = makeChatThread(
            id = UUID.fromString("cc000000-0000-0000-0000-000000000026"),
            threadState = Received,
            contactId = null,
        )
        reconnect()
        val results = mutableListOf<List<ChatThread>>()
        val job = testScope.launch { threads.threadsFlow.collect { results.add(it) } }
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf(thread)))
        val snapshot = assertNotNull(results.last().find { it.id == thread.id }).asCopyable().copy()
        val metadata = ServerResponse.ThreadMetadataLoaded(
            agent = makeAgent(),
            message = makeMessageModel(threadIdOnExternalPlatform = thread.id),
        )
        socketServer.sendServerMessage(metadata)
        socketServer.sendServerMessage(ServerResponse.CaseStatusChanged(thread, Closed))

        threads.thread(snapshot)
        socketServer.sendServerMessage(metadata)
        job.cancel()

        val updatedThread = assertNotNull(results.last().find { it.id == thread.id })
        assertEquals(ChatThreadState.Closed, updatedThread.threadState)
    }

    @Test
    fun `threadsFlow keeps Closed from the thread list over an older cached state after the list is fetched again`() {
        val thread = makeChatThread(
            id = UUID.fromString("cc000000-0000-0000-0000-000000000027"),
            threadState = Received,
            contactId = null,
        )
        val results = collectAfterMessageCreated(thread)
        socketServer.sendServerMessage(ServerResponse.CaseStatusChanged(thread, Closed))

        refetchThreadList(thread)

        val updatedThread = assertNotNull(results.last().find { it.id == thread.id })
        assertEquals(ChatThreadState.Closed, updatedThread.threadState)
        assertEquals(TestContactId, updatedThread.contactId)
    }

    @Test
    fun `threadsFlow shows Loaded once metadata loads for a cached thread`() {
        val thread = makeChatThread(
            id = UUID.fromString("cc000000-0000-0000-0000-00000000000f"),
            threadState = Received,
            contactId = null,
        )
        reconnect()
        val results = mutableListOf<List<ChatThread>>()
        val job = testScope.launch { threads.threadsFlow.collect { results.add(it) } }

        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf(thread)))
        threads.thread(thread)
        // The watcher caches an emission before the metadata arrives; the list must still advance to Loaded
        socketServer.sendServerMessage(ServerResponse.TypingStarted(thread))
        socketServer.sendServerMessage(
            ServerResponse.ThreadMetadataLoaded(
                agent = makeAgent(),
                message = makeMessageModel(threadIdOnExternalPlatform = thread.id),
            )
        )
        job.cancel()

        val updatedThread = results.last().find { it.id == thread.id }
        assertNotNull(updatedThread)
        assertEquals(Loaded, updatedThread.threadState)
    }

    @Test
    fun `threadsFlow does not re-emit for a MessageCreated that changes nothing`() {
        val thread = makeChatThread(
            id = UUID.fromString("cc000000-0000-0000-0000-000000000010"),
            threadState = Received,
            contactId = null,
        )
        val messageModel = makeMessageModel(threadIdOnExternalPlatform = thread.id)
        reconnect()
        val results = mutableListOf<List<ChatThread>>()
        val job = testScope.launch { threads.threadsFlow.collect { results.add(it) } }

        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf(thread)))
        threads.thread(thread)
        socketServer.sendServerMessage(ServerResponse.MessageCreated(thread, messageModel))
        val emissionsAfterFirst = results.size
        socketServer.sendServerMessage(ServerResponse.MessageCreated(thread, messageModel))
        job.cancel()

        assertEquals(emissionsAfterFirst, results.size, "An echo that changes nothing must not re-emit threadsFlow")
    }
}
