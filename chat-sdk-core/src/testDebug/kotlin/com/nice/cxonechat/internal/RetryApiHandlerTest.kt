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

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertFailsWith

internal class RetryApiHandlerTest {

    @Test
    fun `should call onSuccess when action succeeds`() = runTest {
        val handler = RetryApiHandler(2, 10)
        var successCalled = false
        handler.executeWithRetry(
            this,
            action = { "ok" },
            onSuccess = { successCalled = true },
            onFailure = { fail("Should not fail") }
        )
        delay(50)
        assertTrue(successCalled)
    }

    @Test
    fun `should call onFailure when action throws and no retries`() = runTest {
        val handler = RetryApiHandler(0, 10)
        var failureCalled = false
        handler.executeWithRetry(
            coroutineScope = this,
            action = { throw RuntimeException("fail") },
            onSuccess = { fail("Should not succeed") },
            onFailure = { failureCalled = true }
        )
        delay(50)
        assertTrue(failureCalled)
    }

    @Test
    fun `should retry and succeed after failure`() = runTest {
        val handler = RetryApiHandler(1, 10)
        var callCount = 0
        var successCalled = false
        handler.executeWithRetry(
            coroutineScope = this,
            action = {
                callCount++
                if (callCount == 1) throw RuntimeException("fail")
                "success"
            },
            onSuccess = { successCalled = true },
            onFailure = { fail("Should not fail") }
        )
        delay(100)
        assertTrue(successCalled)
        assertEquals(2, callCount)
    }

    @Test
    fun `should call onFailure after all retries fail`() = runTest {
        val handler = RetryApiHandler(2, 10)
        var failureCalled = false
        var callCount = 0
        handler.executeWithRetry(
            coroutineScope = this,
            action = {
                callCount++
                throw RuntimeException("fail")
            },
            onSuccess = { fail("Should not succeed") },
            onFailure = { failureCalled = true }
        )
        delay(100)
        assertTrue(failureCalled)
        assertEquals(3, callCount)
    }

    @Test
    fun `should not execute if cancelled before start`() = runTest {
        val handler = RetryApiHandler(2, 10)
        var called = false
        assertFailsWith<CancellationException> {
            coroutineScope {
                cancel("test")
                handler.executeWithRetry(
                    coroutineScope = this,
                    action = { called = true },
                    onSuccess = {},
                    onFailure = {}
                )
            }
        }
        assertFalse(called)
    }

    @Test(
        expected = CancellationException::class
    )
    fun `should cancel scheduled retries`() = runTest {
        val handler = RetryApiHandler(2, 100)
        var callCount = 0
        val latch = CountDownLatch(1)
        handler.executeWithRetry(
            coroutineScope = this,
            action = {
                callCount++
                if (callCount == 1) {
                    cancel("test")
                    throw RuntimeException("fail")
                }
                "success"
            },
            onSuccess = { latch.countDown() },
            onFailure = { latch.countDown() }
        )
        latch.await(200, TimeUnit.MILLISECONDS)
        assertEquals(1, callCount)
    }

    @Test
    fun `should propagate CancellationException from action instead of retrying`() = runTest {
        val handler = RetryApiHandler(3, 10)
        var callCount = 0
        assertFailsWith<CancellationException> {
            handler.executeWithRetry(
                coroutineScope = this,
                action = {
                    callCount++
                    throw CancellationException("cancelled")
                },
                onSuccess = { fail("Should not succeed") },
                onFailure = { fail("Should not fail") }
            )
        }
        assertEquals(1, callCount)
    }

    @Test
    fun `should handle negative retryIntervalMs gracefully`() = runTest {
        val handler = RetryApiHandler(1, -10)
        var callCount = 0
        var successCalled = false
        handler.executeWithRetry(
            coroutineScope = this,
            action = {
                callCount++
                if (callCount == 1) throw RuntimeException("fail")
                "success"
            },
            onSuccess = { successCalled = true },
            onFailure = { fail("Should not fail") }
        )
        delay(50)
        assertTrue(successCalled)
        assertEquals(2, callCount)
    }
}
