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

package com.nice.cxonechat.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Instant

/**
 * Unit tests for [toDateKey] — the [Instant]-to-"yyyy-MM-dd" key used to group conversation
 * messages by day.
 */
class DateExtTest {

    @Test
    fun toDateKey_knownEpoch_returnsCorrectDate() {
        // 2017-05-26T08:27:55Z  =  1495787275000 ms since epoch
        val instant = Instant.parse("2017-05-26T08:27:55Z")
        val result = instant.toDateKey()
        // Compute the expected date using the same mechanism as the implementation so the
        // assertion remains correct regardless of the JVM default timezone.
        val expected = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(Date(instant.toEpochMilliseconds()))
        assertEquals(expected, result)
    }

    @Test
    fun toDateKey_formatIsYyyyMmDd() {
        // 2020-01-09 — a date with a single-digit month and day (zero-padded expected)
        val instant = Instant.parse("2020-01-09T00:00:00Z")
        val result = instant.toDateKey()
        // Should match yyyy-MM-dd pattern
        assertTrue(
            "Date key '$result' does not match yyyy-MM-dd",
            result.matches(Regex("""\d{4}-\d{2}-\d{2}""")),
        )
    }

    @Test
    fun toDateKey_sameDayTwoInstants_returnSameKey() {
        // Two instants only 1 second apart — guaranteed to fall on the same local date in any
        // timezone, so the test can directly assert key equality.
        val first = Instant.parse("2024-06-15T12:00:00Z")
        val second = Instant.parse("2024-06-15T12:00:01Z")
        val firstKey = first.toDateKey()
        val secondKey = second.toDateKey()
        assertEquals(firstKey, secondKey)
    }

    @Test
    fun toDateKey_differentDays_returnDifferentKeys() {
        val day1 = Instant.parse("2023-03-01T12:00:00Z")
        val day2 = Instant.parse("2023-03-02T12:00:00Z")
        // Keys for different UTC days must differ (tested mid-day to avoid edge-of-day TZ issues).
        val key1 = day1.toDateKey()
        val key2 = day2.toDateKey()
        assertTrue(
            "Expected keys for different days to differ but both were '$key1'",
            key1 != key2,
        )
    }

    @Test
    fun toDateKey_epochZero_returnsWellFormedKey() {
        val epoch = Instant.fromEpochMilliseconds(0L)
        val result = epoch.toDateKey()
        assertTrue(
            "Date key '$result' is not well-formed yyyy-MM-dd",
            result.matches(Regex("""\d{4}-\d{2}-\d{2}""")),
        )
    }
}
