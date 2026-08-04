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

import android.content.Context
import android.content.res.Resources
import android.icu.text.DateFormat
import android.text.format.DateFormat.getBestDateTimePattern
import androidx.compose.runtime.Stable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Instant

private val HEADER_INPUT_FORMAT = ThreadLocal.withInitial {
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
}

/**
 * Returns the first locale from the resource configuration, falling back to [Locale.getDefault]
 * when the locale list is empty (mirrors [currentLocale] logic).
 */
private val Resources.currentLocale: Locale
    get() = if (configuration.locales.isEmpty) Locale.getDefault() else configuration.locales[0]

@Stable
internal fun Resources.toShortDateTimeString(instant: Instant): String {
    val locale = currentLocale
    val formatter = DateFormat.getDateTimeInstance(
        DateFormat.RELATIVE_SHORT,
        DateFormat.RELATIVE_SHORT,
        locale
    )
    return formatter.format(Date(instant.toEpochMilliseconds())).capitalizeFirstChar(locale)
}

@Stable
internal fun Resources.toShortTimeString(instant: Instant): String {
    val locale = currentLocale
    val formatter = DateFormat.getTimeInstance(
        DateFormat.RELATIVE_SHORT,
        locale
    )
    return formatter.format(Date(instant.toEpochMilliseconds())).capitalizeFirstChar(locale)
}

internal fun formatHeader(context: Context, dateKey: String): String {
    val locale = context.resources.currentLocale
    val input = requireNotNull(HEADER_INPUT_FORMAT.get())
    val parsedDate = input.parse(dateKey)
        ?: throw IllegalArgumentException(
            "Invalid dateKey '$dateKey'. Expected format: yyyy-MM-dd"
        )
    val pattern = getBestDateTimePattern(locale, "MMMMdy")
    return SimpleDateFormat(pattern, locale).format(parsedDate)
}

internal fun formatTime(context: Context, instant: Instant): String {
    val locale = context.resources.currentLocale
    // "j" skeleton lets the system choose 12h vs 24h based on locale and user preference
    val pattern = getBestDateTimePattern(locale, "jmm")
    return SimpleDateFormat(pattern, locale).format(Date(instant.toEpochMilliseconds()))
}

internal fun formatDuration(seconds: Long) = "${seconds / 60} min" // value will be always in minutes, so we can skip seconds part
