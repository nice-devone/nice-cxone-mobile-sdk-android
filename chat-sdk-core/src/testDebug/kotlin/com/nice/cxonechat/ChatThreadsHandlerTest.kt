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

import com.nice.cxonechat.FakeChatStateListener.ChatStateConnection.Ready
import com.nice.cxonechat.exceptions.RuntimeChatException
import com.nice.cxonechat.internal.ChatLogging
import com.nice.cxonechat.internal.ChatThreadsHandlerMemoizeHandlers
import com.nice.cxonechat.internal.ChatWithParameters
import com.nice.cxonechat.internal.model.ChannelConfiguration
import com.nice.cxonechat.internal.model.ChatThreadMutable.Companion.asMutable
import com.nice.cxonechat.internal.model.network.EventCaseStatusChanged.CaseStatus.Closed
import com.nice.cxonechat.model.makeAgent
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.model.makeMessageModel
import com.nice.cxonechat.server.ServerRequest
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState
import com.nice.cxonechat.thread.ChatThreadState.Loaded
import com.nice.cxonechat.thread.ChatThreadState.Received
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.junit.Test
import java.util.UUID
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

internal class ChatThreadsHandlerTest : AbstractMultiThreadChatTest() {

    private lateinit var threads: ChatThreadsHandler

    override val config: ChannelConfiguration
        get() = requireNotNull(super.config)

    override fun prepare() {
        super.prepare()
        threads = chat.threads()
    }

    // ---

    @Test
    fun `threads returns same handler instance across calls`() {
        val first = chat.threads()
        val second = chat.threads()
        assertSame(first, second)
    }

    @Test
    fun `ChatLogging threads returns same handler across calls`() {
        val logging = ChatLogging(chat as ChatWithParameters)
        val first = logging.threads()
        val second = logging.threads()
        assertSame(first, second)
    }

    @Test
    fun refresh_sendsExpectedMessage() {
        assertSendText(ServerRequest.FetchThreadList(connection)) {
            threads.refresh()
        }
    }

    @Test
    fun threads_notifies_withInitialList() {
        val initial = List(2) { makeChatThread(threadState = Received, contactId = null) }
        val message = makeMessageModel(threadIdOnExternalPlatform = initial[0].id)
        val agentModel = makeAgent()
        val expected = listOf(
            initial[0].copy(threadAgent = agentModel.toAgent(), messages = listOfNotNull(message.toMessage()), threadState = Loaded),
            initial[1]
        ).map {
            it.asMutable()
        }
        reconnect()
        // verify that the metadata is loaded when the list is received
        assertSendTexts(
            ServerRequest.FetchThreadList(connection),
            ServerRequest.LoadThreadMetadata(connection, initial[0]),
            ServerRequest.LoadThreadMetadata(connection, initial[1])
        ) {
            // Multithread threads should start in READY state.
            assertEquals(Ready, chatStateListener.connection)
            val actual = testCallback(::threads) {
                sendServerMessage(ServerResponse.ThreadListFetched(initial))
                sendServerMessage(
                    ServerResponse.ThreadMetadataLoaded(agent = agentModel, message = message)
                )
            }
            assertEquals(expected, actual)
        }
    }

    @Test
    fun create_passesNewThread() {
        val handler = chat.threads().create()
        val thread = handler.get()
        assertNotNull(thread)
    }

    @Test
    fun create_withCustomParameters_passesNewThread() {
        val handler = chat.threads().create(emptyMap())
        val thread = handler.get()
        assertNotNull(thread)
    }

    @Test
    fun threads_notifies_caseClosed() {
        val initial = List(2) { makeChatThread(threadState = Received, contactId = null) }
        val expected = initial.toMutableList().also {
            it[0] = it[0].copy(
                canAddMoreMessages = false,
                threadState = ChatThreadState.Closed,
            )
        }
        assertEquals(true, initial[0].canAddMoreMessages)
        val actual = testCallback(::threads) {
            sendServerMessage(ServerResponse.ThreadListFetched(initial))
            sendServerMessage(ServerResponse.CaseStatusChanged(expected[0], Closed))
        }
        assertNotEquals(expected, initial)
        assertEquals(expected, actual)
    }

