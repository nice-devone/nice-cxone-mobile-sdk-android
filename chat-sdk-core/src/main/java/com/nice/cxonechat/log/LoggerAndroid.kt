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
 * See [Documentation](https://developer.android.com/reference/android/util/Log#isLoggable(java.lang.String,%20int))
 * */
internal class LoggerAndroid(
    private val tag: String = "CXOneChat",
) : Logger {

    private val Level.priority
        get() = when {
            this >= Level.Severe -> Log.ERROR
            this >= Level.Warning -> Log.WARN
            this >= Level.Info -> Log.INFO
            this >= Level.Config -> Log.DEBUG
            else -> Log.VERBOSE
        }

    override fun log(level: Level, message: String, throwable: Throwable?) {
        val priority = level.priority

        for (part in message.chunked(4000))
            Log.println(priority, tag, part)

        if (throwable != null) {
            val trace = Log.getStackTraceString(throwable)
            Log.println(priority, tag, trace)
        }
    }

}
