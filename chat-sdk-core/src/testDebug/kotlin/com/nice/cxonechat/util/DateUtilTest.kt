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

package com.nice.cxonechat.util

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

internal class DateUtilTest {

    // region toTimestamp

    @Test
    fun `toTimestamp formats epoch zero as ISO 8601 without fractional seconds`() {
        val date = Instant.fromEpochMilliseconds(0L)
        assertEquals("1970-01-01T00:00:00Z", date.toTimestamp())
    }

    @Test
    fun `toTimestamp preserves non-zero milliseconds as fractional seconds`() {
        val date = Instant.parse("2024-06-15T12:30:45.250Z")
        assertEquals("2024-06-15T12:30:45.250Z", date.toTimestamp())
    }

    @Test
    fun `toTimestamp truncates sub-millisecond precision`() {
        val millis = 1_718_450_000_123L
        val date = Instant.fromEpochMilliseconds(millis)
        assertEquals(millis, Instant.parse(date.toTimestamp()).toEpochMilliseconds())
    }

    // endregion

    // region toInstant

    @Test
    fun `toInstant parses standard uppercase-Z ISO 8601 string`() {
        val result = "2024-03-15T10:30:00Z".toInstant()
        assertEquals(Instant.parse("2024-03-15T10:30:00Z"), result)
    }

    @Test
    fun `toInstant parses legacy lowercase-z UTC suffix`() {
        val result = "2024-03-15T10:30:00z".toInstant()
        assertEquals(Instant.parse("2024-03-15T10:30:00Z"), result)
    }

    @Test
    fun `toInstant parses ISO 8601 string with milliseconds and uppercase-Z`() {
        val result = "2024-03-15T10:30:00.123Z".toInstant()
        assertEquals(Instant.parse("2024-03-15T10:30:00.123Z"), result)
    }

    @Test
    fun `toInstant parses ISO 8601 string with numeric UTC offset`() {
        val result = "2024-03-15T12:30:00+02:00".toInstant()
        assertEquals(Instant.parse("2024-03-15T10:30:00Z"), result)
    }

    @Test
    fun `toInstant throws on invalid input`() {
        assertFailsWith<IllegalArgumentException> {
            "not-a-date".toInstant()
        }
    }

    // endregion

    // region truncateToSeconds

    @Test
    fun `truncateToSeconds drops the sub-second component`() {
        val instant = Instant.parse("2026-07-01T12:34:56.789123456Z")

        val truncated = instant.truncateToSeconds()

        assertEquals(Instant.parse("2026-07-01T12:34:56Z"), truncated)
        assertEquals(0, truncated.nanosecondsOfSecond)
    }

    @Test
    fun `truncateToSeconds leaves a whole-second instant unchanged`() {
        val instant = Instant.parse("2026-07-01T12:34:56Z")

        assertEquals(instant, instant.truncateToSeconds())
    }

    @Test
    fun `truncateToSeconds floors a pre-epoch instant toward the past`() {
        val instant = Instant.parse("1969-12-31T23:59:59.500Z")

        val truncated = instant.truncateToSeconds()

        assertEquals(Instant.parse("1969-12-31T23:59:59Z"), truncated)
        assertEquals(0, truncated.nanosecondsOfSecond)
        assertTrue(truncated <= instant)
    }

    // endregion

    // region expiresWithin

    @Test
    fun `expiresWithin returns true for instant in the past`() {
        val past = Instant.parse("2000-01-01T00:00:00Z")
        assertTrue(past.expiresWithin(1.minutes))
    }

    @Test
    fun `expiresWithin returns true when instant is within the duration window`() {
        val soonExpiring = Instant.fromEpochMilliseconds(
            System.currentTimeMillis() + 5_000L // 5 seconds from now
        )
        assertTrue(soonExpiring.expiresWithin(10.seconds))
    }

    @Test
    fun `expiresWithin returns false when instant is beyond the duration window`() {
        val farFuture = Instant.fromEpochMilliseconds(
            System.currentTimeMillis() + 60_000L // 60 seconds from now
        )
        assertFalse(farFuture.expiresWithin(10.seconds))
    }

    @Test
    fun `expiresWithin returns false for DISTANT_FUTURE`() {
        assertFalse(Instant.DISTANT_FUTURE.expiresWithin(1.hours))
    }

    @Test
    fun `expiresWithin returns true when instant is just in the past for zero duration`() {
        val past = Instant.fromEpochMilliseconds(System.currentTimeMillis() - 1L)
        assertTrue(past.expiresWithin(0.milliseconds))
    }

    // endregion
}
