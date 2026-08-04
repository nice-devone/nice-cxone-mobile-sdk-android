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

import androidx.compose.ui.text.intl.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class DurationExtTest {

    private val locale = Locale("en-US")

    @Test
    fun toTimeStamp_basicFormats() {
        // Tests basic MM:SS and HH:MM:SS formats
        assertEquals("00:00", 0.toDuration(DurationUnit.SECONDS).toTimeStamp(locale))
        assertEquals("00:30", 30.toDuration(DurationUnit.SECONDS).toTimeStamp(locale))
        assertEquals("01:00", 60.toDuration(DurationUnit.SECONDS).toTimeStamp(locale))
        assertEquals("02:45", (2 * 60 + 45).toDuration(DurationUnit.SECONDS).toTimeStamp(locale))
    }

    @Test
    fun toTimeStamp_hoursFormats() {
        // Tests HH:MM:SS formats
        assertEquals("01:00:00", 3600.toDuration(DurationUnit.SECONDS).toTimeStamp(locale))
        assertEquals("01:23:45", (1 * 3600 + 23 * 60 + 45).toDuration(DurationUnit.SECONDS).toTimeStamp(locale))
        assertEquals("10:15:30", (10 * 3600 + 15 * 60 + 30).toDuration(DurationUnit.SECONDS).toTimeStamp(locale))
    }

    @Test
    fun toTimeStamp_forceLongFormat() {
        // Tests forceLongFormat parameter
        val minutesOnly = (5 * 60 + 30).toDuration(DurationUnit.SECONDS)
        val withHours = (1 * 3600 + 5 * 60 + 30).toDuration(DurationUnit.SECONDS)

        assertEquals("00:05:30", minutesOnly.toTimeStamp(locale, forceLongFormat = true))
        assertEquals("01:05:30", withHours.toTimeStamp(locale, forceLongFormat = true))
    }

    @Test
    fun toTimeStamp_infiniteDuration() {
        val duration = Duration.INFINITE
        val result = duration.toTimeStamp(locale)
        assertTrue(result.contains("NaN") || result.lowercase().contains("nan"))
    }

    @Test
    fun toTimeStamp_consistencyAndFormatRules() {
        // Tests format consistency and parsing rules
        val hours2min45sec30 = (2L * 3600 + 45 * 60 + 30).toDuration(DurationUnit.SECONDS)
        assertEquals("02:45:30", hours2min45sec30.toTimeStamp(locale))

        // Verify minutes-only format stays short
        val minutesOnly = (5 * 60 + 30).toDuration(DurationUnit.SECONDS)
        val result = minutesOnly.toTimeStamp(locale, forceLongFormat = false)
        assertEquals("05:30", result)

        // Verify hours always show HH:MM:SS
        val oneHour = (1 * 3600).toDuration(DurationUnit.SECONDS)
        assertEquals("01:00:00", oneHour.toTimeStamp(locale, forceLongFormat = false))
        assertTrue(oneHour.toTimeStamp(locale).contains(":"))
    }
}
