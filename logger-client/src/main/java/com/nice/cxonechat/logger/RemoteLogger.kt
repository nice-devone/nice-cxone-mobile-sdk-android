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

package com.nice.cxonechat.logger

import android.net.Uri
import com.nice.cxonechat.log.Level
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.error
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.MalformedURLException

/**
 * Remote logging mechanism that sends logs to a remote server.
 *
 * This logger uses OkHttp to send log messages to a specified URL. It supports different log levels
 * and includes additional details such as the file and line number of the log message.
 *
 * @param version The version of the application, used in the log messages.
 * @param okHttpClient The OkHttpClient instance used for making HTTP requests.
 */
class RemoteLogger(
    private val version: String,
    private val okHttpClient: OkHttpClient
) : Logger {

    private val Level.priority
        get() = when {
            this >= Level.Error -> LogLevel.ERROR
            this >= Level.Warning -> LogLevel.WARNING
            this >= Level.Info -> LogLevel.INFO
            else -> LogLevel.DEFAULT
        }

    private enum class LogLevel(val value: String) {
        INFO("0"),
        WARNING("1"),
        ERROR("2"),
        DEFAULT("3")
    }

    private var isEnabled = true

    private fun logError(
        level: Level,
        message: String,
        file: String?,
        line: Int?,
    ) {
        if (level < Level.Warning || !isEnabled) return // Ignore debug and verbose logs

        CoroutineScope(Dispatchers.IO).launch {
            post(level.priority, message, file, line)
        }
    }

    private fun post(
        level: LogLevel,
        message: String,
        file: String?,
        line: Int?,
    ) {
        val detailMap = LogDetail(
            file = file,
            line = line?.toString(),
            deviceFingerprint = deviceFingerprint
        )

        val logBody = LogBody(
            level = level.value,
            message = message,
            appVersion = version,
            detail = detailMap
        )
        val url = finalLoggerUrl
        if (url.isNullOrEmpty()) return

        val request = Request.Builder()
            .url(url)
            .post(Json.encodeToString(logBody).toRequestBody("application/json".toMediaType()))
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    // Disable logging to prevent cycle calling of the method
                    isEnabled = false
                    error("Failed to log message, received status code ${response.code} from $finalLoggerUrl")
                }
            }
        } catch (e: IOException) {
            // Disable logging to prevent cycle calling of the method
            isEnabled = false
            error("Failed to log message", e)
        }
    }

    override fun log(level: Level, message: String, throwable: Throwable?) {
        logError(
            level = level,
            message = message,
            file = throwable?.stackTrace?.get(2)?.fileName, // getting index 2 to skip the logger's own stack trace
            line = throwable?.stackTrace?.get(2)?.lineNumber
        )
    }

    /**
     * Set the logger url & other required data for logging.
     */
    companion object {
        private var finalLoggerUrl: String? = null
        private var deviceFingerprint: String? = null
        private const val PROGRAM = "android-dfo-chat"

        /**
         * Sets the data required for the logger to function.
         *
         * @param brandId The ID of the brand.
         * @param loggerUrl The URL for the logger. If empty, it will be evaluated from the chat URL.
         * @param chatUrl The URL for the chat service, used to derive the logger URL if needed.
         * @param deviceFingerprint An optional device fingerprint for additional context in logs.
         */
        fun setData(brandId: Long, loggerUrl: String, chatUrl: String, deviceFingerprint: String?) {
            val baseLoggerUrl = loggerUrl.ifEmpty { evaluateLoggerUrl(chatUrl) }
            finalLoggerUrl = if (!baseLoggerUrl.isNullOrEmpty()) {
                Uri.parse(baseLoggerUrl)
                    .buildUpon()
                    .appendQueryParameter("brandId", brandId.toString())
                    .appendQueryParameter("program", PROGRAM)
                    .build()
                    .toString()
            } else {
                null
            }
            this.deviceFingerprint = deviceFingerprint
        }

        private fun evaluateLoggerUrl(chatUrl: String): String? {
            val url = try {
                java.net.URI(chatUrl).toURL()
            } catch (e: MalformedURLException) {
                return null
            }

            val baseUrl = url.toString().substringBeforeLast("/chat/")
            val replaced = baseUrl.replace("channels", "app")
            val result = "$replaced/logger-public"

            val regex = Regex("^https://app[^/]+/logger-public$")
            return if (regex.matches(result)) {
                result
            } else {
                null
            }
        }
    }
}
