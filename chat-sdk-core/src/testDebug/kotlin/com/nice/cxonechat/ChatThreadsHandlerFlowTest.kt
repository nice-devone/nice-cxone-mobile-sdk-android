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

import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.server.ServerRequest
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState.Received
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * `threadsFlow` emission mechanics of the multi-thread [ChatThreadsHandler]: per-update emission,
 * replay to late subscribers, multiple concurrent collectors, cancellation, and single-fetch
 * sharing. Handler identity lives in [ChatThreadsHandlerTest]; metadata loading in
 * [ChatThreadsHandlerMetadataTest]; the messages cache in [ChatThreadsHandlerMessagesCacheTest].
 */
internal class ChatThreadsHandlerFlowTest : AbstractChatThreadsHandlerTest() {

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
}
