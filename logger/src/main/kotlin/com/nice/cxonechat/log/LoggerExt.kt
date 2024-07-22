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

import com.nice.cxonechat.log.Level.Debug
import com.nice.cxonechat.log.Level.Error
import com.nice.cxonechat.log.Level.Info
import com.nice.cxonechat.log.Level.Verbose
import com.nice.cxonechat.log.Level.Warning
import kotlin.time.DurationUnit.MILLISECONDS
import kotlin.time.measureTimedValue

/**
 * Calls [Logger.log] message with [Verbose] level.
 *
 * @param message Message to log.
 * @param throwable Optional [Throwable] to log, default `null`.
 */
fun Logger.verbose(message: String, throwable: Throwable? = null) {
    log(Verbose, message, throwable)
}

/**
 * Calls [Logger.log] message with [Debug] level.
 *
 * @param message Message to log.
 * @param throwable Optional [Throwable] to log, default `null`.
 */
fun Logger.debug(message: String, throwable: Throwable? = null) {
    log(Debug, message, throwable)
}

/**
 *  Calls [Logger.log] message with [Info] level.
 *
 * @param message Message to log.
 * @param throwable Optional [Throwable] to log, default `null`.
 */
fun Logger.info(message: String, throwable: Throwable? = null) {
    log(Info, message, throwable)
}

/**
 * Calls [Logger.log] message with [Warning] level.
 *
 * @param message Message to log.
 * @param throwable Optional [Throwable] to log, default `null`.
 */
fun Logger.warning(message: String, throwable: Throwable? = null) {
    log(Warning, message, throwable)
}

/**
 * Calls [Logger.log] message with [Error] level.
 *
 * @param message Message to log.
 * @param throwable Optional [Throwable] to log, default `null`.
 */
fun Logger.error(message: String, throwable: Throwable? = null) {
    log(Error, message, throwable)
}

/**
 * Measures the duration it takes to invoke the [body].
 * Logs with [Verbose] level message `Started` before the invocation and `Finished took XYZms` after completion.
 *
 * @param T Result type of [body] execution.
 * @param body Function which should be measured.
 * @return Result of [body] execution.
 */
inline fun <T> Logger.duration(body: () -> T): T {
    verbose("Started")
    val (value, duration) = measureTimedValue(body)
    verbose("Finished took ${duration.toDouble(MILLISECONDS)}ms")
    return value
}