    @Test
    fun threadsFlow_doesNotEmit_afterCollectionCancelled() {
        val initial = List(1) { makeChatThread(threadState = Received, contactId = null) }
        val results = mutableListOf<List<ChatThread>>()
        val job = testScope.launch { threads.threadsFlow.collect { results.add(it) } }
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(initial))
        threads.create()
        job.cancel()
        // Further creates must not reach the cancelled collector
        threads.create()
        assertEquals(2, results.size, "Collection should stop after cancel")
    }

    @Test
    fun threadsFlow_emits_onEachUpdate() {
        val firstList = List(2) { makeChatThread(threadState = Received, contactId = null) }
        val secondList = List(3) { makeChatThread(threadState = Received, contactId = null) }
        val results = mutableListOf<List<ChatThread>>()
        val job = testScope.launch { threads.threadsFlow.take(4).toList(results) }
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(firstList))
        threads.create()
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(secondList))
        threads.create()
        assertEquals(4, results.size)
        assertEquals(2, results[0].size, "Initial fetch: 2 threads")
        assertEquals(3, results[1].size, "After first create: 3 threads")
        assertEquals(4, results[2].size, "Second fetch: 3 new + 1 pending = 4 threads")
        assertEquals(5, results[3].size, "After second create: 5 threads")
        job.cancel()
    }

    @Test
    fun threadsFlow_supportsMultipleConcurrentCollectors() {
        val initial = List(1) { makeChatThread(threadState = Received, contactId = null) }
        val results1 = mutableListOf<List<ChatThread>>()
        val results2 = mutableListOf<List<ChatThread>>()
        val job1 = testScope.launch { threads.threadsFlow.take(2).toList(results1) }
        val job2 = testScope.launch { threads.threadsFlow.take(2).toList(results2) }
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(initial))
        threads.create()
        assertEquals(2, results1.size, "First collector receives all emissions")
        assertEquals(2, results2.size, "Second collector receives all emissions")
        assertEquals(1, results1[0].size, "First collector: initial list size")
        assertEquals(1, results2[0].size, "Second collector: initial list size")
        assertEquals(2, results1[1].size, "First collector: after create")
        assertEquals(2, results2[1].size, "Second collector: after create")
        job1.cancel()
        job2.cancel()
    }

    @Test
    fun thread_createsHandlerForExistingThread() {
        val existingThread = makeChatThread(threadState = Loaded, contactId = null)
        val handler = threads.thread(existingThread)
        assertNotNull(handler)
        assertEquals(existingThread.id, handler.get().id)
    }

    @Test
    fun thread_createsHandlerForThreadWithMessages() {
        val message = makeMessageModel().toMessage()
        val threadWithMessages = makeChatThread(
            threadState = Loaded,
            contactId = null,
            messages = listOfNotNull(message)
        )
        val handler = threads.thread(threadWithMessages)
        assertNotNull(handler)
        assertEquals(threadWithMessages.id, handler.get().id)
        assertEquals(1, handler.get().messages.size)
    }

    @Test
    fun threads_loadsMetadata_forNonPendingThread_only() {
        val nonPending = makeChatThread(id = UUID.fromString("de4080b9-4f24-46cf-b3a0-793ec3dfb2d5"), threadState = Received, contactId = null)
        val initial = listOf(nonPending)
        reconnect()
        val pendingHandler = threads.create()
        val pending = pendingHandler.get()
        assertEquals(ChatThreadState.Pending, pending.threadState)
        // Only non-pending thread should trigger metadata load
        val receivedLists = mutableListOf<List<ChatThread>>()
        assertSendTexts(
            ServerRequest.FetchThreadList(connection),
            ServerRequest.LoadThreadMetadata(connection, nonPending),
        ) {
            val job = testScope.launch { threads.threadsFlow.collect { receivedLists.add(it) } }
            socketServer.sendServerMessage(ServerResponse.ThreadListFetched(initial))
            job.cancel()
        }
        val finalList = receivedLists.last()
        assertContains(finalList.map { it.id }, pending.id)
        assertContains(finalList.map { it.id }, nonPending.id)
    }

    @Test
    fun threadsFlow_emits_threadList() {
        val initial = List(2) { makeChatThread(threadState = Received, contactId = null) }
        var actual: List<ChatThread>? = null
        val job = testScope.launch {
            actual = threads.threadsFlow.first()
        }
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(initial))
        job.cancel()
        val finalActual = actual
        assertNotNull(finalActual)
        assertEquals(2, finalActual.size)
    }

    @Test
    fun threadsFlow_emits_updated_list_on_create() {
        val initial = List(1) { makeChatThread(threadState = Received, contactId = null) }
        val results = mutableListOf<List<ChatThread>>()
        val job = testScope.launch {
            threads.threadsFlow.take(2).toList(results)
        }
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(initial))
        threads.create()
        job.cancel()
        assertEquals(2, results.size)
        assertEquals(1, results[0].size)
        assertEquals(2, results[1].size)
    }

    @Test
    fun threadsFlow_delivers_replayedValue_toLateSubscriber() {
        val initial = List(2) { makeChatThread(threadState = Received, contactId = null) }
        // Prime the replay cache
        var primed: List<ChatThread>? = null
        val job1 = testScope.launch { primed = threads.threadsFlow.first() }
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(initial))
        job1.cancel()
        assertNotNull(primed)
        // Late subscriber must receive replayed value without a new server event
        var replayed: List<ChatThread>? = null
        val job2 = testScope.launch { replayed = threads.threadsFlow.first() }
        job2.cancel()
        assertEquals(primed.map { it.id }, replayed?.map { it.id })
    }

    @Test
    fun threads_loadsMetadata_oncePerThread_acrossMultipleThreadListFetched() {
        val thread = makeChatThread(
            id = UUID.fromString("de4080b9-4f24-46cf-b3a0-793ec3dfb2d6"),
            threadState = Received,
            contactId = null,
        )
        reconnect()
        assertSendTexts(
            ServerRequest.FetchThreadList(connection),
            ServerRequest.LoadThreadMetadata(connection, thread),
        ) {
            val job = testScope.launch { threads.threadsFlow.collect { } }
            // First fetch — triggers metadata load
            socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf(thread)))
            // Second fetch with same thread — must NOT trigger a second metadata load
            socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf(thread)))
            job.cancel()
        }
    }

    @Test
    fun threads_metadataCompletion_doesNotDropThreadsAddedByLaterFetch() {
        val t1 = makeChatThread(
            id = UUID.fromString("11111111-0000-0000-0000-000000000001"),
            threadState = Received,
            contactId = null,
        )
        val t2 = makeChatThread(
            id = UUID.fromString("11111111-0000-0000-0000-000000000002"),
            threadState = Received,
            contactId = null,
        )
        val t3 = makeChatThread(
            id = UUID.fromString("11111111-0000-0000-0000-000000000003"),
            threadState = Received,
            contactId = null,
        )
        val agentModel = makeAgent()
        val message = makeMessageModel(threadIdOnExternalPlatform = t1.id)

        reconnect()
        val results = mutableListOf<List<ChatThread>>()
        val job = testScope.launch { threads.threadsFlow.collect { results.add(it) } }

        // First fetch — metadata requested for T1 and T2, not yet responded
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf(t1, t2)))
        // Second fetch adds T3 — T1/T2 deduped, T3 triggers new metadata load
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf(t1, t2, t3)))
        // T1 metadata arrives — stale launch would trySend([T1,T2]) and erase T3
        socketServer.sendServerMessage(ServerResponse.ThreadMetadataLoaded(agent = agentModel, message = message))

        job.cancel()

        val lastEmit = results.last()
        val ids = lastEmit.map { it.id }
        assertContains(ids, t1.id, "T1 must still be in list after metadata completion")
        assertContains(ids, t3.id, "T3 must not disappear when T1 metadata completes")
        assertEquals(3, lastEmit.size, "All 3 threads must be present in final emission")
    }

    @Test
    fun threads_reportsServerCommunicationError_andRetriesMetadata_whenMetadataRequestFails() {
        val thread = makeChatThread(
            id = UUID.fromString("de4080b9-4f24-46cf-b3a0-793ec3dfb2d7"),
            threadState = Received,
            contactId = null,
        )
        reconnect()
        // Fail the first LoadThreadMetadata socket send to simulate a transient network error
        clearMocks(socket)
        var metadataFailureCount = 0
        val capturedMessages = mutableListOf<String>()
        every { socket.send(text = capture(capturedMessages)) } answers {
            val msg = capturedMessages.last()
            if (msg.contains("LoadThreadMetadata") && metadataFailureCount++ == 0) throw RuntimeException("Simulated socket failure")
            true
        }
        val job = testScope.launch { threads.threadsFlow.collect {} }
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf(thread)))
        assertEquals(1, chatStateListener.onChatRuntimeExceptions.size)
        assertTrue(chatStateListener.onChatRuntimeExceptions[0] is RuntimeChatException.ServerCommunicationError)
        // After failure the thread ID is removed from metadataRequested; the next ThreadListFetched retries
        assertSendTexts(ServerRequest.LoadThreadMetadata(connection, thread)) {
            socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf(thread)))
        }
        job.cancel()
    }

    @Test
    fun threadsFlow_allowsMetadataRetry_whenMetadataLoadTimesOut() {
        val thread = makeChatThread(
            id = UUID.fromString("dd000001-0000-0000-0000-000000000001"),
            threadState = Received,
            contactId = null,
        )
        reconnect()
        val job = testScope.launch { threads.threadsFlow.collect {} }
        // First fetch — metadata load starts but never completes
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf(thread)))
        // Advance past the 30-second metadata-load timeout
        testScope.testScheduler.advanceTimeBy(30_001L)
        // metadataRequested should be cleared — next fetch must retry metadata
        assertSendTexts(ServerRequest.LoadThreadMetadata(connection, thread)) {
            socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf(thread)))
        }
        job.cancel()
    }

    @Test
    fun threadsFlow_doesNotEmitExtraUpdate_whenThreadIsAlreadyReadyAfterRecovery() {
        val thread = makeChatThread(
            id = UUID.fromString("dd000002-0000-0000-0000-000000000001"),
            threadState = Received,
            contactId = null,
        )
        reconnect()
        val results = mutableListOf<List<ChatThread>>()
        val job = testScope.launch { threads.threadsFlow.collect { results.add(it) } }
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf(thread)))
        val sizeAfterFetch = results.size
        // ThreadRecovered transitions the thread to Ready state — updateMetadata should
        // take the "already ready" branch and NOT emit an additional threadsFlow update.
        socketServer.sendServerMessage(ServerResponse.ThreadRecovered(thread = thread))
        assertEquals(
            sizeAfterFetch,
            results.size,
            "No extra threadsFlow emission expected when thread transitions to Ready via ThreadRecovered"
        )
        job.cancel()
    }

    fun threads(listener: (List<ChatThread>) -> Unit): Cancellable {
        val job = testScope.launch { threads.threadsFlow.collect { listener(it) } }
        return Cancellable { job.cancel() }
    }

    @Test
    fun threadsFlow_triggersOnlyOneFetchRequest_withMultipleConcurrentCollectors() {
        reconnect()
        assertSendTexts(ServerRequest.FetchThreadList(connection)) {
            val job1 = testScope.launch { threads.threadsFlow.collect {} }
            val job2 = testScope.launch { threads.threadsFlow.collect {} }
            socketServer.sendServerMessage(ServerResponse.ThreadListFetched(emptyList()))
            job1.cancel()
            job2.cancel()
        }
    }

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
    fun `thread returns the same handler instance for repeated calls with the same id`() {
        val thread = makeChatThread(
            id = UUID.fromString("cc000000-0000-0000-0000-00000000000a"),
            threadState = Received,
            contactId = null,
        )
        reconnect()
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf(thread)))

        // Simulates the list screen and the conversation screen independently calling .thread(id)
        // for the same thread within the same session — they must share one handler, not each get
        // their own independent copy of the thread state.
        val first = threads.thread(thread)
        val second = threads.thread(thread)

        assertSame(first, second, "Repeated .thread(id) calls for the same id must return the same memoized handler")
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
    fun `thread calls origin exactly once when called twice with the same thread id`() {
        // ChatThreadsHandlerLogging re-wraps results on every call, so assertSame cannot work here.
        val thread = makeChatThread(
            id = UUID.fromString("cc000000-0000-0000-0000-00000000000a"),
            threadState = Received,
            contactId = null,
        )
        var callCount = 0
        val fakeOrigin = mockk<ChatThreadsHandler>(relaxed = true)
        every { fakeOrigin.thread(any()) } answers {
            callCount++
            mockk(relaxed = true)
        }
        val memoize = ChatThreadsHandlerMemoizeHandlers(fakeOrigin)

        memoize.thread(thread)
        memoize.thread(thread)

        assertEquals(1, callCount, "origin.thread() must be called exactly once — second call must use the memoized handler")

        memoize.thread(makeChatThread())
        assertEquals(2, callCount, "origin.thread() must be called exactly once per thread")
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
