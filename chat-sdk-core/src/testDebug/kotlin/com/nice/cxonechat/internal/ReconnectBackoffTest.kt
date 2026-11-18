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

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies the shared exponential-backoff formula in [ReconnectBackoff].
 *
 * Both [ReconnectingListener] (socket-level) and [PreSocketReconnectScheduler] (pre-socket REST)
 * delegate their delay computation to [ReconnectBackoff.nextDelayMillis]. A bug in the formula
 * breaks both reconnect mechanisms simultaneously, so a direct, formula-level regression test is
 * more protective than relying solely on the higher-level scheduler/listener tests.
 */
internal class ReconnectBackoffTest {

    // Use a fixed jitter of 0 for deterministic expectations on the first attempt.
    private val noJitter: () -> Long = { 0L }

    @Test
    fun `first attempt delay equals INITIAL_DELAY plus randomDelay`() {
        val jitter = 2000L

        val delay = ReconnectBackoff.nextDelayMillis(
            attempt = 0,
            previousDelayMillis = -1L,
            randomDelay = { jitter },
        )

        assertEquals(ReconnectBackoff.INITIAL_DELAY + jitter, delay)
    }

    @Test
    fun `first attempt with zero jitter equals INITIAL_DELAY`() {
        val delay = ReconnectBackoff.nextDelayMillis(
            attempt = 0,
            previousDelayMillis = -1L,
            randomDelay = noJitter,
        )

        assertEquals(ReconnectBackoff.INITIAL_DELAY, delay)
    }

    @Test
    fun `subsequent attempt multiplies previous delay by 1_3`() {
        val previousDelay = 4000L

        val delay = ReconnectBackoff.nextDelayMillis(
            attempt = 1,
            previousDelayMillis = previousDelay,
            randomDelay = noJitter, // ignored for attempt > 0
        )

        // Stated literally rather than as (previousDelay * 1.3).toLong(): re-deriving the
        // expectation from the production formula would make the test pass for a wrong multiplier.
        assertEquals(5200L, delay, "4000ms * 1.3 truncates to 5200ms")
    }

    @Test
    fun `randomDelay is not used for attempts after the first`() {
        // randomDelay must be ignored when attempt > 0 — its inclusion would break the
        // deterministic exponential growth and violate the backend's retry-rate contract.
        var randomCalled = false
        val flaggingRandom = {
            randomCalled = true
            9999L
        }

        ReconnectBackoff.nextDelayMillis(
            attempt = 2,
            previousDelayMillis = 3000L,
            randomDelay = flaggingRandom,
        )

        assertFalse(randomCalled, "randomDelay must only be called for the first attempt (attempt == 0)")
    }

    @Test
    fun `delay is capped at MAX_BACKOFF when exponential growth exceeds it`() {
        // A large previousDelayMillis * 1.3 would far exceed MAX_BACKOFF without the cap.
        val hugeDelay = ReconnectBackoff.MAX_BACKOFF

        val delay = ReconnectBackoff.nextDelayMillis(
            attempt = 5,
            previousDelayMillis = hugeDelay,
            randomDelay = noJitter,
        )

        assertEquals(ReconnectBackoff.MAX_BACKOFF, delay, "Delay must not exceed MAX_BACKOFF")
    }

    @Test
    fun `delay is not capped while the product stays below MAX_BACKOFF`() {
        // Pins the branch rather than the boundary: proves a sub-cap product passes through at all
        // instead of being pinned to the ceiling. 384_615 * 1.3 == 499_999.5 -> 499_999.
        val justBelowCap = 384_615L

        val delay = ReconnectBackoff.nextDelayMillis(
            attempt = 1,
            previousDelayMillis = justBelowCap,
            randomDelay = noJitter,
        )

        assertEquals(499_999L, delay, "A product below MAX_BACKOFF must pass through uncapped")
    }

    @Test
    fun `delay equals MAX_BACKOFF when the product lands exactly on the cap`() {
        // The boundary-sensitive case, bracketing the cap from the other side: 384_616 * 1.3 ==
        // 500_000.8 truncates to exactly MAX_BACKOFF. Any clamp tighter than coerceAtMost(MAX_BACKOFF)
        // — an off-by-one ceiling, or a `<` where `<=` is meant — returns 499_999 here and fails.
        val exactlyAtCap = 384_616L

        val delay = ReconnectBackoff.nextDelayMillis(
            attempt = 1,
            previousDelayMillis = exactlyAtCap,
            randomDelay = noJitter,
        )

        assertEquals(ReconnectBackoff.MAX_BACKOFF, delay, "A product equal to the cap must not be clamped below it")
    }

