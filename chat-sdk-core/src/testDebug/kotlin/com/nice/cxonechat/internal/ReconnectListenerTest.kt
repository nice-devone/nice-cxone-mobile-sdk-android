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

import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.WebSocket
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Reconnect-decision behavior of [ReconnectingListener]: when it must not reconnect, backoff
 * attempt counting/caps, and close/scope-cancel handling. Delay computation lives in
 * [ReconnectListenerDelayTest]; connect-exception, listener callbacks, and the onStateChanged path
 * live in [ReconnectListenerStateChangeTest].
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class ReconnectListenerTest : AbstractReconnectListenerTest() {

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
}
