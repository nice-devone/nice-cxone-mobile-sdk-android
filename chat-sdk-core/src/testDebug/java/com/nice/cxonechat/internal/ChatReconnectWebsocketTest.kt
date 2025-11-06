/*
 * Copyright (c) 2021-2025. NICE Ltd. All rights reserved.
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

import com.nice.cxonechat.ChatStateListener
import com.nice.cxonechat.internal.model.ConfigurationInternal
import com.nice.cxonechat.internal.socket.ProxyWebSocketListener
import com.nice.cxonechat.internal.socket.SocketFactory
import com.nice.cxonechat.state.Connection
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
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
        every { socketFactory.createProxyListener() } returns ProxyWebSocketListener()
        chatReconnect = ChatReconnectWebsocket(
            ChatImpl(
                connection = connection,
                entrails = entrails,
                socketFactory = socketFactory,
                configuration = configuration,
                callback = callback,
                chatStateListener = chatStateListener,
            ),
            dispatcher = dispatcher
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
        val listener = ChatReconnectWebsocket::class.java.getDeclaredField("reconnectListener").apply { isAccessible = true }.get(chatReconnect)
        assertEquals(0, ReconnectingListener::class.java.getDeclaredField("reconnectAttempts").apply { isAccessible = true }.get(listener))
    }

    @Test
    fun `should cancel reconnect job on close`() = runTest(dispatcher) {
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
}
