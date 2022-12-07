package com.nice.cxonechat.log

import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

internal fun Logger.finest(message: String, throwable: Throwable? = null) {
    log(Level.Finest, message, throwable)
}

internal fun Logger.finer(message: String, throwable: Throwable? = null) {
    log(Level.Finer, message, throwable)
}

internal fun Logger.fine(message: String, throwable: Throwable? = null) {
    log(Level.Fine, message, throwable)
}

internal fun Logger.info(message: String, throwable: Throwable? = null) {
    log(Level.Info, message, throwable)
}

internal fun Logger.warning(message: String, throwable: Throwable? = null) {
    log(Level.Warning, message, throwable)
}

internal fun Logger.severe(message: String, throwable: Throwable? = null) {
    log(Level.Severe, message, throwable)
}

@OptIn(ExperimentalTime::class)
internal inline fun <T> Logger.duration(body: () -> T): T {
    finest("Started")
    val (value, duration) = measureTimedValue(body)
    finest("Finished" took duration)
    return value
}
