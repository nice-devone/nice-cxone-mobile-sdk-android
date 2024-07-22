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

/**
 * Wrapper for [Logger] which prepends each logged message with the [scope].
 */
interface LoggerScope : Logger {
    /**  Description of the scope, usually a class name or a custom name. */
    val scope: String

    /** The wrapped [Logger]. */
    val identity: Logger

    companion object {

        /**
         * Creates an instance of [LoggerScope] with custom [name].
         *
         * @param name The name of the scope.
         * @param identity The wrapped [Logger].
         */
        operator fun invoke(name: String, identity: Logger): LoggerScope = NamedScope(scope = name, identity = identity)

        /**
         * Creates an instance of [LoggerScope] with [scope] set to class simple name.
         *
         * @param T The type which will be used for naming of the [LoggerScope].
         * @param identity The wrapped [Logger].
         */
        inline operator fun <reified T> invoke(identity: Logger) =
            LoggerScope(T::class.java.simpleName, identity)
    }
}

/**
 * The default implementation of [LoggerScope] if custom naming of the scope is needed.
 *
 * @param scope The custom name of the scope.
 * @param identity The wrapped [Logger].
 */
private class NamedScope(
    override val scope: String,
    override val identity: Logger,
) : LoggerScope {

    override fun log(level: Level, message: String, throwable: Throwable?) {
        identity.log(level, "[$scope] $message", throwable)
    }
}

/**
 * Creates temporary sub-scope of current [LoggerScope] with custom [name] post-fixed to the new [scope].
 * The sub-scope is discarded once the [body] invocation is finished.
 *
 * @param T The result type of the scoped function.
 * @param name Name of custom sub-scope.
 * @param body Function which defines the sub-scope.
 * @return The result of the [body] invocation.
 */
inline fun <T> LoggerScope.scope(name: String, body: LoggerScope.() -> T): T =
    LoggerScope("$scope/$name", identity).body()

/**
 * Measures the duration of a scope.
 *
 * Shorthand for scope(name) { duration { body }}.
 *
 * @param T The result type of the scoped function.
 * @param name Name of custom sub-scope.
 * @param body Function which defines the sub-scope.
 * @return The result of the [body] invocation.
 */
inline fun <T> LoggerScope.timedScope(name: String, body: () -> T): T = scope(name) {
    duration(body)
}
