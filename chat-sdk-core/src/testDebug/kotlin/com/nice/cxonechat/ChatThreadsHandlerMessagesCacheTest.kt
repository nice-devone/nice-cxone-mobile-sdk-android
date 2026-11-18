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

import com.nice.cxonechat.internal.model.network.EventCaseStatusChanged.CaseStatus.Closed
import com.nice.cxonechat.model.makeAgent
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.model.makeMessageModel
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState.Received
import kotlinx.coroutines.launch
import org.junit.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Messages-cache preservation, eviction, and watcher-lifecycle behavior of the multi-thread
 * [ChatThreadsHandler]. Handler identity lives in [ChatThreadsHandlerTest]; `threadsFlow` emission
 * in [ChatThreadsHandlerFlowTest]; metadata loading in [ChatThreadsHandlerMetadataTest]; the contact metadata the
 * cache carries into `threadsFlow` in [ChatThreadsHandlerCachedMetadataTest].
 */
internal class ChatThreadsHandlerMessagesCacheTest : AbstractChatThreadsHandlerTest() {

    @Test
    fun threadsFlow_messagesPreservedFromCache_whenNewThreadListHasNoMessages() {
        val thread = makeChatThread(
            id = UUID.fromString("cc000000-0000-0000-0000-000000000001"),
            threadState = Received,
            contactId = null,
        )
        val agentModel = makeAgent()
        val message = makeMessageModel(threadIdOnExternalPlatform = thread.id)
        reconnect()
        val results = mutableListOf<List<ChatThread>>()
        val job = testScope.launch { threads.threadsFlow.collect { results.add(it) } }

        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf(thread)))
        // Start the cache watcher so ChatThreadsHandlerMessages populates its cache
        threads.thread(thread)
        socketServer.sendServerMessage(ServerResponse.ThreadMetadataLoaded(agent = agentModel, message = message))
        // Second fetch returns threads without message history
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf(thread)))
        job.cancel()

        val cachedThread = results.last().find { it.id == thread.id }
        assertNotNull(cachedThread, "Thread must be present after second fetch")
        assertTrue(cachedThread.messages.isNotEmpty(), "Messages must be preserved from ChatThreadsHandlerMessages cache")
    }

    @Test
    fun `threadsFlow re-emits when a message arrives for a cached thread without a follow-up ThreadListFetched`() {
        val thread = makeChatThread(
            id = UUID.fromString("cc000000-0000-0000-0000-000000000002"),
            threadState = Received,
            contactId = null,
        )
        val messageModel = makeMessageModel(threadIdOnExternalPlatform = thread.id)
        reconnect()
        val results = mutableListOf<List<ChatThread>>()
        val job = testScope.launch { threads.threadsFlow.collect { results.add(it) } }

        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf(thread)))
        // Start the cache watcher so ChatThreadsHandlerMessages observes EventMessageCreated for this thread
        threads.thread(thread)
        val emissionsBeforeMessage = results.size

        // A new message arrives while the list is on screen — no follow-up ThreadListFetched
        socketServer.sendServerMessage(ServerResponse.MessageCreated(thread, messageModel))
        job.cancel()

        assertTrue(
            results.size > emissionsBeforeMessage,
            "threadsFlow must re-emit when a cached thread receives a new message, " +
                    "even without a follow-up ThreadListFetched"
        )
        val updatedThread = results.last().find { it.id == thread.id }
        assertNotNull(updatedThread, "Thread must still be present after the message arrives")
        assertTrue(updatedThread.messages.isNotEmpty(), "New message must be visible in the list-level snapshot")
    }

    @Test
    fun `threadsFlow accumulates successive messages without an intervening ThreadListFetched`() {
        val thread = makeChatThread(
            id = UUID.fromString("cc000000-0000-0000-0000-000000000009"),
            threadState = Received,
            contactId = null,
        )
        val firstMessage = makeMessageModel(threadIdOnExternalPlatform = thread.id)
        val secondMessage = makeMessageModel(threadIdOnExternalPlatform = thread.id)
        reconnect()
        val results = mutableListOf<List<ChatThread>>()
        val job = testScope.launch { threads.threadsFlow.collect { results.add(it) } }

        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf(thread)))
        threads.thread(thread)
        socketServer.sendServerMessage(ServerResponse.MessageCreated(thread, firstMessage))
        socketServer.sendServerMessage(ServerResponse.MessageCreated(thread, secondMessage))
        job.cancel()

        val updatedThread = results.last().find { it.id == thread.id }
        assertNotNull(updatedThread, "Thread must still be present after both messages arrive")
        assertEquals(
            2,
            updatedThread.messages.size,
            "Both successive messages must accumulate in the list-level snapshot"
        )
    }

    @Test
    fun `threadsFlow surfaces a new message for a thread that already shows a ThreadMetadataLoaded preview`() {
        // Reproduces the device-observed case: an already-active conversation's list entry already
        // carries one non-empty preview message (from ThreadMetadataLoaded, the normal case for any
        // thread that isn't brand new) when a further message arrives via the cache watcher.
        val thread = makeChatThread(
            id = UUID.fromString("cc000000-0000-0000-0000-00000000000b"),
            threadState = Received,
            contactId = null,
        )
        val agentModel = makeAgent()
        val previewMessage = makeMessageModel(threadIdOnExternalPlatform = thread.id)
        val newMessage = makeMessageModel(threadIdOnExternalPlatform = thread.id)
        reconnect()
        val results = mutableListOf<List<ChatThread>>()
        val job = testScope.launch { threads.threadsFlow.collect { results.add(it) } }

        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf(thread)))
        socketServer.sendServerMessage(ServerResponse.ThreadMetadataLoaded(agent = agentModel, message = previewMessage))
        threads.thread(thread)
        val emissionsBeforeMessage = results.size

        socketServer.sendServerMessage(ServerResponse.MessageCreated(thread, newMessage))
        job.cancel()

        assertTrue(
            results.size > emissionsBeforeMessage,
            "threadsFlow must re-emit even though the thread's list entry already had a non-empty preview"
        )
        val updatedThread = results.last().find { it.id == thread.id }
        assertNotNull(updatedThread, "Thread must still be present after the new message arrives")
        assertEquals(
            2,
            updatedThread.messages.size,
            "Both the original preview and the new message must be visible in the list-level snapshot"
        )
    }

    @Test
    fun `threadsFlow evicts messages cache for threads no longer in the thread list`() {
        val thread = makeChatThread(
            id = UUID.fromString("cc000000-0000-0000-0000-000000000003"),
            threadState = Received,
            contactId = null,
        )
        val agentModel = makeAgent()
        val message = makeMessageModel(threadIdOnExternalPlatform = thread.id)
        reconnect()
        val results = mutableListOf<List<ChatThread>>()
        val job = testScope.launch { threads.threadsFlow.collect { results.add(it) } }

        // Populate the cache
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf(thread)))
        threads.thread(thread)
        socketServer.sendServerMessage(ServerResponse.ThreadMetadataLoaded(agent = agentModel, message = message))

        // Thread is absent from the next fetch (deleted/purged on the server)
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(emptyList()))

        // Thread reappears with no message history — cache must have been evicted
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf(thread)))
        job.cancel()

        val reappearedThread = results.last().find { it.id == thread.id }
        assertNotNull(reappearedThread, "Thread must be present after reappearing")
        assertTrue(
            reappearedThread.messages.isEmpty(),
            "Stale messages must not be injected after the thread was absent from the list"
        )
    }

    @Test
    fun `threadsFlow preserves messages cache for archived threads still in the list`() {
        val thread = makeChatThread(
            id = UUID.fromString("cc000000-0000-0000-0000-000000000004"),
            threadState = Received,
            contactId = null,
        )
        val agentModel = makeAgent()
        val message = makeMessageModel(threadIdOnExternalPlatform = thread.id)
        reconnect()
        val results = mutableListOf<List<ChatThread>>()
        val job = testScope.launch { threads.threadsFlow.collect { results.add(it) } }

        // Populate the cache
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf(thread)))
        threads.thread(thread)
        socketServer.sendServerMessage(ServerResponse.ThreadMetadataLoaded(agent = agentModel, message = message))

        // Thread is archived — watcher stops but thread remains in the server list
        socketServer.sendServerMessage(ServerResponse.CaseStatusChanged(thread, Closed))

        // Server returns the archived thread with no message history — cache must be preserved
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf(thread)))
        job.cancel()

        val archivedThread = results.last().find { it.id == thread.id }
        assertNotNull(archivedThread, "Archived thread must still appear in the list")
        assertEquals(1, archivedThread.messages.size, "Exactly the cached message must be present")
        assertEquals(
            message.idOnExternalPlatform,
            archivedThread.messages.first().id,
            "Cached message identity must match the original"
        )
    }

    @Test
    fun `threadsFlow evicts only the absent thread cache entry when other threads remain in the list`() {
        val threadA = makeChatThread(
            id = UUID.fromString("cc000000-0000-0000-0000-000000000005"),
            threadState = Received,
            contactId = null,
        )
        val threadB = makeChatThread(
            id = UUID.fromString("cc000000-0000-0000-0000-000000000006"),
            threadState = Received,
            contactId = null,
        )
        val threadC = makeChatThread(
            id = UUID.fromString("cc000000-0000-0000-0000-000000000007"),
            threadState = Received,
            contactId = null,
        )
        val agentModel = makeAgent()
        val messageA = makeMessageModel(threadIdOnExternalPlatform = threadA.id)
        val messageB = makeMessageModel(threadIdOnExternalPlatform = threadB.id)
        val messageC = makeMessageModel(threadIdOnExternalPlatform = threadC.id)
        reconnect()
        val results = mutableListOf<List<ChatThread>>()
        val job = testScope.launch { threads.threadsFlow.collect { results.add(it) } }

        // Populate cache for all three threads
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf(threadA, threadB, threadC)))
        threads.thread(threadA)
        socketServer.sendServerMessage(ServerResponse.ThreadMetadataLoaded(agent = agentModel, message = messageA))
        threads.thread(threadB)
        socketServer.sendServerMessage(ServerResponse.ThreadMetadataLoaded(agent = agentModel, message = messageB))
        threads.thread(threadC)
        socketServer.sendServerMessage(ServerResponse.ThreadMetadataLoaded(agent = agentModel, message = messageC))

        // threadB is purged — only A and C remain
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf(threadA, threadC)))
        job.cancel()

        val lastList = results.last()
        val resultA = lastList.find { it.id == threadA.id }
        val resultB = lastList.find { it.id == threadB.id }
        val resultC = lastList.find { it.id == threadC.id }
        assertNotNull(resultA, "Thread A must still be in the list")
        assertNull(resultB, "Thread B must have been removed from the list")
        assertNotNull(resultC, "Thread C must still be in the list")
        assertTrue(resultA.messages.isNotEmpty(), "Thread A cache must be preserved after partial eviction")
        assertTrue(resultC.messages.isNotEmpty(), "Thread C cache must be preserved after partial eviction")
    }

    @Test
    fun `threadsFlow watcher job is cancelled on eviction so post-purge metadata does not repopulate cache`() {
        val thread = makeChatThread(
            id = UUID.fromString("cc000000-0000-0000-0000-000000000008"),
            threadState = Received,
            contactId = null,
        )
        val agentModel = makeAgent()
        val message = makeMessageModel(threadIdOnExternalPlatform = thread.id)
        reconnect()
        val results = mutableListOf<List<ChatThread>>()
        val job = testScope.launch { threads.threadsFlow.collect { results.add(it) } }

        // Populate cache and start the watcher
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf(thread)))
        threads.thread(thread)
        socketServer.sendServerMessage(ServerResponse.ThreadMetadataLoaded(agent = agentModel, message = message))

        // Thread is purged — cache entry and watcher job must both be evicted
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(emptyList()))

        // Delayed metadata for the now-evicted thread: if the watcher is NOT cancelled,
        // this would repopulate messages[thread.id]; a cancelled watcher ignores it
        socketServer.sendServerMessage(ServerResponse.ThreadMetadataLoaded(agent = agentModel, message = message))

        // Thread reappears with no history — cache must stay empty because watcher was cancelled
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf(thread)))
        job.cancel()

        val reappearedThread = results.last().find { it.id == thread.id }
        assertNotNull(reappearedThread, "Thread must be present after reappearing")
        assertTrue(
            reappearedThread.messages.isEmpty(),
            "Watcher must be cancelled on eviction — post-purge metadata must not repopulate the cache"
        )
    }

    @Test
    fun `thread installs fresh watcher after eviction and reappearance so messages repopulate`() {
        val thread = makeChatThread(
            id = UUID.fromString("cc000000-0000-0000-0000-00000000000b"),
            threadState = Received,
            contactId = null,
        )
        val agentModel = makeAgent()
        val message = makeMessageModel(threadIdOnExternalPlatform = thread.id)
        reconnect()
        val results = mutableListOf<List<ChatThread>>()
        val job = testScope.launch { threads.threadsFlow.collect { results.add(it) } }

        // Round 1: install watcher, populate cache
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf(thread)))
        threads.thread(thread)
        socketServer.sendServerMessage(ServerResponse.ThreadMetadataLoaded(agent = agentModel, message = message))
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf(thread)))
        val afterFirstFetch = results.last().find { it.id == thread.id }
        assertNotNull(afterFirstFetch, "Thread must be present after first fetch")
        assertTrue(afterFirstFetch.messages.isNotEmpty(), "Messages must be cached after first metadata load")

        // Eviction: thread disappears from the list
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(emptyList()))

        // Reappearance: both the messages cache and the MemoizeHandlers entry must be gone
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf(thread)))
        val afterReappear = results.last().find { it.id == thread.id }
        assertNotNull(afterReappear, "Thread must be present after reappearing")
        assertTrue(afterReappear.messages.isEmpty(), "Evicted thread must reappear with no cached messages")

        // Round 2: reinstall watcher — computeIfAbsent must miss and call origin.thread()
        threads.thread(thread)
        socketServer.sendServerMessage(ServerResponse.ThreadMetadataLoaded(agent = agentModel, message = message))
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf(thread)))
        job.cancel()

        val afterReinstall = results.last().find { it.id == thread.id }
        assertNotNull(afterReinstall, "Thread must be present after watcher reinstall")
        assertTrue(
            afterReinstall.messages.isNotEmpty(),
            "Fresh watcher must have repopulated the messages cache after reinstall",
        )
    }
}
