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

import android.annotation.SuppressLint
import com.nice.cxonechat.FakeChatStateListener.ChatStateConnection.Ready
import com.nice.cxonechat.enums.ErrorType.RecoveringThreadFailed
import com.nice.cxonechat.exceptions.MissingThreadListFetchException
import com.nice.cxonechat.exceptions.UnsupportedChannelConfigException
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.server.ServerRequest
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.tool.nextStringMap
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class ChatThreadsHandlerSingleThreadTest : AbstractSingleThreadChatTest() {

    private lateinit var threads: ChatThreadsHandler

    override fun prepare() {
        super.prepare()
        threads = chat.threads()
    }

    // ---

    @Test(expected = UnsupportedChannelConfigException::class)
    fun create_throws_whenCannotCreateMultipleThreads() {
        with(chat.threads()) {
            testScope.launch { threadsFlow.collect {} }
            serverResponds(ServerResponse.ThreadListFetched(listOf(makeChatThread())))
            create()
        }
    }

    @Test(expected = UnsupportedChannelConfigException::class)
    fun create_withCustomFields_throws_whenCannotCreateMultipleThreads() {
        with(chat.threads()) {
            testScope.launch { threadsFlow.collect {} }
            serverResponds(ServerResponse.ThreadListFetched(listOf(makeChatThread())))
            create(nextStringMap())
        }
    }

    @Test(expected = MissingThreadListFetchException::class)
    fun create_throws_whenThreadsList_isRegisteredButNotLoaded() {
        with(chat.threads()) {
            testScope.launch { threadsFlow.collect {} }
            create()
        }
    }

    @Test(expected = MissingThreadListFetchException::class)
    fun create_withCustomFields_throws_whenThreadsList_isRegisteredButNotLoaded() {
        with(chat.threads()) {
            testScope.launch { threadsFlow.collect {} }
            create(nextStringMap())
        }
    }

    @Test(expected = MissingThreadListFetchException::class)
    fun create_throws_whenThreadsList_isNotRegistered() {
        chat.threads().create()
    }

    @Test(expected = MissingThreadListFetchException::class)
    fun create__withCustomFields_throws_whenThreadsList_isNotRegistered() {
        chat.threads().create(nextStringMap())
    }

    @SuppressLint("CheckResult")
    @Test
    fun create_permitsSingularThread() {
        with(chat.threads()) {
            testScope.launch { threadsFlow.collect {} }
            serverResponds(ServerResponse.ThreadListFetched(listOf()))
            create()
        }
    }

    @SuppressLint("CheckResult")
    @Test
    fun create_withCustomFields_permitsSingularThread() {
        with(chat.threads()) {
            testScope.launch { threadsFlow.collect {} }
            serverResponds(ServerResponse.ThreadListFetched(listOf()))
            create(nextStringMap())
        }
    }

    @SuppressLint("CheckResult")
    @Test
    fun `create succeeds on subsequent chat threads call after thread list fetched`() {
        with(chat.threads()) {
            testScope.launch { threadsFlow.collect {} }
            serverResponds(ServerResponse.ThreadListFetched(listOf()))
        }
        chat.threads().create()
    }

    @Test
    fun threadsFlow_emits_whenNewThreadIsCreated_inSingleThreadMode() {
        val results = mutableListOf<List<ChatThread>>()
        val job = testScope.launch { threads.threadsFlow.take(2).toList(results) }

        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf()))
        threads.create()

        assertEquals(2, results.size, "Expected two emissions: initial list and after creation")
        assertEquals(0, results[0].size, "Expected zero threads in initial list")
        assertEquals(1, results[1].size, "Expected one thread after creation")
        job.cancel()
    }

    @Test
    fun threadsFlow_doesNotEmit_afterCollectionCancelled_inSingleThreadMode() {
        val firstResults = mutableListOf<List<ChatThread>>()
        val firstJob = testScope.launch { threads.threadsFlow.take(1).toList(firstResults) }
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf()))
        assertEquals(1, firstResults.size, "First collection should receive initial thread list")
        assertEquals(0, firstResults[0].size, "Initial list should be empty")
        firstJob.cancel()

        val secondResults = mutableListOf<List<ChatThread>>()
        val secondJob = testScope.launch { threads.threadsFlow.take(2).toList(secondResults) }
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf(makeChatThread())))
        assertTrue(secondResults.any { it.isNotEmpty() }, "Second collection should receive non-empty thread list")
        secondJob.cancel()
    }

    @Test
    fun threadsFlow_emits_recoveredThread_whenThreadRecovered() {
        connect()
        val thread = makeChatThread()
        val results = mutableListOf<List<ChatThread>>()
        val job = testScope.launch { threads.threadsFlow.take(1).toList(results) }
        socketServer.sendServerMessage(ServerResponse.ThreadRecovered(thread = thread))
        assertEquals(1, results.size, "Should emit once on ThreadRecovered")
        assertEquals(1, results[0].size, "Recovered list should contain the recovered thread")
        assertEquals(thread.id, results[0][0].id)
        assertEquals(Ready, chatStateListener.connection, "onReady() should be called after recovery")
        job.cancel()
    }

    @Test
    fun threadsFlow_emits_emptyList_andCallsOnReady_whenRecoveringThreadFailed() {
        connect()
        val results = mutableListOf<List<ChatThread>>()
        val job = testScope.launch { threads.threadsFlow.take(1).toList(results) }
        socketServer.sendServerMessage(ServerResponse.ErrorResponse(RecoveringThreadFailed.value))
        assertEquals(1, results.size, "Should emit once on RecoveringThreadFailed")
        assertTrue(results[0].isEmpty(), "List must be empty when thread recovery fails")
        assertEquals(Ready, chatStateListener.connection, "onReady() should be called after failed recovery")
        job.cancel()
    }

    @Test
    fun threadsFlow_triggersOnlyOneRecoverRequest_withMultipleConcurrentCollectors() {
        connect()
        assertSendTexts(ServerRequest.RecoverThread(connection, null)) {
            val job1 = testScope.launch { threads.threadsFlow.collect {} }
            val job2 = testScope.launch { threads.threadsFlow.collect {} }
            job1.cancel()
            job2.cancel()
        }
    }

    @Test
    fun `threadsFlow late subscriber after recovery failure does not receive stale emptyList from replay`() {
        connect()
        val earlyJob = testScope.launch { threads.threadsFlow.take(1).toList(mutableListOf()) }
        socketServer.sendServerMessage(ServerResponse.ErrorResponse(RecoveringThreadFailed.value))
        earlyJob.cancel()

        val lateResults = mutableListOf<List<ChatThread>>()
        val lateJob = testScope.launch { threads.threadsFlow.take(1).toList(lateResults) }
        val thread = makeChatThread()
        socketServer.sendServerMessage(ServerResponse.ThreadRecovered(thread = thread))
        assertEquals(1, lateResults.size, "Late subscriber should receive one emission")
        assertEquals(1, lateResults[0].size, "Late subscriber must get the recovered thread, not stale emptyList")
        assertEquals(thread.id, lateResults[0][0].id)
        lateJob.cancel()
    }

    @Test
    fun `threadsFlow late subscriber after recovery then failure sees authoritative thread list not emptyList`() {
        connect()
        val thread = makeChatThread()
        val firstJob = testScope.launch { threads.threadsFlow.collect {} }
        socketServer.sendServerMessage(ServerResponse.ThreadRecovered(thread = thread))
        socketServer.sendServerMessage(ServerResponse.ErrorResponse(RecoveringThreadFailed.value))
        firstJob.cancel()

        val lateResults = mutableListOf<List<ChatThread>>()
        val lateJob = testScope.launch { threads.threadsFlow.take(1).toList(lateResults) }
        assertEquals(1, lateResults.size, "Late subscriber should receive one emission from replay")
        assertEquals(1, lateResults[0].size, "Replay must hold the authoritative thread, not the failure emptyList")
        assertEquals(thread.id, lateResults[0][0].id)
        lateJob.cancel()
    }
}
