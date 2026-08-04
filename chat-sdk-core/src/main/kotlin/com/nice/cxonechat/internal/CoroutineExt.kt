/*
 * Copyright (c) 2021-2026. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.internal

import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.warning
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * Creates a child [LoggerScope] whose tag is `"$scope/$name"`, without executing any body.
 * Useful for passing a named sub-scope to [safeLaunch] or other logging contexts.
 */
internal fun LoggerScope.childScope(name: String): LoggerScope = LoggerScope("$scope/$name", identity)

/**
 * Launches a coroutine that catches and logs exceptions instead of propagating them
 * to the parent scope. Use for fire-and-forget operations where failure should be
 * logged but not crash the scope.
 *
 * [CancellationException] is always re-thrown to respect structured concurrency.
 * Only [Exception] subclasses are caught — [Error] subclasses (e.g. [OutOfMemoryError])
 * propagate normally so the JVM can handle them appropriately.
 * The [loggerScope] tag is included in any warning message, so callers should pass
 * a sub-scope (e.g. via [childScope]) that names the operation.
 *
 * @param loggerScope Scope used for recording failures.
 * @param block The suspend block to execute.
 * @return The launched [Job].
 */
internal fun CoroutineScope.safeLaunch(
    loggerScope: LoggerScope,
    block: suspend CoroutineScope.() -> Unit,
): Job = launch {
    try {
        block()
    } catch (expected: CancellationException) {
        throw expected
    } catch (logged: Exception) {
        loggerScope.warning("Fire-and-forget operation failed", logged)
    }
}

/**
 * Sends [value] to [channel] via [SendChannel.trySend]. If the send fails and the channel is still open,
 * logs a warning via [loggerScope] that the [tag] update was dropped due to a full buffer.
 * The [kotlinx.coroutines.channels.ChannelResult.isClosed] guard prevents spurious log spam on intentional shutdown.
 */
internal fun <T> trySendOrLog(channel: SendChannel<T>, value: T, loggerScope: LoggerScope, tag: String) {
    val result = channel.trySend(value)
    if (result.isFailure && !result.isClosed) loggerScope.warning("Dropped $tag — channel buffer full")
}

/**
 * Maps the failure of a [Result] by applying [transform] to the contained exception.
 * [CancellationException] is never transformed — it always passes through unchanged
 * to preserve structured concurrency semantics.
 * Success values are returned as-is.
 */
internal inline fun <T> Result<T>.mapFailure(transform: (Throwable) -> Throwable): Result<T> =
    exceptionOrNull()?.let { ex ->
        if (ex is CancellationException) this else Result.failure(transform(ex))
    } ?: this

/**
 * Collects this [Flow] while guarding each emission against unexpected exceptions. Any
 * [CancellationException] is rethrown to preserve structured concurrency; other
 * [Exception] subclasses are logged via [loggerScope] and collection continues with the
 * next value. [Error] subclasses propagate normally.
 *
 * Use for long-lived listener collectors where an occasional throw in the collector
 * body (e.g. a misbehaving client-supplied callback) must not permanently terminate
 * the subscription and silently drop subsequent events.
 *
 * @param T the element type emitted by the [Flow].
 * @param loggerScope Scope used for recording failures.
 * @param action The suspend action to invoke per emitted value.
 */
internal suspend fun <T> Flow<T>.safeCollect(
    loggerScope: LoggerScope,
    action: suspend (T) -> Unit,
): Unit = collect { value ->
    try {
        action(value)
    } catch (expected: CancellationException) {
        throw expected
    } catch (logged: Exception) {
        loggerScope.warning("Collector body failed", logged)
    }
}
