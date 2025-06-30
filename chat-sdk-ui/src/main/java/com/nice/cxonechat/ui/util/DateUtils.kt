/*
 * Copyright (c) 2021-2025. NICE Ltd. All rights reserved.
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
import androidx.compose.runtime.Stable
import java.util.Date

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
