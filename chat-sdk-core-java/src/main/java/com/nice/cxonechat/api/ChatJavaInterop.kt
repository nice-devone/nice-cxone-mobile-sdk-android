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

@file:JvmName("ChatJavaInterop")

package com.nice.cxonechat.api

import androidx.annotation.VisibleForTesting
import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.Chat
import com.nice.cxonechat.ChatStateEvent
import com.nice.cxonechat.Public
import com.nice.cxonechat.exceptions.CXoneException
import com.nice.cxonechat.exceptions.InternalError
import kotlinx.coroutines.CancellationException
import java.util.concurrent.Executor
import java.util.function.Consumer

/**
 * Java-friendly asynchronous variant of [Chat.connect].
 *
 * Internally launches a background coroutine and notifies the supplied callbacks. Returns a
 * [Cancellable] that cancels the underlying work.
 *
 * **Note about error channels**: [Chat.connect] does **not** report connection failures
 * (network unreachable, TLS/auth errors, …) through this `onError` callback — they are surfaced
 * via [Chat.stateFlow] and cancel the launched coroutine with
 * [kotlinx.coroutines.CancellationException]. Use [observeState] for connection-failure
 * diagnostics. The defensive [onError] catch in this function only fires if [Chat.connect]
 * itself throws synchronously — either a [com.nice.cxonechat.exceptions.CXoneException] (auth,
 * bad config) or an unexpected non-cancellation exception. When [onError] is null, both
 * branches are silently swallowed.
 *
 * **Note about exceptions thrown by [onSuccess]**: a throw from inside [onSuccess] is a consumer
 * bug, not an SDK error — it is **not** routed through [onError] (which would mislabel it as an
 * SDK failure). Such throws propagate out of the launched coroutine and are handled by
 * [kotlinx.coroutines][kotlinx.coroutines.CoroutineExceptionHandler]'s default uncaught-exception
 * machinery, which delegates to the current thread's [Thread.UncaughtExceptionHandler]. On
 * Android this is `RuntimeInit.KillApplicationHandler` by default, **which terminates the
 * process** — wrap [onSuccess] defensively or install a custom
 * [Thread.UncaughtExceptionHandler] if you need a softer landing.
 *
 * @param chat The [Chat] instance to connect.
 * @param onSuccess Invoked when the connection completes successfully. May be null.
 * @param onError Invoked when an error occurs. May be null.
 * @param callbackExecutor If non-null, the launched coroutine uses this [Executor] as its
 *                         [kotlinx.coroutines.CoroutineDispatcher], so [onSuccess] and [onError]
 *                         resume on the executor's thread (e.g. a main-thread executor for
 *                         Android UI updates). When null, callbacks fire on the SDK's internal
 *                         IO thread.
 * @return A [Cancellable] that cancels the launched coroutine.
 */
@Public
@JvmOverloads
fun connectAsync(
    chat: Chat,
    onSuccess: Runnable? = null,
    onError: ErrorCallback? = null,
    callbackExecutor: Executor? = null,
): Cancellable = launchCancellable(callbackExecutor) { connectChat(chat, onError, onSuccess) }

@VisibleForTesting
@JvmSynthetic
internal suspend fun connectChat(
    chat: Chat,
    onError: ErrorCallback? = null,
    onSuccess: Runnable? = null,
) {
    try {
        chat.connect()
    } catch (e: CancellationException) {
        throw e
    } catch (e: CXoneException) {
        onError?.onError(e)
        return
    } catch (expected: Exception) {
        onError?.onError(InternalError("Unexpected error connecting Chat", expected))
        return
    }
    // Outside the SDK try/catch: consumer bugs in onSuccess do NOT get reported as SDK errors.
    onSuccess?.run()
}

/**
 * Java-friendly asynchronous variant of [Chat.getChannelAvailability].
 *
 * Internally launches a background coroutine and notifies the supplied callbacks. Returns a
 * [Cancellable] that cancels the underlying work.
 *
 * See [connectAsync] for notes on null `onError` and exceptions thrown by `onSuccess`.
 *
 * @param chat The [Chat] instance to query.
 * @param onSuccess Invoked with the availability boolean.
 * @param onError Invoked when an error occurs. May be null.
 * @param callbackExecutor If non-null, used as the launched coroutine's dispatcher so
 *                         callbacks run on the executor's thread. When null, callbacks fire on
 *                         the SDK's internal IO thread.
 * @return A [Cancellable] that cancels the launched coroutine.
 */
@Public
@JvmOverloads
fun getChannelAvailabilityAsync(
    chat: Chat,
    onSuccess: Consumer<Boolean>,
    onError: ErrorCallback? = null,
    callbackExecutor: Executor? = null,
): Cancellable = launchCancellable(callbackExecutor) { fetchChannelAvailability(chat, onError, onSuccess) }

@VisibleForTesting
@JvmSynthetic
internal suspend fun fetchChannelAvailability(
    chat: Chat,
    onError: ErrorCallback? = null,
    onSuccess: Consumer<Boolean>,
) {
    val result: Boolean = try {
        chat.getChannelAvailability()
    } catch (e: CancellationException) {
        throw e
    } catch (e: CXoneException) {
        onError?.onError(e)
        return
    } catch (expected: Exception) {
        onError?.onError(InternalError("Unexpected error querying channel availability", expected))
        return
    }
    onSuccess.accept(result)
}

/**
 * Java-friendly observer for [Chat.stateFlow].
 *
 * Internally launches a background coroutine that collects the state flow and invokes
 * [listener] for each emission. **Each emission is delivered inside an isolated try/catch** —
 * a throw from [listener] aborts only that single emission, not the collector itself, so the
 * observer keeps receiving subsequent events.
 *
 * **Upstream errors** (an exception thrown by the underlying flow itself, as opposed to by
 * [listener]) terminate the collector. They are routed to [onError]:
 * [com.nice.cxonechat.exceptions.CXoneException] subclasses pass through unchanged, anything
 * else is wrapped in [InternalError]. When [onError] is null, the exception escapes to the
 * thread's [Thread.UncaughtExceptionHandler] (process-killing on stock Android).
 *
 * **You MUST invoke the returned [Cancellable] when finished.** [Chat.stateFlow] is a hot
 * [SharedFlow][kotlinx.coroutines.flow.SharedFlow] that never completes on its own; failing to
 * cancel leaks the listener (including any captured references such as an `Activity`/`View`),
 * the internal coroutine scope, and the upstream flow subscription for the process lifetime.
 *
 * @param chat The [Chat] instance whose state to observe.
 * @param listener Invoked for each [ChatStateEvent] emission.
 * @param onError Invoked once with an upstream-flow error before the collector terminates. May be null.
 * @param callbackExecutor If non-null, used as the collector coroutine's dispatcher so
 *                         [listener] runs on the executor's thread. When null, the listener
 *                         fires on the SDK's internal IO thread.
 * @return A [Cancellable] that cancels the collector. Must be invoked to release resources.
 */
@Public
@JvmOverloads
fun observeState(
    chat: Chat,
    listener: Consumer<ChatStateEvent>,
    onError: ErrorCallback? = null,
    callbackExecutor: Executor? = null,
): Cancellable = launchCancellable(callbackExecutor) { collectStateFlow(chat, listener, onError) }

@VisibleForTesting
@JvmSynthetic
internal suspend fun collectStateFlow(
    chat: Chat,
    listener: Consumer<ChatStateEvent>,
    onError: ErrorCallback? = null,
) {
    collectWithErrorCallback(chat.stateFlow, listener, onError, "Unexpected error in stateFlow observer")
}
