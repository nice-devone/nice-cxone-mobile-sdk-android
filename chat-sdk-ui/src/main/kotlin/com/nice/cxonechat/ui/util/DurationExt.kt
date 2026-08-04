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

import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.intl.PlatformLocale
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/** Converts [Duration] to timestamp string using provided [Locale]. **/
@Stable
internal fun Duration.toTimeStamp(locale: Locale, forceLongFormat: Boolean = false): String =
    toTimeStamp(locale.platformLocale, forceLongFormat)

/**
 * Converts [Duration] to timestamp string using provided [PlatformLocale].
 * Format is HH:mm:ss or mm:ss if hours are 0.
 * If duration is infinite, returns NaN.
 *
 * @return timestamp string in format HH:mm:ss or mm:ss
 */
@Stable
internal fun Duration.toTimeStamp(platformLocale: PlatformLocale, forceLongFormat: Boolean = false): String {
    if (this.isInfinite()) return String.format(platformLocale, "%f", Float.NaN)
    val hours = inWholeHours
    val minutes = (this - hours.toDuration(DurationUnit.HOURS)).inWholeMinutes
    val secs = (this - hours.toDuration(DurationUnit.HOURS) - minutes.toDuration(DurationUnit.MINUTES)).inWholeSeconds

    return if (hours > 0 || forceLongFormat) {
        String.format(platformLocale, "%02d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format(platformLocale, "%02d:%02d", minutes, secs)
    }
}

/**
 * Converts [Duration] to a localized accessibility-friendly spoken format using ICU MeasureFormat.
 * Example: 2 min 30 sec → "2 minutes, 30 seconds" (locale-aware)
 *
 * @param locale the locale to use for formatting the accessibility string
 * @param infinityTextProvider provides the string for infinite durations
 * @return localized spoken time format for TalkBack announcements
 */
@Suppress("SpreadOperator")
internal fun Duration.toAccessibilityTimeFormat(
    locale: java.util.Locale = java.util.Locale.getDefault(),
    infinityTextProvider: () -> String,
): String {
    if (this.isInfinite()) return infinityTextProvider()

    val hours = inWholeHours
    val minutes = (this - hours.toDuration(DurationUnit.HOURS)).inWholeMinutes
    val secs = (this - hours.toDuration(DurationUnit.HOURS) - minutes.toDuration(DurationUnit.MINUTES)).inWholeSeconds

    val measures = mutableListOf<Measure>()
    if (hours > 0) measures.add(Measure(hours, MeasureUnit.HOUR))
    if (minutes > 0) measures.add(Measure(minutes, MeasureUnit.MINUTE))
    if (secs > 0 || measures.isEmpty()) measures.add(Measure(secs, MeasureUnit.SECOND))

    val formatter = MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.WIDE)
    return formatter.formatMeasures(*measures.toTypedArray())
}

@Composable
internal fun rememberAccessibilityTime(
    duration: Duration,
    infinityTextRes: Int,
): String {
    val resources = LocalResources.current
    val locale = LocalLocale.current.platformLocale
    return remember(duration, locale, infinityTextRes) {
        duration.toAccessibilityTimeFormat(locale) {
            resources.getString(infinityTextRes)
        }
    }
}
