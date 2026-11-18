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
import org.junit.Test

/**
 * Unit tests for [formatDuration].
 *
 * `formatDuration` converts a duration in **seconds** to a human-readable string of the form
 * `"N min"`, truncating any remainder below a full minute.  It is used in the TIME_PICKER
 * bottom sheet to display slot durations.
 */
class DateUtilsFormatDurationTest {

    @Test
    fun formatDuration_zeroSeconds_returnsZeroMin() {
        assertEquals("0 min", formatDuration(0L))
    }

    @Test
    fun formatDuration_exactlyOneMinute_returnsOneMin() {
        assertEquals("1 min", formatDuration(60L))
    }

    @Test
    fun formatDuration_lessThanOneMinute_returnsZeroMin() {
        // 59 s < 60 s — the truncating integer division should yield 0.
        assertEquals("0 min", formatDuration(59L))
    }

    @Test
    fun formatDuration_oneMinuteAndFraction_isTruncated() {
        // 90 s = 1 min 30 s — the extra 30 s should be discarded.
        assertEquals("1 min", formatDuration(90L))
    }

    @Test
    fun formatDuration_oneHour_returnsSixtyMin() {
        assertEquals("60 min", formatDuration(3600L))
    }

    @Test
    fun formatDuration_multipleExactMinutes_returnsCorrectCount() {
        assertEquals("45 min", formatDuration(2700L)) // 45 * 60
        assertEquals("10 min", formatDuration(600L)) // 10 * 60
    }
}
