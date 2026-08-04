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

package com.nice.cxonechat.internal

import com.nice.cxonechat.Authorization
import com.nice.cxonechat.ChatStateListener
import com.nice.cxonechat.internal.model.ChatImplDependencies
import com.nice.cxonechat.internal.model.ConfigurationInternal
import com.nice.cxonechat.internal.socket.ProxyWebSocketListener
import com.nice.cxonechat.internal.socket.SocketFactory
import com.nice.cxonechat.state.Connection
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.WebSocket
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Callback
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatReconnectWebsocketTest {
    private lateinit var chatReconnect: ChatReconnectWebsocket
    private lateinit var socketFactory: SocketFactory
    private lateinit var entrails: ChatEntrails
    private lateinit var configuration: ConfigurationInternal
    private lateinit var callback: Callback<Void>
    private lateinit var connection: Connection
    private lateinit var chatStateListener: ChatStateListener
    private lateinit var dispatcher: TestDispatcher
    private lateinit var testScope: TestScope
    private lateinit var authorization: Authorization

    @Before
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        testScope = TestScope(dispatcher)
        socketFactory = mockk(relaxed = true)
        entrails = mockk(relaxed = true)
        configuration = mockk(relaxed = true)
        callback = mockk(relaxed = true)
        connection = mockk(relaxed = true)
        chatStateListener = mockk(relaxed = true)
        authorization = Authorization("code", "verifier")
        every { socketFactory.createProxyListener() } returns ProxyWebSocketListener()
        val threading = ThreadingExecutor(
            mainDispatcher = dispatcher,
            backgroundDispatcher = dispatcher,
            ioDispatcher = dispatcher,
            coroutineScope = testScope,
            storageWriteScope = CoroutineScope(SupervisorJob(testScope.coroutineContext[Job]) + dispatcher),
            storageDispatcher = dispatcher,
        )
        every { entrails.threading } returns threading
        chatReconnect = ChatReconnectWebsocket(
            ChatImpl(
                connection = connection,
                entrails = entrails,
                dependencies = ChatImplDependencies(
                    socketFactory = socketFactory,
                    callback = callback,
                    authorization = authorization
                ),
                configuration = configuration,
                rawChatStateListener = chatStateListener
            ),
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `should notify integrator immediately on initial connection failure and not reconnect`() = runTest(dispatcher) {
        val webSocket = mockk<WebSocket>(relaxed = true)
        every { socketFactory.create(any(), any()) } returns webSocket
        val throwable = Throwable("Initial connection failed")
        chatReconnect.socketListener.onFailure(webSocket, throwable, null)
        advanceUntilIdle()
        verify { chatStateListener.onUnexpectedDisconnect() }
        val listener = chatReconnect.reconnectListener
        assertEquals(0, listener.reconnectAttempts.get())
    }

    @Test
    fun `should cancel reconnect job on close`() = runTest {
        val webSocket = mockk<WebSocket>(relaxed = true)
        every { socketFactory.create(any(), any()) } returns webSocket
        chatReconnect.socketListener.onOpen(webSocket, mockk(relaxed = true))
        chatReconnect.socketListener.onClosed(webSocket, 1000, "Test disconnect")
        chatReconnect.close()
        advanceUntilIdle()
        val listener = ChatReconnectWebsocket::class.java.getDeclaredField("reconnectListener").apply { isAccessible = true }.get(chatReconnect)
        val reconnectJob = ReconnectingListener::class.java.getDeclaredField("reconnectJob").apply { isAccessible = true }.get(listener)
        assertTrue(reconnectJob == null || (reconnectJob as Job).isCancelled)
    }

    @Test
    fun `should rearm reconnect listener after close and connect cycle`() = runTest(dispatcher) {
        val webSocket = mockk<WebSocket>(relaxed = true)
        every { socketFactory.create(any(), any()) } returns webSocket

        // Session 1 connects, then the app goes to background: close() disposes the listener.
        chatReconnect.socketListener.onOpen(webSocket, mockk(relaxed = true))
        val disposedListener = chatReconnect.reconnectListener
        chatReconnect.close()
        advanceUntilIdle()

        // Session 2: connect() on the same instance must re-arm auto-reconnect.
        val connectJob = launch { chatReconnect.connect() }
        advanceUntilIdle()
        assertNotSame(
            disposedListener,
            chatReconnect.reconnectListener,
            "connect() after close() must register a fresh reconnect listener"
        )
        chatReconnect.socketListener.onOpen(webSocket, mockk(relaxed = true))
        advanceUntilIdle()

        // An unexpected disconnect in session 2 must reach a LIVE reconnect listener, which then
        // drives reconnection (observable as a fresh onConnecting from the re-invoked connect()).
        clearMocks(chatStateListener, answers = false)
        chatReconnect.socketListener.onFailure(webSocket, Throwable("Session-2 connection lost"), null)
        advanceUntilIdle()
        verify(atLeast = 1) { chatStateListener.onConnecting() }
        connectJob.cancel()
    }
}
