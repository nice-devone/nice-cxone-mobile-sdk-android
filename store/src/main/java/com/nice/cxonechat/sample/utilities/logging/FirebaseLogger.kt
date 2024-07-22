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

package com.nice.cxonechat.sample.utilities.logging

import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.nice.cxonechat.log.Level
import com.nice.cxonechat.log.Level.All
import com.nice.cxonechat.log.Level.Custom
import com.nice.cxonechat.log.Level.Debug
import com.nice.cxonechat.log.Level.Error
import com.nice.cxonechat.log.Level.Info
import com.nice.cxonechat.log.Level.Verbose
import com.nice.cxonechat.log.Level.Warning
import com.nice.cxonechat.log.Logger

/**
 * [Logger] implementation which redirects logged messages to Firebase [crashlytics].
 *
 * @param minLevel Minimal [Level] of message to be logged to Firebase [crashlytics] for additional context when
 * exception is recorded. The default value is [Level.Info].
 * @param minExceptionLevel Minimal [Level] of logged exception in order for it to be recorded to Firebase [crashlytics]
 * as non-fatal exception. The default value is [Level.Error].
 */
class FirebaseLogger(
    private val minLevel: Level = Info,
    private val minExceptionLevel: Level = Error,
) : Logger {

    private fun Level.toChar(): Char = when (this) {
        All -> 'A'
        Verbose -> 'V'
        Debug -> 'D'
        Info -> 'I'
        Warning -> 'W'
        Error -> 'E'
        is Custom -> 'C'
    }

    override fun log(level: Level, message: String, throwable: Throwable?) {
        if (level < minLevel) return
        with(Firebase.crashlytics) {
            log("${level.toChar()}:$message")
            if (throwable != null && level >= minExceptionLevel) recordException(throwable)
        }
    }
}
