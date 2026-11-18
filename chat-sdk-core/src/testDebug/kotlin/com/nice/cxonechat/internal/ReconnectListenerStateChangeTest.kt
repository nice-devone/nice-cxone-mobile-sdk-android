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

import com.nice.cxonechat.internal.socket.SocketState
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.tool.MockLogger
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import okhttp3.WebSocket
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Connect-lambda exception handling, [com.nice.cxonechat.ChatStateListener] callback signalling,
 * and the `onStateChanged` (SocketStateListener) path of [ReconnectingListener]. Reconnect-decision
 * behavior lives in [ReconnectListenerTest]; delay computation in [ReconnectListenerDelayTest].
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class ReconnectListenerStateChangeTest : AbstractReconnectListenerTest() {

    @Test
    fun `should not crash if connect lambda throws`() = runTest(dispatcher) {
        val webSocket = mockk<WebSocket>(relaxed = true)
        reconnectListener = ReconnectingListener(
            dispatcher = dispatcher,
            chatStateListener = chatStateListener,
            loggerScope = LoggerScope("ReconnectListenerTest", MockLogger()),
            connect = {
                connectCalled++
                throw TestException("Test exception")
            }
        )
        reconnectListener.randomDelayProvider = { 1000L }
        reconnectListener.onOpen(webSocket, mockk(relaxed = true))
        reconnectListener.onClosing(webSocket, 1001, "Test disconnect")
        advanceUntilIdle()
        // Should not crash, and connectCalled should be incremented
        assertEquals(ReconnectingListener.MAX_RECONNECT_ATTEMPTS, connectCalled)
    }

    @Test
    fun `should abort reconnect loop when connect throws PermanentConnectionFailureException`() = runTest(dispatcher) {
        val webSocket = mockk<WebSocket>(relaxed = true)
        reconnectListener = ReconnectingListener(
            dispatcher = dispatcher,
            chatStateListener = chatStateListener,
            loggerScope = LoggerScope("ReconnectListenerTest", MockLogger()),
            connect = {
                connectCalled++
                throw PermanentConnectionFailureException("Auth failed", RuntimeException("ConsumerReconnectionFailed"))
            }
        )
        reconnectListener.randomDelayProvider = { 1000L }
        reconnectListener.onOpen(webSocket, mockk(relaxed = true)) // wasEverConnected = true
        reconnectListener.onClosing(webSocket, 1001, "Test disconnect")
        advanceUntilIdle()
        // PermanentConnectionFailureException must not be retried — connect() called exactly once.
        assertEquals(1, connectCalled)
        // Permanent failure during reconnect must still surface as onUnexpectedDisconnect so the
        // consumer is not left stuck in the Connecting state.
        verify { chatStateListener.onUnexpectedDisconnect() }
    }

    @Test
    fun `should retry when connect throws TransientConnectFailureException`() = runTest(dispatcher) {
        val webSocket = mockk<WebSocket>(relaxed = true)
        reconnectListener = ReconnectingListener(
            dispatcher = dispatcher,
            chatStateListener = chatStateListener,
            loggerScope = LoggerScope("ReconnectListenerTest", MockLogger()),
            connect = {
                connectCalled++
                throw TransientConnectFailureException("Socket refused", IOException("ECONNREFUSED"))
            }
        )
        reconnectListener.randomDelayProvider = { 1000L }
        reconnectListener.onOpen(webSocket, mockk(relaxed = true)) // wasEverConnected = true
        reconnectListener.onClosing(webSocket, 1001, "Test disconnect")
        advanceUntilIdle()
        // TransientConnectFailureException (transient socket-open failure) must be retried up to MAX_RECONNECT_ATTEMPTS.
        assertEquals(ReconnectingListener.MAX_RECONNECT_ATTEMPTS, connectCalled)
        // After exhausting all attempts the listener must report the disconnect.
        verify { chatStateListener.onUnexpectedDisconnect() }
    }

    @Test
    fun `should retry when connect throws CancellationException (transient socket failure)`() = runTest(dispatcher) {
        val webSocket = mockk<WebSocket>(relaxed = true)
        reconnectListener = ReconnectingListener(
            dispatcher = dispatcher,
            chatStateListener = chatStateListener,
            loggerScope = LoggerScope("ReconnectListenerTest", MockLogger()),
            connect = {
                connectCalled++
                throw CancellationException("Socket failed to open")
            }
        )
        reconnectListener.randomDelayProvider = { 1000L }
        reconnectListener.onOpen(webSocket, mockk(relaxed = true)) // wasEverConnected = true
        reconnectListener.onClosing(webSocket, 1001, "Test disconnect")
        advanceUntilIdle()
        // Transient CancellationException (scope still active) must be retried up to MAX_RECONNECT_ATTEMPTS.
        assertEquals(ReconnectingListener.MAX_RECONNECT_ATTEMPTS, connectCalled)
        // After exhausting all attempts the listener must report the disconnect.
        verify { chatStateListener.onUnexpectedDisconnect() }
    }

    @Test
    fun `should call onConnecting when onFailure occurs after a prior successful connection`() =
        runTest(dispatcher) {
            val webSocket = mockk<WebSocket>(relaxed = true)
            reconnectListener.onOpen(webSocket, mockk(relaxed = true))
            reconnectListener.onFailure(webSocket, Throwable("Network lost"), null)
            advanceUntilIdle()
            verify { chatStateListener.onConnecting() }
        }

    @Test
    fun `should call onConnecting when onClosing is triggered with abnormal code after prior successful connection`() =
        runTest(dispatcher) {
            val webSocket = mockk<WebSocket>(relaxed = true)
            reconnectListener.onOpen(webSocket, mockk(relaxed = true))
            reconnectListener.onClosing(webSocket, 1001, "Abnormal closure")
            advanceUntilIdle()
            verify { chatStateListener.onConnecting() }
        }

    @Test
    fun `should not call onConnecting on initial connection failure`() = runTest(dispatcher) {
        val webSocket = mockk<WebSocket>(relaxed = true)
        reconnectListener.onFailure(webSocket, Throwable("Initial connection failed"), null)
        advanceUntilIdle()
        verify(exactly = 0) { chatStateListener.onConnecting() }
        verify { chatStateListener.onUnexpectedDisconnect() }
    }

    @Test
    fun `should call onUnexpectedDisconnect when max reconnection attempts are exhausted`() =
        runTest(dispatcher) {
            val webSocket = mockk<WebSocket>(relaxed = true)
            reconnectListener.randomDelayProvider = { 1000L }
            reconnectListener.onOpen(webSocket, mockk(relaxed = true))
            repeat(ReconnectingListener.MAX_RECONNECT_ATTEMPTS + 1) {
                reconnectListener.onFailure(webSocket, Throwable("Network fail $it"), null)
                advanceUntilIdle()
            }
            verify { chatStateListener.onUnexpectedDisconnect() }
            assertEquals(ReconnectingListener.MAX_RECONNECT_ATTEMPTS, reconnectListener.reconnectAttempts.get())
        }

    @Test
    fun `should ignore onClosing with abnormal code when the socket was never connected`() = runTest(dispatcher) {
        val webSocket = mockk<WebSocket>(relaxed = true)

        // No onOpen and no onStateChanged — wasEverConnected stays false.
        reconnectListener.onClosing(webSocket, 1001, "Abnormal close before first connection")
        advanceUntilIdle()

        // Without a prior successful connection an abnormal close must be a complete no-op: the
        // socket was never up, so there is nothing to reconnect and nothing to report as lost.
        assertEquals(0, reconnectListener.reconnectAttempts.get())
        assertEquals(0, connectCalled)
        verify(exactly = 0) { chatStateListener.onUnexpectedDisconnect() }
        verify(exactly = 0) { chatStateListener.onConnecting() }
    }

    @Test
    fun `onStateChanged OPEN when never connected marks as connected`() = runTest(dispatcher) {
        reconnectListener.onStateChanged(SocketState.OPEN)
        advanceUntilIdle()
        assertTrue(reconnectListener.getWasEverConnected())
        assertEquals(0, reconnectListener.reconnectAttempts.get())
    }

    @Test
    fun `onStateChanged CONNECTED when never connected marks as connected`() = runTest(dispatcher) {
        reconnectListener.onStateChanged(SocketState.CONNECTED)
        advanceUntilIdle()
        assertTrue(reconnectListener.getWasEverConnected())
        assertEquals(0, reconnectListener.reconnectAttempts.get())
    }

    @Test
    fun `onStateChanged OPEN after first connection enables reconnection on subsequent disconnect`() = runTest(dispatcher) {
        val webSocket = mockk<WebSocket>(relaxed = true)
        // Simulate a connection that never fires onOpen but receives a message (OPEN state from proxy)
        reconnectListener.onStateChanged(SocketState.OPEN)
        advanceUntilIdle()
        // Now a disconnect should trigger reconnection, not onUnexpectedDisconnect
        reconnectListener.onClosing(webSocket, 1001, "Abnormal closure")
        advanceUntilIdle()
        assertTrue(connectCalled >= 1)
        verify { chatStateListener.onConnecting() }
    }

    @Test
    fun `onStateChanged OPEN is idempotent after onOpen already established connection`() = runTest(dispatcher) {
        val webSocket = mockk<WebSocket>(relaxed = true)
        reconnectListener.onOpen(webSocket, mockk(relaxed = true))
        // Set currentDelayMillis to a sentinel so we can detect if onConnected() fires a second time
        // (onConnected() always resets it to -1; if it stays at 500 the guard held)
        reconnectListener.setCurrentDelayMillis(500L)
        reconnectListener.onStateChanged(SocketState.OPEN)
        advanceUntilIdle()
        // If onConnected() ran again it would have reset currentDelayMillis to -1
        assertEquals(500L, reconnectListener.getCurrentDelayMillis())
    }

    @Test
    fun `onStateChanged with non-connected states does not affect wasEverConnected`() = runTest(dispatcher) {
        for (state in listOf(SocketState.INITIAL, SocketState.CONNECTING, SocketState.CLOSING, SocketState.CLOSED)) {
            reconnectListener.onStateChanged(state)
        }
        advanceUntilIdle()
        assertFalse(reconnectListener.getWasEverConnected())
    }

    @Test
    fun `onStateChanged CLOSED does not trigger reconnection`() = runTest(dispatcher) {
        reconnectListener.onStateChanged(SocketState.CLOSED)
        advanceUntilIdle()
        assertEquals(0, connectCalled)
        verify(exactly = 0) { chatStateListener.onConnecting() }
        verify(exactly = 0) { chatStateListener.onUnexpectedDisconnect() }
    }

    @Test
    fun `should treat first connection established via onStateChanged as connected`() = runTest(dispatcher) {
        val webSocket = mockk<WebSocket>(relaxed = true)
        reconnectListener.randomDelayProvider = { 1000L }
        // onStateChanged fires before onOpen (e.g. from ChatSocketConnector.reportState)
        reconnectListener.onStateChanged(SocketState.CONNECTED)
        // Subsequent failure must trigger backoff reconnect, not onUnexpectedDisconnect
        reconnectListener.onFailure(webSocket, Throwable("Network error"), null)
        advanceUntilIdle()
        verify(exactly = 0) { chatStateListener.onUnexpectedDisconnect() }
        verify { chatStateListener.onConnecting() }
        assertEquals(1, reconnectListener.reconnectAttempts.get())
    }

    @Test
    fun `onConnected during an in-flight connect must not cancel the connect coroutine`() = runTest(dispatcher) {
        val webSocket = mockk<WebSocket>(relaxed = true)
        var recoveryCompleted = false
        lateinit var listener: ReconnectingListener
        listener = ReconnectingListener(
            dispatcher = dispatcher,
            chatStateListener = chatStateListener,
            loggerScope = LoggerScope("ReconnectListenerTest", MockLogger()),
            connect = {
                connectCalled++
                // Real connect() opens the socket; OkHttp then fires onOpen -> onConnected() WHILE
                // connect() is still running its post-open live-chat recovery. onConnected() calls
                // reconnectJob.cancel() — and on the autonomous path reconnectJob IS this coroutine.
                listener.onOpen(webSocket, mockk(relaxed = true))
                // Recovery suspends here (await LivechatRecovered) before calling onReady(). If the
                // in-flight connect job was cancelled by onConnected(), this suspension throws and
                // the completion below (the onReady() stand-in) never runs — the DE-172157 B4 bug.
                yield()
                recoveryCompleted = true
            },
        )
        listener.randomDelayProvider = { 1000L }
        listener.onOpen(webSocket, mockk(relaxed = true)) // wasEverConnected = true
        listener.onClosing(webSocket, 1001, "Network drop") // -> autonomous reconnect -> connect()
        advanceUntilIdle()

        assertEquals(1, connectCalled)
        assertTrue(
            recoveryCompleted,
            "connect() must run to completion after the socket opens; onConnected() must not cancel " +
                    "the reconnect job that is actively executing connect() (else post-open recovery/onReady " +
                    "is aborted and the chat is stuck at Connected) — DE-172157 B4"
        )
    }
}
