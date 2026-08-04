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
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class SocketConnectionListenerTest {

    @Test
    fun `onOpen delegates to onConnected`() {
        var connected = false
        val listener = SocketConnectionListener(
            onConnected = { connected = true },
            onFailed = { throw AssertionError("onFailed should not be called") },
        )
        listener.onOpen(mockk(), mockk())
        assertTrue(connected)
    }

    @Test
    fun `onFailure delegates to onFailed`() {
        var failedWith: Throwable? = null
        val expected = RuntimeException("test")
        val listener = SocketConnectionListener(
            onConnected = { throw AssertionError("onConnected should not be called") },
            onFailed = { failedWith = it },
        )
        listener.onFailure(mockk(), expected, null)
        assertEquals(expected, failedWith)
    }

    @Test
    fun `onClosing delegates to onDisconnected`() {
        var closedCode = -1
        var closedReason = ""
        val listener = SocketConnectionListener(
            onConnected = { throw AssertionError("onConnected should not be called") },
            onFailed = { throw AssertionError("onFailed should not be called") },
            onDisconnected = { code, reason -> closedCode = code; closedReason = reason },
        )
        listener.onClosing(mockk(), 1001, "going away")
        assertEquals(1001, closedCode)
        assertEquals("going away", closedReason)
    }

    @Test
    fun `onClosed delegates to onDisconnected`() {
        var closedCode = -1
        var closedReason = ""
        val listener = SocketConnectionListener(
            onConnected = { throw AssertionError("onConnected should not be called") },
            onFailed = { throw AssertionError("onFailed should not be called") },
            onDisconnected = { code, reason -> closedCode = code; closedReason = reason },
        )
        listener.onClosed(mockk(), 1000, "normal closure")
        assertEquals(1000, closedCode)
        assertEquals("normal closure", closedReason)
    }
}