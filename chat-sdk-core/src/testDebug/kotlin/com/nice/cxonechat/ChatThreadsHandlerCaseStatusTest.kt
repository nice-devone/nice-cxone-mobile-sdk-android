/*
 * Copyright (c) 2026. NICE Ltd. All rights reserved.
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

import com.nice.cxonechat.internal.model.ChatThreadMutable
import com.nice.cxonechat.internal.model.network.EventCaseStatusChanged.CaseStatus.Closed
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.model.makeMessageModel
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState
import com.nice.cxonechat.thread.ChatThreadState.Received
import com.nice.cxonechat.thread.ContactHandler
import kotlinx.coroutines.launch
import org.junit.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

/**
 * How `threadsFlow` and an open thread handler of the multi-thread [ChatThreadsHandler] follow a thread that closes
 * and opens again: a case closed by CaseStatusChanged or MessageCreated, then a recovery or a new message. The
 * metadata merge itself lives in [ChatThreadsHandlerCachedMetadataTest].
 */
internal class ChatThreadsHandlerCaseStatusTest : AbstractChatThreadsHandlerTest() {

    @Test
    fun `threadsFlow shows a thread closed by MessageCreated as Ready again once its handler recovers it`() {
        val thread = makeChatThread(
            id = UUID.fromString("cc000000-0000-0000-0000-000000000025"),
            threadState = Received,
            contactId = null,
        )
        val results = collectAfterMessageCreated(thread, status = "closed")
        val closed = assertNotNull(results.last().find { it.id == thread.id })
        assertEquals(ChatThreadState.Closed, closed.threadState, "Precondition: MessageCreated closes the listed thread")

        socketServer.sendServerMessage(ServerResponse.ThreadRecovered(thread = thread))

        assertEquals(ChatThreadState.Ready, threads.thread(thread).get().threadState, "Precondition: the handler recovered")
        val updatedThread = assertNotNull(results.last().find { it.id == thread.id })
        assertEquals(ChatThreadState.Ready, updatedThread.threadState)
    }

    @Test
    fun `threadsFlow shows Closed from the thread list when a refetched thread closes again after a recovery`() {
        val thread = makeChatThread(
            id = UUID.fromString("cc000000-0000-0000-0000-000000000028"),
            threadState = Received,
            contactId = null,
        )
        val results = collectAfterMessageCreated(thread, status = "closed")
        socketServer.sendServerMessage(ServerResponse.CaseStatusChanged(thread, Closed))
        socketServer.sendServerMessage(ServerResponse.ThreadRecovered(thread = thread))
        refetchThreadList(thread)

        socketServer.sendServerMessage(ServerResponse.CaseStatusChanged(thread, Closed))

        val updatedThread = assertNotNull(results.last().find { it.id == thread.id })
        assertEquals(ChatThreadState.Closed, updatedThread.threadState)
    }

    @Test
    fun `an open thread handler shows Closed once CaseStatusChanged closes its case`() {
        val thread = makeChatThread(
            id = UUID.fromString("cc000000-0000-0000-0000-000000000029"),
            threadState = Received,
            contactId = null,
        )
        collectAfterMessageCreated(thread)

        socketServer.sendServerMessage(ServerResponse.CaseStatusChanged(thread, Closed))

        val handled = threads.thread(thread).get()
        assertEquals(ChatThreadState.Closed, handled.threadState)
        assertEquals(false, handled.canAddMoreMessages)
    }

