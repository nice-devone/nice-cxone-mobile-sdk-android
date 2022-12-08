package com.nice.cxonechat.log

import kotlin.time.Duration
import kotlin.time.DurationUnit.MILLISECONDS

internal interface LoggerScope : Logger {

    val scope: String
    val identity: Logger

    companion object {

        operator fun invoke(name: String, identity: Logger): LoggerScope {
            return NamedScope(scope = name, identity = identity)
        }

        inline operator fun <reified T> invoke(identity: Logger) =
            LoggerScope(T::class.java.simpleName, identity)

    }

}

private class NamedScope(
    override val scope: String,
    override val identity: Logger,
) : LoggerScope {

    override fun log(level: Level, message: String, throwable: Throwable?) {
        identity.log(level, "[$scope] $message", throwable)
    }

}

internal infix fun String.took(duration: Duration) =
    "$this took ${duration.toDouble(MILLISECONDS)}ms"

internal inline fun <T> LoggerScope.scope(name: String, body: LoggerScope.() -> T): T =
    body(LoggerScope("$scope/$name", identity))
