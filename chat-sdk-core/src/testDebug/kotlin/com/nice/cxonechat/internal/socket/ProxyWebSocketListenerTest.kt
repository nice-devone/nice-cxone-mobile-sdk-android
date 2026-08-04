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

package com.nice.cxonechat.internal.socket

import io.mockk.mockk
import io.mockk.verify
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.junit.Test
import kotlin.test.assertEquals

internal class ProxyWebSocketListenerTest {

    private val proxy = ProxyWebSocketListener()

    // region reportState

    @Test
    fun `reportState notifies SocketStateListener when state changes`() {
        val states = mutableListOf<SocketState>()
        proxy.addListener(stateListener { states.add(it) })

        proxy.reportState(SocketState.OPEN)

        assertEquals(listOf(SocketState.OPEN), states)
    }

    @Test
    fun `reportState does not notify when same state is reported twice`() {
        val states = mutableListOf<SocketState>()
        proxy.addListener(stateListener { states.add(it) })

        proxy.reportState(SocketState.OPEN)
        proxy.reportState(SocketState.OPEN)

        assertEquals(1, states.size)
    }

    @Test
    fun `reportState notifies on each distinct state transition`() {
        val states = mutableListOf<SocketState>()
        proxy.addListener(stateListener { states.add(it) })

        proxy.reportState(SocketState.CONNECTED)
        proxy.reportState(SocketState.OPEN)
        proxy.reportState(SocketState.CLOSING)
        proxy.reportState(SocketState.CLOSED)

        assertEquals(
            listOf(SocketState.CONNECTED, SocketState.OPEN, SocketState.CLOSING, SocketState.CLOSED),
            states
        )
    }

    @Test
    fun `reportState does not notify plain WebSocketListener`() {
        val plainListener = mockk<WebSocketListener>(relaxed = true)
        proxy.addListener(plainListener)

        proxy.reportState(SocketState.OPEN)

        verify(exactly = 0) { plainListener.onOpen(any(), any()) }
        verify(exactly = 0) { plainListener.onMessage(any<WebSocket>(), any<String>()) }
        verify(exactly = 0) { plainListener.onMessage(any<WebSocket>(), any<ByteString>()) }
        verify(exactly = 0) { plainListener.onClosing(any(), any(), any()) }
        verify(exactly = 0) { plainListener.onClosed(any(), any(), any()) }
        verify(exactly = 0) { plainListener.onFailure(any(), any(), anyNullable()) }
    }

    @Test
    fun `removed listener does not receive state notifications`() {
        val states = mutableListOf<SocketState>()
        val listener = stateListener { states.add(it) }
        proxy.addListener(listener)
        proxy.removeListener(listener)

        proxy.reportState(SocketState.OPEN)

        assertEquals(0, states.size)
    }

    // endregion

    // region standard WebSocket events trigger reportState

    @Test
    fun `onMessage text triggers OPEN state report`() {
        val states = mutableListOf<SocketState>()
        proxy.addListener(stateListener { states.add(it) })
        val webSocket = mockk<WebSocket>(relaxed = true)

        proxy.onMessage(webSocket, "hello")

        assertEquals(listOf(SocketState.OPEN), states)
    }

    @Test
    fun `onClosing triggers CLOSING state report`() {
        val states = mutableListOf<SocketState>()
        proxy.addListener(stateListener { states.add(it) })
        val webSocket = mockk<WebSocket>(relaxed = true)

        proxy.onClosing(webSocket, 1001, "going away")

        assertEquals(listOf(SocketState.CLOSING), states)
    }

    @Test
    fun `onClosed triggers CLOSED state report`() {
        val states = mutableListOf<SocketState>()
        proxy.addListener(stateListener { states.add(it) })
        val webSocket = mockk<WebSocket>(relaxed = true)

        proxy.onClosed(webSocket, 1000, "normal")

        assertEquals(listOf(SocketState.CLOSED), states)
    }

    @Test
    fun `onFailure triggers CLOSED state report`() {
        val states = mutableListOf<SocketState>()
        proxy.addListener(stateListener { states.add(it) })
        val webSocket = mockk<WebSocket>(relaxed = true)

        proxy.onFailure(webSocket, Throwable("Network error"), null)

        assertEquals(listOf(SocketState.CLOSED), states)
    }

    // endregion

    // region event forwarding to child listeners

    @Test
    fun `onMessage text is forwarded to all WebSocketListeners`() {
        val child = mockk<WebSocketListener>(relaxed = true)
        proxy.addListener(child)
        val webSocket = mockk<WebSocket>(relaxed = true)

        proxy.onMessage(webSocket, "test message")

        verify { child.onMessage(webSocket, "test message") }
    }

    @Test
    fun `onClosing is forwarded to all WebSocketListeners`() {
        val child = mockk<WebSocketListener>(relaxed = true)
        proxy.addListener(child)
        val webSocket = mockk<WebSocket>(relaxed = true)

        proxy.onClosing(webSocket, 1001, "going away")

        verify { child.onClosing(webSocket, 1001, "going away") }
    }

    private fun stateListener(onStateChanged: (SocketState) -> Unit): WebSocketListener = object : WebSocketListener(),
        SocketStateListener {
        override fun onStateChanged(state: SocketState) {
            onStateChanged.invoke(state)
        }
    }

    // endregion
}
