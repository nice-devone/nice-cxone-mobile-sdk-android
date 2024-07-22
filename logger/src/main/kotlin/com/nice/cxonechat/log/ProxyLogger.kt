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

import java.util.Collections

/**
 * [Logger] implementation which calls all registered [Logger]s when this instance [log] method is called.
 *
 * @param initialLoggers Initial [Iterable] of loggers to be used by the [ProxyLogger].
 */
class ProxyLogger(
    initialLoggers: Iterable<Logger> = emptyList(),
) : Logger {

    private val loggerList = initialLoggers.toMutableList()

    /** Returns count of currently registered [Logger]s. */
    val loggerCount
        get() = synchronized(loggerList) { loggerList.size }

    /** Returns unmodifiable copy of registered [Logger]s. */
    val loggers: List<Logger>
        get() = synchronized(loggerList) { loggerList.toList() }

    /**
     * Creates an instance of [ProxyLogger] using supplied [loggers].
     *
     * @param loggers An array of loggers to be used by the [ProxyLogger].
     */
    constructor(
        vararg loggers: Logger,
    ) : this(loggers.asIterable())

    /**
     * Add a new logger.
     */
    fun add(logger: Logger) {
        require(logger !== this) { ERR_CANT_ADD_SELF }
        return synchronized(loggerList) {
            loggerList.add(logger)
        }
    }

    /** Add new loggers. */
    fun add(vararg loggers: Logger) {
        for (logger in loggers) {
            require(logger !== this) { ERR_CANT_ADD_SELF }
        }
        synchronized(loggerList) {
            Collections.addAll(loggerList, *loggers)
        }
    }

    /** Add new loggers. */
    fun addAll(loggers: Iterable<Logger>) {
        require(loggers.none { it === this }) { ERR_CANT_ADD_SELF }
        synchronized(loggerList) {
            loggerList.addAll(loggers)
        }
    }

    /** Remove given logger. */
    fun remove(logger: Logger) {
        synchronized(loggerList) {
            loggerList.remove(logger)
        }
    }

    /** Remove all loggers. */
    fun clear() {
        synchronized(loggerList, loggerList::clear)
    }

    override fun log(level: Level, message: String, throwable: Throwable?) {
        synchronized(loggerList) {
            for (logger in loggerList) {
                logger.log(level, message, throwable)
            }
        }
    }

    private companion object {
        private const val ERR_CANT_ADD_SELF = "Can't add self."
    }
}
