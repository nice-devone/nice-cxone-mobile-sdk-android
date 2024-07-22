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

package com.nice.cxonechat.util

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.time.Duration

private val timestampFormat: SimpleDateFormat
    get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'z'", Locale.ROOT).also {
        it.timeZone = TimeZone.getTimeZone("UTC")
    }
private val timestampFormatWithoutMillis: SimpleDateFormat
    get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZ", Locale.ROOT).also {
        it.timeZone = TimeZone.getTimeZone("UTC")
    }

@Throws(ParseException::class)
internal fun String.timestampToDate(): Date {
    return try {
        timestampFormat.parse(this)
    } catch (ignore: ParseException) {
        timestampFormatWithoutMillis.parse(this)
    }.let(::requireNotNull)
}

internal fun Date.toTimestamp(): String = timestampFormat.format(this)

internal fun DateTime.toTimestamp(): String = timestampFormatWithoutMillis.format(date)

internal operator fun Date.plus(millis: Long) = Date(time + millis)

internal fun Date.expiresWithin(duration: Duration): Boolean {
    val now = Date() + duration.inWholeMilliseconds
    return now.after(this)
}
