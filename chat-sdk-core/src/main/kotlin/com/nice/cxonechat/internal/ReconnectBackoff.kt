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

/**
 * Exponential backoff formula shared by every reconnect mechanism in the SDK.
 *
 * This is a backend requirement, not politeness: a client that retries too frequently within a
 * short window gets blocked by the backend until a block timeout expires. Any reconnect loop
 * (socket-level in [ReconnectingListener], or pre-socket REST-phase in [PreSocketReconnectScheduler])
 * must compute its delay via [nextDelayMillis] rather than inventing its own timing.
 */
internal object ReconnectBackoff {
    /** Maximum number of reconnection attempts allowed before giving up. */
    const val MAX_RECONNECT_ATTEMPTS = 20

    /** Initial delay for the first reconnection attempt (in milliseconds). */
    const val INITIAL_DELAY = 1000L

    /** Minimum random delay for the first reconnection attempt (in milliseconds). */
    const val MIN_RANDOM_DELAY = 1000L

    /** Maximum random delay for the first reconnection attempt (in milliseconds). */
    const val MAX_RANDOM_DELAY = 5000L

    /** Maximum delay for exponential backoff (in milliseconds). */
    const val MAX_BACKOFF = 500_000L

    private const val BACKOFF_MULTIPLIER = 1.3

    /** Default random delay source: a uniform value in [MIN_RANDOM_DELAY]..[MAX_RANDOM_DELAY]. */
    fun defaultRandomDelay(): Long = (MIN_RANDOM_DELAY..MAX_RANDOM_DELAY).random()

    /**
     * Computes the delay for the next reconnection attempt.
     * The first attempt (attempt == 0) waits [INITIAL_DELAY] plus a random jitter from [randomDelay].
     * Subsequent attempts multiply the previous delay by 1.3, capped at [MAX_BACKOFF].
     *
     * @param attempt Zero-based count of attempts already made.
     * @param previousDelayMillis Delay used for the previous attempt; ignored when [attempt] is 0.
     * @param randomDelay Jitter source for the first attempt; overridable for deterministic tests.
     */
    fun nextDelayMillis(
        attempt: Int,
        previousDelayMillis: Long,
        randomDelay: () -> Long = ::defaultRandomDelay,
    ): Long = if (attempt == 0) {
        INITIAL_DELAY + randomDelay()
    } else {
        (previousDelayMillis * BACKOFF_MULTIPLIER).toLong().coerceAtMost(MAX_BACKOFF)
    }
}
