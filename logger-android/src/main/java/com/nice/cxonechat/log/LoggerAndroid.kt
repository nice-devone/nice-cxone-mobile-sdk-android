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

package com.nice.cxonechat.log

import android.util.Log

/**
 * Android Logging mechanism. Uses platform logging [Log] to expose underlying
 * logs to the receiver (user).
 *
 * Message is split to ~4kB parts supported by the platform [Log]. The code logging
 * the messages is not synchronized, therefore the scenario where message is intercepted
 * by other is likely to happen.
 *
 * Additionally throwable, if provided, is retrieved via [Log.getStackTraceString]
 * and printed **after** the message.
 *
 * Logging is always performed unless platform refuses the logging operation via
 * [Log.isLoggable].
 *
 * See [Documentation](https://developer.android.com/reference/android/util/Log#isLoggable(kotlin.lang.String,%20int))
 *
 * @param tag The tag which will be supplied when logging via [Log] for each logged message.
 * */
class LoggerAndroid(
    private val tag: String,
) : Logger {

    private val Level.priority
        get() = when {
            this >= Level.Error -> Log.ERROR
            this >= Level.Warning -> Log.WARN
            this >= Level.Info -> Log.INFO
            this >= Level.Debug -> Log.DEBUG
            else -> Log.VERBOSE
        }

    override fun log(level: Level, message: String, throwable: Throwable?) {
        val priority = level.priority

        for (part in message.chunked(4000)) {
            Log.println(priority, tag, part)
        }

        if (throwable != null) {
            val trace = Log.getStackTraceString(throwable)
            Log.println(priority, tag, trace)
        }
    }
}
