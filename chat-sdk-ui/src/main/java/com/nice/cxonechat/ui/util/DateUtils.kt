/*
 * Copyright (c) 2021-2024. NICE Ltd. All rights reserved.
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
import android.text.format.DateFormat
import androidx.compose.runtime.Stable
import java.util.Calendar
import java.util.Date

internal fun Date.isSameDay(date: Date): Boolean {
    val cal1 = Calendar.getInstance().also { it.time = this }
    val cal2 = Calendar.getInstance().apply { time = date }
    return cal1.isSameDay(cal2)
}

internal fun Calendar.isSameDay(cal2: Calendar): Boolean = this[Calendar.ERA] == cal2[Calendar.ERA] &&
        this[Calendar.YEAR] == cal2[Calendar.YEAR] &&
        this[Calendar.DAY_OF_YEAR] == cal2[Calendar.DAY_OF_YEAR]

@Stable
internal fun Context.toShortDateString(date: Date): String = DateFormat.getDateFormat(this).format(date)
