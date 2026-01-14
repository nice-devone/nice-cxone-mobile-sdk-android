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

package com.nice.cxonechat.sample.utilities

import android.content.Context
import android.icu.text.DateFormat
import com.nice.cxonechat.log.Level
import com.nice.cxonechat.log.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import java.io.File
import java.util.Date

/**
 * FileLogger is a utility class for logging messages to a file in the Android file system.
 * This implements the [Logger] interface and writes log messages to a specified file.
 * It appends log entries with timestamps and log levels.
 * There is no checking for file size or log rotation; it simply appends to the file.
 *
 * @param context The Android Context used to access the external files directory, application context should be used to avoid memory leaks.
 * @param fileName The name of the log file. Defaults to `nice_cxonechat_app_log.txt`.
 */
internal class FileLogger(context: Context, private val fileName: String = DEFAULT_LOG_NAME) : Logger {
    internal val logFile = File(context.getExternalFilesDir(null), fileName)

    init {
        log(Level.All, "------------------------ Start ------------------------")
    }

    override fun log(level: Level, message: String, throwable: Throwable?) {
        Dispatchers.IO.asExecutor().execute {
            logFile.appendText("${now()} : [${level.name()}] $message\n${throwable?.stackTraceToString()?.let { it + "\n" } ?: ""}")
        }
    }

    private fun Level.name() = when (this) {
        Level.Error -> "E"
        Level.Warning -> "W"
        Level.Info -> "I"
        Level.Debug -> "D"
        Level.Verbose -> "V"
        Level.All -> "A"
        is Level.Custom -> "C($intValue)"
    }

    private fun now(): String? = DateFormat.getPatternInstance(DATE_FORMAT).format(Date())

    internal companion object {
        const val DEFAULT_LOG_NAME = "nice_cxonechat_app.log"
        private const val DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS"

        private var instance: FileLogger? = null

        fun getInstance(context: Context, fileName: String = DEFAULT_LOG_NAME): FileLogger = synchronized(this) {
            instance ?: FileLogger(context.applicationContext, fileName).also { instance = it }
        }
    }
}
