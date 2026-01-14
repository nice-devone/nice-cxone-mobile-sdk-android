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

import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.ChatStateListener
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.tool.MockLogger
import io.mockk.clearMocks
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.WebSocket
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
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
                Cancellable.noop
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
        assertEquals(0, reconnectListener.getReconnectAttempts())
        assertTrue(connectCalled == 0)
    }

    @Test
    fun `should attempt exponential backoff reconnect after successful connection`() = runTest(dispatcher) {
        val webSocket = mockk<WebSocket>(relaxed = true)
        reconnectListener.onOpen(webSocket, mockk(relaxed = true))
        reconnectListener.onClosing(webSocket, 1001, "Test disconnect") // abnormal closure
        advanceUntilIdle()
        assertTrue(reconnectListener.getReconnectAttempts() >= 1)
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
        assertEquals(ReconnectingListener.MAX_RECONNECT_ATTEMPTS, reconnectListener.getReconnectAttempts())
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
        assertTrue(reconnectJob == null || (reconnectJob as Job).isCancelled)
    }

    @Test
    fun `should not attempt reconnect when closed with CLOSE_NORMAL_CODE`() = runTest(dispatcher) {
        val webSocket = mockk<WebSocket>(relaxed = true)
        reconnectListener.onOpen(webSocket, mockk(relaxed = true))
        // CLOSE_NORMAL_CODE is typically 1000
        reconnectListener.onClosing(webSocket, 1000, "Normal closure")
        advanceUntilIdle()
        assertEquals(0, reconnectListener.getReconnectAttempts())
        assertTrue(connectCalled == 0)
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
        assertEquals(0, reconnectListener.getReconnectAttempts())
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
        val attempts = reconnectListener.getReconnectAttempts()
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
        assertEquals(0, reconnectListener.getReconnectAttempts())
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
        assertEquals(ReconnectingListener.MAX_RECONNECT_ATTEMPTS, reconnectListener.getReconnectAttempts())
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
    fun `should set cancellable when connect is called`() = runTest(dispatcher) {
        val webSocket = mockk<WebSocket>(relaxed = true)
        val mockCancellable = mockk<Cancellable>(relaxed = true)
        reconnectListener = ReconnectingListener(
            dispatcher = dispatcher,
            chatStateListener = chatStateListener,
            loggerScope = LoggerScope("ReconnectListenerTest", MockLogger()),
            connect = {
                connectCalled++
                mockCancellable
            }
        )
        reconnectListener.randomDelayProvider = { 1000L }
        reconnectListener.onOpen(webSocket, mockk(relaxed = true))
        reconnectListener.onClosing(webSocket, 1001, "Test disconnect")
        advanceUntilIdle()
        val cancellableField = ReconnectingListener::class.java.getDeclaredField("cancellable").apply { isAccessible = true }
        val cancellable = cancellableField.get(reconnectListener)
        assertTrue(cancellable === mockCancellable)
    }

    @Test
    fun `should cancel cancellable on close`() = runTest(dispatcher) {
        val webSocket = mockk<WebSocket>(relaxed = true)
        val mockCancellable = mockk<Cancellable>(relaxed = true)
        justRun { mockCancellable.cancel() }
        reconnectListener = ReconnectingListener(
            dispatcher = dispatcher,
            chatStateListener = chatStateListener,
            loggerScope = LoggerScope("ReconnectListenerTest", MockLogger()),
            connect = {
                connectCalled++
                mockCancellable
            }
        )
        reconnectListener.randomDelayProvider = { 1000L }
        reconnectListener.onOpen(webSocket, mockk(relaxed = true))
        reconnectListener.onClosing(webSocket, 1001, "Test disconnect")
        advanceUntilIdle()
        reconnectListener.close()
        verify { mockCancellable.cancel() }
    }

    @Test
    fun `should reset cancellable to null on connection events`() = runTest(dispatcher) {
        val webSocket = mockk<WebSocket>(relaxed = true)
        val mockCancellable = mockk<Cancellable>(relaxed = true)
        reconnectListener = ReconnectingListener(
            dispatcher = dispatcher,
            chatStateListener = chatStateListener,
            loggerScope = LoggerScope("ReconnectListenerTest", MockLogger()),
            connect = {
                connectCalled++
                mockCancellable
            }
        )
        reconnectListener.randomDelayProvider = { 1000L }
        reconnectListener.onOpen(webSocket, mockk(relaxed = true))
        reconnectListener.onClosing(webSocket, 1001, "Test disconnect")
        advanceUntilIdle()
        reconnectListener.onOpen(webSocket, mockk(relaxed = true))
        val cancellableField = ReconnectingListener::class.java.getDeclaredField("cancellable").apply { isAccessible = true }
        val cancellable = cancellableField.get(reconnectListener)
        assertTrue(cancellable == null)
        reconnectListener.onFailure(webSocket, Throwable("fail"), null)
        val cancellableAfterFailure = cancellableField.get(reconnectListener)
        assertTrue(cancellableAfterFailure == null)
        reconnectListener.onClosing(webSocket, 1001, "Test disconnect")
        val cancellableAfterClosed = cancellableField.get(reconnectListener)
        assertTrue(cancellableAfterClosed == null)
    }

    private companion object {
        fun ReconnectingListener.getReconnectAttempts() = ReconnectingListener::class.java
            .getDeclaredField("reconnectAttempts")
            .apply { isAccessible = true }
            .get(this) as Int
    }

    private class TestException(message: String) : Exception(message)
}
