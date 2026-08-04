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

import com.nice.cxonechat.ChatStateListener
import com.nice.cxonechat.internal.socket.SocketState
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.tool.MockLogger
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import okhttp3.WebSocket
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
internal class ReconnectListenerTest {
    private lateinit var dispatcher: TestDispatcher
    private lateinit var chatStateListener: ChatStateListener
    private var connectCalled: Int = 0
    private lateinit var reconnectListener: ReconnectingListener

    @Before
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        chatStateListener = mockk(relaxed = true)
        connectCalled = 0
        reconnectListener = ReconnectingListener(
            dispatcher = dispatcher,
            chatStateListener = chatStateListener,
            loggerScope = LoggerScope("ReconnectListenerTest", MockLogger()),
            connect = {
                connectCalled++
            }
        )
    }

    @After
    fun tearDown() {
        clearMocks(chatStateListener)
    }

    @Test
    fun `should notify integrator immediately on initial connection failure and not reconnect`() = runTest(dispatcher) {
        val webSocket = mockk<WebSocket>(relaxed = true)
        val throwable = Throwable("Initial connection failed")
        reconnectListener.onFailure(webSocket, throwable, null)
        advanceUntilIdle()
        verify { chatStateListener.onUnexpectedDisconnect() }
        assertEquals(0, reconnectListener.reconnectAttempts.get())
        assertEquals(0, connectCalled)
    }

    @Test
    fun `should attempt exponential backoff reconnect after successful connection`() = runTest(dispatcher) {
        val webSocket = mockk<WebSocket>(relaxed = true)
        reconnectListener.onOpen(webSocket, mockk(relaxed = true))
        reconnectListener.onClosing(webSocket, 1001, "Test disconnect") // abnormal closure
        advanceUntilIdle()
        assertTrue(reconnectListener.reconnectAttempts.get() >= 1)
        assertTrue(connectCalled >= 1)
    }

    @Test
    fun `should stop reconnecting after 20 attempts`() = runTest(dispatcher) {
        val webSocket = mockk<WebSocket>(relaxed = true)
        reconnectListener.onOpen(webSocket, mockk(relaxed = true))
        repeat(ReconnectingListener.MAX_RECONNECT_ATTEMPTS + 1) {
            reconnectListener.onClosing(webSocket, 1001, "Test disconnect $it")
            advanceUntilIdle()
        }
        assertEquals(ReconnectingListener.MAX_RECONNECT_ATTEMPTS, reconnectListener.reconnectAttempts.get())
        assertEquals(ReconnectingListener.MAX_RECONNECT_ATTEMPTS, connectCalled)
    }

    @Test
    fun `should cancel reconnect job on close`() = runTest(dispatcher) {
        val webSocket = mockk<WebSocket>(relaxed = true)
        reconnectListener.onOpen(webSocket, mockk(relaxed = true))
        reconnectListener.onClosing(webSocket, 1001, "Test disconnect")
        reconnectListener.close()
        advanceUntilIdle()
        val reconnectJob = reconnectListener.reconnectJob
        assertTrue(reconnectJob == null || reconnectJob.isCancelled)
    }

    @Test
    fun `should not attempt reconnect when closed with CLOSE_NORMAL_CODE`() = runTest(dispatcher) {
        val webSocket = mockk<WebSocket>(relaxed = true)
        reconnectListener.onOpen(webSocket, mockk(relaxed = true))
        // CLOSE_NORMAL_CODE is typically 1000
        reconnectListener.onClosing(webSocket, 1000, "Normal closure")
        advanceUntilIdle()
        assertEquals(0, reconnectListener.reconnectAttempts.get())
        assertEquals(0, connectCalled)
    }

    @Test
    fun `initial reconnect delay should be 1s plus random 1-5s`() = runTest(dispatcher) {
        val webSocket = mockk<WebSocket>(relaxed = true)
        // Set deterministic random delay for test
        val randomDelay = 3000L
        reconnectListener.randomDelayProvider = { randomDelay } // simulate random value 3s
        reconnectListener.onOpen(webSocket, mockk(relaxed = true))
        reconnectListener.onClosing(webSocket, 1001, "Test disconnect")
        advanceUntilIdle()
        val delayField = ReconnectingListener::class.java.getDeclaredField("currentDelayMillis").apply { isAccessible = true }
        val delay = delayField.get(reconnectListener) as Long
        assertEquals(ReconnectingListener.INITIAL_DELAY + randomDelay, delay) // 1000 + 3000
    }

    @Test
    fun `subsequent reconnect delays should increase exponentially by factor 1_3`() = runTest(dispatcher) {
        val webSocket = mockk<WebSocket>(relaxed = true)
        val randomDelay = 2000L
        reconnectListener.randomDelayProvider = { randomDelay } // first random delay 2s
        reconnectListener.onOpen(webSocket, mockk(relaxed = true))
        // First attempt
        reconnectListener.onClosing(webSocket, 1001, "Test disconnect 1")
        advanceUntilIdle()
        val delayField = ReconnectingListener::class.java.getDeclaredField("currentDelayMillis").apply { isAccessible = true }
        val firstDelay = delayField.get(reconnectListener) as Long
        // Second attempt
        reconnectListener.onClosing(webSocket, 1001, "Test disconnect 2")
        advanceUntilIdle()
        val secondDelay = delayField.get(reconnectListener) as Long
        assertEquals(ReconnectingListener.INITIAL_DELAY + randomDelay, firstDelay) // 1000 + 2000
        assertEquals((firstDelay * 1.3).toLong(), secondDelay)
    }

    @Test
    fun `random delay is only used for first reconnect attempt`() = runTest(dispatcher) {
        val webSocket = mockk<WebSocket>(relaxed = true)
        var randomCalled = 0
        reconnectListener.randomDelayProvider = {
            randomCalled++
            4000L
        }
        reconnectListener.onOpen(webSocket, mockk(relaxed = true))
        // First attempt
        reconnectListener.onClosing(webSocket, 1001, "Test disconnect 1")
        advanceUntilIdle()
        // Second attempt
        reconnectListener.onClosing(webSocket, 1001, "Test disconnect 2")
        advanceUntilIdle()
        assertEquals(1, randomCalled)
    }

    @Test
    fun `should cap exponential backoff delay at 500_000 ms`() = runTest(dispatcher) {
        val webSocket = mockk<WebSocket>(relaxed = true)
        reconnectListener.randomDelayProvider = { 5000L }
        reconnectListener.onOpen(webSocket, mockk(relaxed = true))
        // Simulate enough reconnects to exceed cap
        var lastDelay: Long = 0
        for (i in 0..25) {
            reconnectListener.onClosing(webSocket, 1001, "Test disconnect $i")
            advanceUntilIdle()
            val delayField = ReconnectingListener::class.java.getDeclaredField("currentDelayMillis").apply { isAccessible = true }
            lastDelay = delayField.get(reconnectListener) as Long
        }
        assertTrue(lastDelay <= ReconnectingListener.MAX_BACKOFF)
    }

    @Test
    fun `should not reconnect after close is called`() = runTest(dispatcher) {
        val webSocket = mockk<WebSocket>(relaxed = true)
        reconnectListener.onOpen(webSocket, mockk(relaxed = true))
        reconnectListener.close()
        reconnectListener.onClosing(webSocket, 1001, "Test disconnect after close")
        advanceUntilIdle()
        assertEquals(0, reconnectListener.reconnectAttempts.get())
        assertEquals(0, connectCalled)
    }

    @Test
    fun `random delay for first attempt should be within 1000-5000 ms`() = runTest(dispatcher) {
        val webSocket = mockk<WebSocket>(relaxed = true)
        reconnectListener.randomDelayProvider = { 4999L }
        reconnectListener.onOpen(webSocket, mockk(relaxed = true))
        reconnectListener.onClosing(webSocket, 1001, "Test disconnect")
        advanceUntilIdle()
        val delayField = ReconnectingListener::class.java.getDeclaredField("currentDelayMillis").apply { isAccessible = true }
        val delay = delayField.get(reconnectListener) as Long
        assertTrue(delay in 2000L..6000L) // 1000 + 1000..5000
    }

    @Test
    fun `should reset attempts and delay on successful connection`() = runTest(dispatcher) {
        val webSocket = mockk<WebSocket>(relaxed = true)
        reconnectListener.randomDelayProvider = { 2000L }
        reconnectListener.onOpen(webSocket, mockk(relaxed = true))
        reconnectListener.onClosing(webSocket, 1001, "Test disconnect")
        advanceUntilIdle()
        reconnectListener.onOpen(webSocket, mockk(relaxed = true)) // simulate reconnect success
        val attempts = reconnectListener.reconnectAttempts.get()
        val delayField = ReconnectingListener::class.java.getDeclaredField("currentDelayMillis").apply { isAccessible = true }
        val delay = delayField.get(reconnectListener) as Long
        assertEquals(0, attempts)
        assertEquals(-1L, delay)
    }

    @Test
    fun `should not reconnect if never connected and multiple failures occur`() = runTest(dispatcher) {
        val webSocket = mockk<WebSocket>(relaxed = true)
        for (i in 0..5) {
            reconnectListener.onFailure(webSocket, Throwable("fail $i"), null)
            advanceUntilIdle()
        }
        assertEquals(0, reconnectListener.reconnectAttempts.get())
        assertEquals(0, connectCalled)
    }

    @Test
    fun `should not reconnect after max attempts reached`() = runTest(dispatcher) {
        val webSocket = mockk<WebSocket>(relaxed = true)
        reconnectListener.randomDelayProvider = { 1000L }
        reconnectListener.onOpen(webSocket, mockk(relaxed = true))
        repeat(ReconnectingListener.MAX_RECONNECT_ATTEMPTS) {
            reconnectListener.onClosing(webSocket, 1001, "Test disconnect $it")
            advanceUntilIdle()
        }
        // Try one more abnormal closure
        reconnectListener.onClosing(webSocket, 1001, "Test disconnect after max")
        advanceUntilIdle()
        assertEquals(ReconnectingListener.MAX_RECONNECT_ATTEMPTS, reconnectListener.reconnectAttempts.get())
        assertEquals(ReconnectingListener.MAX_RECONNECT_ATTEMPTS, connectCalled)
    }

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
    fun `should abort when scope is cancelled mid-backoff (isActive=false path)`() = runTest(dispatcher) {
        val webSocket = mockk<WebSocket>(relaxed = true)
        reconnectListener.randomDelayProvider = { 1000L }
        reconnectListener.onOpen(webSocket, mockk(relaxed = true))
        reconnectListener.onClosing(webSocket, 1001, "Test disconnect")
        // Reconnect job is now paused inside delay() — connect() has not been called yet.
        // Cancel the scope before advancing time to simulate SDK shutdown mid-backoff.
        reconnectListener.close()
        advanceUntilIdle()
        // Scope cancellation CancellationException (isActive=false) must not trigger connect() or a retry.
        assertEquals(0, connectCalled)
        verify(exactly = 0) { chatStateListener.onUnexpectedDisconnect() }
    }

    @Test
    fun `should call onConnecting when onFailure occurs after a prior successful connection`() = runTest(dispatcher) {
        val webSocket = mockk<WebSocket>(relaxed = true)
        reconnectListener.onOpen(webSocket, mockk(relaxed = true))
        reconnectListener.onFailure(webSocket, Throwable("Network lost"), null)
        advanceUntilIdle()
        verify { chatStateListener.onConnecting() }
    }

    @Test
    fun `should call onConnecting when onClosing is triggered with abnormal code after prior successful connection`() = runTest(dispatcher) {
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
    fun `should call onUnexpectedDisconnect when max reconnection attempts are exhausted`() = runTest(dispatcher) {
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

    // region onStateChanged (SocketStateListener path)

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

    // endregion

    private companion object {
        fun ReconnectingListener.getWasEverConnected() = ReconnectingListener::class.java
            .getDeclaredField("wasEverConnected")
            .apply { isAccessible = true }
            .let { field ->
                (field.get(this) as java.util.concurrent.atomic.AtomicBoolean).get()
            }

        fun ReconnectingListener.getCurrentDelayMillis() = ReconnectingListener::class.java
            .getDeclaredField("currentDelayMillis")
            .apply { isAccessible = true }
            .getLong(this)

        fun ReconnectingListener.setCurrentDelayMillis(value: Long) {
            ReconnectingListener::class.java
                .getDeclaredField("currentDelayMillis")
                .apply { isAccessible = true }
                .setLong(this, value)
        }
    }

    private class TestException(message: String) : Exception(message)
}
