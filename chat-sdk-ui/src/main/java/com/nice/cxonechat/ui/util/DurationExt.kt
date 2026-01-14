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
import androidx.compose.ui.text.intl.PlatformLocale
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/** Converts [Duration] to timestamp string using provided [Locale]. **/
internal fun Duration.toTimeStamp(locale: Locale): String = toTimeStamp(locale.platformLocale)

/**
 * Converts [Duration] to timestamp string using provided [PlatformLocale].
 * Format is HH:mm:ss or mm:ss if hours are 0.
 * If duration is infinite, returns NaN.
 *
 * @return timestamp string in format HH:mm:ss or mm:ss
 */
internal fun Duration.toTimeStamp(platformLocale: PlatformLocale): String {
    if (this.isInfinite()) return String.format(platformLocale, "%f", Float.NaN)
    val hours = inWholeHours
    val minutes = (this - hours.toDuration(DurationUnit.HOURS)).inWholeMinutes
    val secs = (this - hours.toDuration(DurationUnit.HOURS) - minutes.toDuration(DurationUnit.MINUTES)).inWholeSeconds

    return if (hours > 0) {
        String.format(platformLocale, "%02d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format(platformLocale, "%02d:%02d", minutes, secs)
    }
}