    @Test
    fun `defaultRandomDelay returns a value within MIN_RANDOM_DELAY and MAX_RANDOM_DELAY`() {
        // Invoke the production random provider multiple times; all results must stay within
        // the documented [MIN_RANDOM_DELAY..MAX_RANDOM_DELAY] range.
        repeat(50) {
            val jitter = ReconnectBackoff.defaultRandomDelay()
            assertTrue(
                jitter in ReconnectBackoff.MIN_RANDOM_DELAY..ReconnectBackoff.MAX_RANDOM_DELAY,
                "defaultRandomDelay() produced $jitter, which is outside the expected range"
            )
        }
    }

    @Test
    fun `attempt 0 ignores previousDelayMillis entirely`() {
        val jitter = 1000L

        val afterLongDelay = ReconnectBackoff.nextDelayMillis(
            attempt = 0,
            previousDelayMillis = 100_000L,
            randomDelay = { jitter },
        )
        val afterNoDelay = ReconnectBackoff.nextDelayMillis(
            attempt = 0,
            previousDelayMillis = -1L,
            randomDelay = { jitter },
        )

        assertEquals(afterNoDelay, afterLongDelay, "attempt 0 must restart from INITIAL_DELAY")
    }

    @Test
    fun `attempt 0 adds the minimum jitter at the lower bound`() {
        val delay = ReconnectBackoff.nextDelayMillis(
            attempt = 0,
            previousDelayMillis = -1L,
            randomDelay = { ReconnectBackoff.MIN_RANDOM_DELAY },
        )

        assertEquals(2000L, delay, "INITIAL_DELAY 1000ms + MIN_RANDOM_DELAY 1000ms")
    }

    @Test
    fun `attempt 0 adds the maximum jitter at the upper bound`() {
        val delay = ReconnectBackoff.nextDelayMillis(
            attempt = 0,
            previousDelayMillis = -1L,
            randomDelay = { ReconnectBackoff.MAX_RANDOM_DELAY },
        )

        assertEquals(6000L, delay, "INITIAL_DELAY 1000ms + MAX_RANDOM_DELAY 5000ms")
    }

    @Test
    fun `a full retry sequence reaches MAX_BACKOFF and never exceeds it`() {
        // Growth compounds across attempts, so the cap has to hold for every step of a realistic
        // sequence — not just for a single call seeded with an already-large previous delay.
        //
        // Seeded at the *maximum* first-attempt delay on purpose. With MIN_RANDOM_DELAY the
        // sequence peaks at 292_298ms over the MAX_RECONNECT_ATTEMPTS - 1 steps below, never
        // reaches the 500_000ms cap, and the assertion would hold even with coerceAtMost deleted.
        var delay = ReconnectBackoff.INITIAL_DELAY + ReconnectBackoff.MAX_RANDOM_DELAY
        var cappedAtLeastOnce = false

        // MAX_RECONNECT_ATTEMPTS - 1 multiplications, matching ReconnectingListener: doReconnect()
        // reads `attempt = reconnectAttempts.get()` before incrementAndGet(), and
        // attemptReconnectWithBackoff() returns once the counter reaches MAX_RECONNECT_ATTEMPTS.
        // So attempt 0 is the seeded value above, followed by attempts 1..MAX-1.
        repeat(ReconnectBackoff.MAX_RECONNECT_ATTEMPTS - 1) { previousAttempts ->
            delay = ReconnectBackoff.nextDelayMillis(
                attempt = previousAttempts + 1,
                previousDelayMillis = delay,
                randomDelay = noJitter,
            )

            assertTrue(
                delay <= ReconnectBackoff.MAX_BACKOFF,
                "delay $delay exceeded MAX_BACKOFF at attempt ${previousAttempts + 1}"
            )
            if (delay == ReconnectBackoff.MAX_BACKOFF) cappedAtLeastOnce = true
        }

        assertTrue(
            cappedAtLeastOnce,
            "the sequence must actually reach MAX_BACKOFF, otherwise the cap is never exercised"
        )
    }
}