    @Test
    fun `threadFlow of a handler opened from a listed thread emits Closed once CaseStatusChanged closes its case`() {
        val thread = makeChatThread(
            id = UUID.fromString("cc000000-0000-0000-0000-00000000002c"),
            threadState = Received,
            contactId = null,
        )
        reconnect()
        val listedThreads = mutableListOf<List<ChatThread>>()
        testScope.launch { threads.threadsFlow.collect { listedThreads.add(it) } }
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf(thread)))
        socketServer.sendServerMessage(
            ServerResponse.ThreadMetadataLoaded(message = makeMessageModel(threadIdOnExternalPlatform = thread.id))
        )
        val listed = assertNotNull(listedThreads.last().find { it.id == thread.id })
        assertIs<ChatThreadMutable>(listed, "Precondition: the handler shares the listed thread")
        val emitted = mutableListOf<ChatThread>()
        testScope.launch { threads.thread(listed).threadFlow.collect { emitted.add(it) } }

        socketServer.sendServerMessage(ServerResponse.CaseStatusChanged(thread, Closed))

        val last = emitted.last()
        assertEquals(ChatThreadState.Closed, last.threadState)
        assertEquals(false, last.canAddMoreMessages)
    }

    @Test
    fun `an open thread handler applies the contact handler of a repeated CaseStatusChanged that closes its case`() {
        val thread = makeChatThread(
            id = UUID.fromString("cc000000-0000-0000-0000-00000000002d"),
            threadState = Received,
            contactId = null,
        )
        collectAfterMessageCreated(thread)
        socketServer.sendServerMessage(ServerResponse.CaseStatusChanged(thread, Closed, handler = "cxone"))

        socketServer.sendServerMessage(ServerResponse.CaseStatusChanged(thread, Closed, handler = "cognigy"))

        val handled = threads.thread(thread).get()
        assertEquals(ChatThreadState.Closed, handled.threadState)
        assertEquals(ContactHandler.Cognigy, handled.contactHandler)
    }

    @Test
    fun `a recovery takes the case id of the server snapshot`() {
        val thread = makeChatThread(
            id = UUID.fromString("cc000000-0000-0000-0000-0000000000f0"),
            threadState = Received,
            contactId = null,
        )
        collectAfterMessageCreated(thread)
        socketServer.sendServerMessage(ServerResponse.CaseStatusChanged(thread, Closed))

        socketServer.sendServerMessage(ServerResponse.ThreadRecovered(thread = makeChatThread(id = thread.id, contactId = NEW_CASE_ID)))

        assertEquals(NEW_CASE_ID, threads.thread(thread).get().contactId)
    }

    @Test
    fun `threadsFlow shows Ready when a recovery reopens a thread closed by CaseStatusChanged, also after a refresh`() {
        val thread = makeChatThread(
            id = UUID.fromString("cc000000-0000-0000-0000-00000000002a"),
            threadState = Received,
            contactId = null,
        )
        val results = collectAfterMessageCreated(thread)
        socketServer.sendServerMessage(ServerResponse.CaseStatusChanged(thread, Closed))

        socketServer.sendServerMessage(ServerResponse.ThreadRecovered(thread = thread))

        assertEquals(true, threads.thread(thread).get().canAddMoreMessages, "The recovered handler accepts messages again")
        val recovered = assertNotNull(results.last().find { it.id == thread.id })
        assertEquals(ChatThreadState.Ready, recovered.threadState)
        assertEquals(true, recovered.canAddMoreMessages, "The listed thread accepts messages again")
        refetchThreadList(thread)
        val refetched = assertNotNull(results.last().find { it.id == thread.id })
        assertEquals(ChatThreadState.Ready, refetched.threadState)
        assertEquals(true, refetched.canAddMoreMessages, "The refetched thread accepts messages")
    }

    @Test
    fun `threadsFlow shows Ready when a message on a new case reopens a thread closed by CaseStatusChanged`() {
        val thread = makeChatThread(
            id = UUID.fromString("cc000000-0000-0000-0000-00000000002b"),
            threadState = Received,
            contactId = null,
        )
        val results = collectAfterMessageCreated(thread)
        socketServer.sendServerMessage(ServerResponse.CaseStatusChanged(thread, Closed))

        socketServer.sendServerMessage(
            ServerResponse.MessageCreated(
                thread,
                makeMessageModel(threadIdOnExternalPlatform = thread.id),
                contactId = "newCaseId",
            )
        )

        assertEquals(true, threads.thread(thread).get().canAddMoreMessages, "The reopened handler accepts messages again")
        val reopened = assertNotNull(results.last().find { it.id == thread.id })
        assertEquals(ChatThreadState.Ready, reopened.threadState)
        assertEquals(true, reopened.canAddMoreMessages, "The listed thread accepts messages again")
    }

    @Test
    fun `threadsFlow keeps a thread closed when a stale message on the closed case arrives`() {
        val thread = makeChatThread(
            id = UUID.fromString("cc000000-0000-0000-0000-00000000002c"),
            threadState = Received,
            contactId = null,
        )
        val results = collectAfterMessageCreated(thread)
        socketServer.sendServerMessage(ServerResponse.CaseStatusChanged(thread, Closed))

        socketServer.sendServerMessage(
            ServerResponse.MessageCreated(thread, makeMessageModel(threadIdOnExternalPlatform = thread.id), status = "resolved")
        )

        assertEquals(false, threads.thread(thread).get().canAddMoreMessages, "The closed handler still rejects messages")
        val listed = assertNotNull(results.last().find { it.id == thread.id })
        assertEquals(ChatThreadState.Closed, listed.threadState)
        assertEquals(false, listed.canAddMoreMessages, "The listed thread still rejects messages")
    }

    private companion object {
        const val NEW_CASE_ID = "newCaseId"
    }
}
