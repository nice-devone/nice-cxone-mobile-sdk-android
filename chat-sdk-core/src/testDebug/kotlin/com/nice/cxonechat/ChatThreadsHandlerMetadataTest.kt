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

import com.nice.cxonechat.exceptions.RuntimeChatException
import com.nice.cxonechat.model.makeAgent
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.model.makeMessageModel
import com.nice.cxonechat.server.ServerRequest
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState
import com.nice.cxonechat.thread.ChatThreadState.Received
import io.mockk.clearMocks
import io.mockk.every
import kotlinx.coroutines.launch
import org.junit.Test
import java.util.UUID
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Metadata loading, dedup, retry, and timeout behavior of the multi-thread [ChatThreadsHandler].
 * Handler identity lives in [ChatThreadsHandlerTest]; `threadsFlow` emission in
 * [ChatThreadsHandlerFlowTest]; the messages cache in [ChatThreadsHandlerMessagesCacheTest].
 */
internal class ChatThreadsHandlerMetadataTest : AbstractChatThreadsHandlerTest() {

    @Test
    fun threads_loadsMetadata_forNonPendingThread_only() {
        val nonPending = makeChatThread(
            id = UUID.fromString("de4080b9-4f24-46cf-b3a0-793ec3dfb2d5"),
            threadState = Received,
            contactId = null
        )
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
}
