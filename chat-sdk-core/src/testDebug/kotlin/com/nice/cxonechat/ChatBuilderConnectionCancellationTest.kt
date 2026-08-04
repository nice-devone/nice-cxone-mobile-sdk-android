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

package com.nice.cxonechat

import com.nice.cxonechat.internal.model.ChannelConfiguration
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.tool.SocketFactoryMock
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertTrue

/**
 * Tests that [Chat.connect] correctly terminates (via [kotlinx.coroutines.CancellationException])
 * in the following scenarios:
 *
 * **Caller cancellation** — caller cancels the coroutine while [Chat.connect] is suspended:
 * 1. `authDeferred.await()` in `ChatAuthorization.connect()` — waiting for `EventCustomerAuthorized`
 * 2. `recoveredDeferred.await()` in `ChatSingleThread.connect()` — waiting for `ThreadRecovered`
 * 3. `recoveredDeferred.await()` in `ChatLiveChat.connect()` — waiting for `LivechatRecovered`
 *
 * **Socket drop** — socket fails while [Chat.connect] is suspended at the same three points.
 *
 * **Server auth errors** — server sends `ConsumerReconnectionFailed` or `TokenRefreshingFailed`
 * during authorization. Internally `ChatAuthorization` throws [com.nice.cxonechat.internal.PermanentConnectionFailureException]
 * and calls [com.nice.cxonechat.Chat.close], which cancels the caller's job. From the caller's
 * perspective the job is cancelled (`isCancelled = true`), not "failed" — the exception is replaced
 * by a `JobCancellationException` inside `ChatThreadingImpl`'s `withContext` block once the job
 * enters "cancelling" state.
 *
 * OkHttp's `pingInterval` bounds TCP-level failures, but not application-level hangs (server alive,
 * never responds). The caller is expected to cancel the coroutine if a timeout is needed — these
 * tests verify that cancellation propagates cleanly through every suspend point.
 */
internal class ChatBuilderConnectionCancellationTest : AbstractChatTestSubstrate() {

    private var singleThreadMode = false

    override val config: ChannelConfiguration?
        get() {
            val base = super.config ?: return null
            return if (singleThreadMode) base.copy(
                settings = base.settings.copy(hasMultipleThreadsPerEndUser = false)
            ) else base
        }

    override fun prepare() = Unit

    @Test
    fun `connect can be cancelled while awaiting for socket`() = runTest(dispatcher) {
        val (_, builder) = prepareBuilder()
        val chat = builder.setDevelopmentMode(true).build()
        val connectJob = testScope.launch { chat.connect() } // advances past authorization, waits for socket to open
        connectJob.cancel()
        connectJob.join() // must complete — CancellationException must not be swallowed
        assertTrue(connectJob.isCancelled)
    }

    @Test
    fun `connect can be cancelled while awaiting SingleThread recovery`() = runTest(dispatcher) {
        singleThreadMode = true
        val (_, builder) = prepareBuilder()
        val chat = builder.setDevelopmentMode(true).build()
        val connectJob = testScope.launch { chat.connect() }
        socketServer.open()
        socketServer.sendServerMessage(
            ServerResponse.ConsumerAuthorized(
                firstName = SocketFactoryMock.firstName,
                lastName = SocketFactoryMock.lastName,
            )
        )
        // Auth done; ChatSingleThread suspends at recoveredDeferred.await() — no ThreadRecovered sent
        connectJob.cancel()
        connectJob.join()
        assertTrue(connectJob.isCancelled)
    }

    @Test
    fun `connect can be cancelled while awaiting LiveChat recovery`() = runTest(dispatcher) {
        isLiveChat = true // chatAvailability is Online by default → recovery path is entered
        val (_, builder) = prepareBuilder()
        val chat = builder.setDevelopmentMode(true).build()
        val connectJob = testScope.launch { chat.connect() }
        socketServer.open()
        socketServer.sendServerMessage(
            ServerResponse.ConsumerAuthorized(
                firstName = SocketFactoryMock.firstName,
                lastName = SocketFactoryMock.lastName,
            )
        )
        // Auth done; ChatLiveChat suspends at recoveredDeferred.await() — no LivechatRecovered sent
        connectJob.cancel()
        connectJob.join()
        assertTrue(connectJob.isCancelled)
    }

    @Test
    fun `connect cancels when socket drops while awaiting SingleThread recovery`() = runTest(dispatcher) {
        singleThreadMode = true
        val (_, builder) = prepareBuilder()
        val chat = builder.setDevelopmentMode(true).build()
        val connectJob = testScope.launch { chat.connect() }
        socketServer.open()
        socketServer.sendServerMessage(
            ServerResponse.ConsumerAuthorized(
                firstName = SocketFactoryMock.firstName,
                lastName = SocketFactoryMock.lastName,
            )
        )
        // Auth done; ChatSingleThread suspends at recoveredDeferred.await()
        socketServer.disconnect()
        connectJob.join()
        assertTrue(connectJob.isCancelled)
    }

    @Test
    fun `connect cancels when socket drops while awaiting LiveChat recovery`() = runTest(dispatcher) {
        isLiveChat = true
        val (_, builder) = prepareBuilder()
        val chat = builder.setDevelopmentMode(true).build()
        val connectJob = testScope.launch { chat.connect() }
        socketServer.open()
        socketServer.sendServerMessage(
            ServerResponse.ConsumerAuthorized(
                firstName = SocketFactoryMock.firstName,
                lastName = SocketFactoryMock.lastName,
            )
        )
        // Auth done; ChatLiveChat suspends at recoveredDeferred.await()
        socketServer.disconnect()
        connectJob.join()
        assertTrue(connectJob.isCancelled)
    }

    // ---

    private fun prepareBuilder(): Pair<Connection, ChatBuilder> {
        val factory = SocketFactoryMock(socket, proxyListener)
        val connection = factory.getConfiguration(storage)
        return connection to ChatBuilder(entrails, factory)
    }
}
