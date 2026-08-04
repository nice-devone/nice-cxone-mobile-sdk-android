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

import com.nice.cxonechat.log.Level
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class CoroutineExtTest {

    private class RecordingLogger : Logger {
        val records = mutableListOf<Pair<String, Throwable?>>()
        override fun log(level: Level, message: String, throwable: Throwable?) {
            records += message to throwable
        }
    }

    private val noopLogger = object : Logger {
        override fun log(level: Level, message: String, throwable: Throwable?) = Unit
    }

    // ── safeLaunch ────────────────────────────────────────────────────────────

    @Test
    fun safeLaunch_executesBlock() = runTest {
        var executed = false
        val scope = LoggerScope("Test", noopLogger)

        safeLaunch(scope) { executed = true }.join()

        assertTrue(executed)
    }

    @Test
    fun safeLaunch_logsExceptionWithoutPropagating() = runTest {
        val logger = RecordingLogger()
        val scope = LoggerScope("MyClass", logger)
        val error = RuntimeException("boom")

        safeLaunch(scope) { throw error }.join()

        assertEquals(1, logger.records.size)
        assertEquals(error, logger.records[0].second)
    }

    @Test
    fun safeLaunch_logMessageContainsScopeTag() = runTest {
        val logger = RecordingLogger()
        val scope = LoggerScope("MyClass/myOp", logger)

        safeLaunch(scope) { throw RuntimeException() }.join()

        assertTrue(logger.records[0].first.contains("[MyClass/myOp]"))
    }

    @Test
    fun safeLaunch_cancellationExceptionCancelsJobWithoutLogging() = runTest {
        val logger = RecordingLogger()
        val scope = LoggerScope("Test", logger)

        // CancellationException re-thrown inside launch cancels the job,
        // but does NOT fail the parent scope and is NOT logged.
        val job = safeLaunch(scope) { throw CancellationException("cancelled") }
        job.join()

        assertTrue(job.isCancelled)
        assertTrue(logger.records.isEmpty())
    }

    // ── childScope ────────────────────────────────────────────────────────────

    @Test
    fun childScope_composesTagFromParent() {
        val parent = LoggerScope("Parent", noopLogger)
        val child = parent.childScope("op")

        assertEquals("Parent/op", child.scope)
    }

    @Test
    fun childScope_preservesIdentityLogger() {
        val logger = RecordingLogger()
        val parent = LoggerScope("Parent", logger)
        val child = parent.childScope("op")

        child.log(Level.Warning, "msg", null)

        assertNotNull(logger.records.find { it.first.contains("msg") })
    }

    // ── trySendOrLog ──────────────────────────────────────────────────────────

    @Test
    fun trySendOrLog_doesNotLogWhenSendSucceeds() = runTest {
        val logger = RecordingLogger()
        val scope = LoggerScope("Test", logger)
        val channel = Channel<Int>(capacity = 1)

        trySendOrLog(channel, 42, scope, "counter")

        assertTrue(logger.records.isEmpty())
        channel.close()
    }

    @Test
    fun trySendOrLog_logsWarningWhenChannelIsFullButOpen() = runTest {
        val logger = RecordingLogger()
        val scope = LoggerScope("Test", logger)
        val channel = Channel<Int>(capacity = 1)
        channel.trySend(1) // fill the buffer

        trySendOrLog(channel, 2, scope, "counter update")

        assertEquals(1, logger.records.size)
        assertTrue(logger.records[0].first.contains("counter update"))
    }

    @Test
    fun trySendOrLog_doesNotLogWhenChannelIsClosed() = runTest {
        val logger = RecordingLogger()
        val scope = LoggerScope("Test", logger)
        val channel = Channel<Int>(capacity = 1)
        channel.close()

        trySendOrLog(channel, 42, scope, "counter")

        assertTrue(logger.records.isEmpty())
    }

    // ── safeCollect ───────────────────────────────────────────────────────────

    @Test
    fun safeCollect_invokesActionForEachEmission() = runTest {
        val logger = RecordingLogger()
        val scope = LoggerScope("Test", logger)
        val received = mutableListOf<Int>()

        flow { emit(1); emit(2); emit(3) }.safeCollect(scope) { received.add(it) }

        assertEquals(listOf(1, 2, 3), received)
        assertTrue(logger.records.isEmpty())
    }

    @Test
    fun safeCollect_logsExceptionAndContinuesCollecting() = runTest {
        val logger = RecordingLogger()
        val scope = LoggerScope("Test", logger)
        val received = mutableListOf<Int>()

        flow { emit(1); emit(2); emit(3) }.safeCollect(scope) { value ->
            if (value == 2) throw RuntimeException("bad value")
            received.add(value)
        }

        assertEquals(listOf(1, 3), received)
        assertEquals(1, logger.records.size)
    }

    @Test
    fun safeCollect_rethrowsCancellationException() = runTest {
        val logger = RecordingLogger()
        val scope = LoggerScope("Test", logger)

        assertFailsWith<CancellationException> {
            flow { emit(1) }.safeCollect(scope) { throw CancellationException("cancelled") }
        }
        assertTrue(logger.records.isEmpty())
    }

    // ── mapFailure ────────────────────────────────────────────────────────────

    @Test
    fun `mapFailure passes success value through unchanged`() {
        val result = Result.success(42)
        val mapped = result.mapFailure { RuntimeException("should not be called") }
        assertEquals(Result.success(42), mapped)
    }

    @Test
    fun `mapFailure transforms non-CancellationException failure`() {
        val original = IllegalStateException("original")
        val replacement = RuntimeException("replacement")
        val result = Result.failure<Int>(original)
        val mapped = result.mapFailure { replacement }
        assertEquals(replacement, mapped.exceptionOrNull())
    }

    @Test
    fun `mapFailure does not transform CancellationException`() {
        val cancellation = CancellationException("cancelled")
        val result = Result.failure<Int>(cancellation)
        val mapped = result.mapFailure { RuntimeException("should not replace") }
        assertFailsWith<CancellationException> { mapped.getOrThrow() }
    }
}
