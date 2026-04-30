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
import android.icu.text.DateFormat
import android.text.format.DateFormat.getBestDateTimePattern
import androidx.compose.runtime.Stable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val HEADER_INPUT_FORMAT = ThreadLocal.withInitial {
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
}

@Stable
internal fun Context.toShortDateString(date: Date): String {
    val locale = resources.configuration.locales[0]
    val formatter = DateFormat.getDateTimeInstance(
        DateFormat.RELATIVE_SHORT,
        DateFormat.RELATIVE_SHORT,
        locale
    )
    return formatter.format(date).capitalizeFirstChar(locale)
}

@Stable
internal fun Context.toShortTimeString(date: Date): String {
    val locale = resources.configuration.locales[0]
    val formatter = DateFormat.getTimeInstance(
        DateFormat.RELATIVE_SHORT,
        locale
    )
    return formatter.format(date).capitalizeFirstChar(locale)
}

internal fun formatHeader(context: Context, dateKey: String): String {
    val locale = context.resources.configuration.locales[0]
    val input = requireNotNull(HEADER_INPUT_FORMAT.get())
    val parsedDate = input.parse(dateKey)
        ?: throw IllegalArgumentException(
            "Invalid dateKey '$dateKey'. Expected format: yyyy-MM-dd"
        )
    val pattern = getBestDateTimePattern(locale, "MMMMdy")
    return SimpleDateFormat(pattern, locale).format(parsedDate)
}

internal fun formatTime(context: Context, date: Date): String {
    val locale = context.resources.configuration.locales[0]
    // "j" skeleton lets the system choose 12h vs 24h based on locale and user preference
    val pattern = getBestDateTimePattern(locale, "jmm")
    return SimpleDateFormat(pattern, locale).format(date)
}

internal fun formatDuration(seconds: Long) = "${seconds / 60} min" // value will be always in minutes, so we can skip seconds part
