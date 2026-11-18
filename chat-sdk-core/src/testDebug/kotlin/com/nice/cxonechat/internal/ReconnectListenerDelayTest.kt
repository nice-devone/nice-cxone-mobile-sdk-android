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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.WebSocket
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Exponential-backoff delay computation of [ReconnectingListener] (initial delay, growth factor,
 * cap, random-jitter bounds, and reset on success). Reconnect-decision behavior lives in
 * [ReconnectListenerTest].
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class ReconnectListenerDelayTest : AbstractReconnectListenerTest() {

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
}
