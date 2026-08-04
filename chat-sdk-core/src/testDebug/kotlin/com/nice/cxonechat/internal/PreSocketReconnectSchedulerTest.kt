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

import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.tool.MockLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
internal class PreSocketReconnectSchedulerTest {
    private lateinit var dispatcher: TestDispatcher
    private lateinit var preSocketReconnectScheduler: PreSocketReconnectScheduler
    private var connectCalled = 0
    private var exhaustedCalled = 0
    private val cause = ChannelAvailabilityFailedException("Offline", IOException("Unable to resolve host"))

    @Before
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        connectCalled = 0
        exhaustedCalled = 0
        preSocketReconnectScheduler = PreSocketReconnectScheduler(
            dispatcher = dispatcher,
            loggerScope = LoggerScope("PreSocketReconnectSchedulerTest", MockLogger()),
            connect = { connectCalled++ },
            onExhausted = { exhaustedCalled++ },
        )
        preSocketReconnectScheduler.randomDelayProvider = { 0L }
    }

    /**
     * Simulates what ChatInstanceProvider.doConnect() does in production: catch a repeated
     * ChannelAvailabilityFailedException and re-report it to the scheduler.
     */
    private fun reportFailureAndAdvance() {
        preSocketReconnectScheduler.onPreSocketConnectFailure(cause)
        dispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `onExhausted is invoked once MAX_RECONNECT_ATTEMPTS consecutive failures are reported`() {
        repeat(ReconnectBackoff.MAX_RECONNECT_ATTEMPTS) {
            reportFailureAndAdvance()
        }
        assertEquals(ReconnectBackoff.MAX_RECONNECT_ATTEMPTS, connectCalled)
        assertEquals(0, exhaustedCalled)

        // One more failure past the limit — no further retry, onExhausted fires exactly once.
        reportFailureAndAdvance()

        assertEquals(ReconnectBackoff.MAX_RECONNECT_ATTEMPTS, connectCalled)
        assertEquals(1, exhaustedCalled)
    }

    @Test
    fun `exhaustion re-arms the scheduler instead of permanently disabling auto-retry`() {
        repeat(ReconnectBackoff.MAX_RECONNECT_ATTEMPTS + 1) {
            reportFailureAndAdvance()
        }
        assertEquals(1, exhaustedCalled)
        // Re-armed: a later, independent failure gets its own fresh attempt budget.
        assertEquals(0, preSocketReconnectScheduler.attempts.get())

        reportFailureAndAdvance()

        assertEquals(ReconnectBackoff.MAX_RECONNECT_ATTEMPTS + 1, connectCalled)
        assertEquals(1, exhaustedCalled)
    }

    @Test
    fun `reset clears attempts so a later failure schedules attempt 1 again, not a continuation`() {
        repeat(3) { reportFailureAndAdvance() }
        assertEquals(3, preSocketReconnectScheduler.attempts.get())

        preSocketReconnectScheduler.reset()

        assertEquals(0, preSocketReconnectScheduler.attempts.get())
        assertEquals(-1L, preSocketReconnectScheduler.currentDelayMillis)
    }

    @Test
    fun `reset immediately after scheduling leaves currentDelayMillis at -1, even once the cancelled job would have run`() {
        preSocketReconnectScheduler.onPreSocketConnectFailure(cause)
        // First attempt: INITIAL_DELAY + randomDelayProvider() = 1000 + 0.
        assertEquals(ReconnectBackoff.INITIAL_DELAY, preSocketReconnectScheduler.currentDelayMillis)

        preSocketReconnectScheduler.reset()
        // currentDelayMillis is only ever written synchronously before scheduling, never inside
        // the coroutine — advancing must not resurrect a stale value.
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(-1L, preSocketReconnectScheduler.currentDelayMillis)
        assertEquals(0, connectCalled)
    }

    @Test
    fun `a reset landing after ensureActive but before connect still prevents the stale retry`() {
        preSocketReconnectScheduler.onPreSocketConnectFailure(cause)
        // Bump generation directly (not via reset()/cancel()) to isolate the generation-check path.
        preSocketReconnectScheduler.generation++

        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, connectCalled)
    }
}
