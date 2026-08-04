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

@file:JvmSynthetic

package com.nice.cxonechat.api

import androidx.annotation.CheckResult
import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.exceptions.CXoneException
import com.nice.cxonechat.exceptions.InternalError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import java.util.function.Consumer

/**
 * Message used for the [InternalError] surfaced when a `*CoroutineWrapper` async method is called
 * after [java.lang.AutoCloseable.close]. Shared across wrappers so all three classes report consistently.
 */
@JvmSynthetic
internal const val WRAPPER_CLOSED_MESSAGE: String =
    "Wrapper has been closed; cannot perform async operation"

/**
 * Creates a fresh internal-only [CoroutineScope] for an interop helper to own.
 *
 * Used in two shapes:
 * - **One-shot Async helpers** (e.g. `connectAsync`, `archiveAsync`) build a scope, launch a
 *   single job, and let `invokeOnCompletion { scope.cancel() }` tear it down when the job
 *   finishes.
 * - **Long-lived `*CoroutineWrapper` instances** install this scope as a member field and reuse
 *   it across every `*Async` call on the wrapper; the scope lives until the consumer invokes
 *   [java.lang.AutoCloseable.close], which cancels it.
 *
 * Lifecycle / cancellation expectations therefore differ by caller — the scope itself is the
 * same shape in both, but it's the caller's responsibility to cancel it (one-shot via
 * `invokeOnCompletion`, long-lived via `close()`).
 *
 * When [callbackExecutor] is supplied, the scope uses it as its [kotlinx.coroutines.CoroutineDispatcher]
 * (via [Executor.asCoroutineDispatcher]). When [callbackExecutor] is `null`, the scope uses
 * [Dispatchers.IO].
 *
 * **What runs on the executor**: every part of the launched coroutine that does not explicitly
 * switch context — i.e. `onSuccess` / `onError` invocations, listener calls, **and the initial
 * dispatch of every suspend call / Flow `collect` block** — runs on this dispatcher. The
 * dispatcher only stops dictating the thread when a suspending callee internally `withContext`s
 * or `flowOn`s away.
 *
 * Passing a main-thread executor is therefore **not inherently safe** — it only works because the
 * SDK's own suspend operations wrap their blocking I/O via `withContext(Dispatchers.IO)` (HTTP,
 * disk, websocket). The convention applies to every coroutine surface this module exposes; new
 * SDK code that performs blocking work on the caller's dispatcher would surface on the main
 * thread of any consumer who passes `ContextCompat.getMainExecutor(context)`.
 *
 * The scope uses a [SupervisorJob] so cancelling a single launched job does not affect siblings.
 */
@JvmSynthetic
internal fun newInteropScope(callbackExecutor: Executor? = null): CoroutineScope =
    CoroutineScope(
        (callbackExecutor?.asCoroutineDispatcher() ?: Dispatchers.IO) + SupervisorJob()
    )

/**
 * One-shot async helper: builds a fresh [newInteropScope], launches [block] as a single job, and
 * tears the scope down via `invokeOnCompletion { scope.cancel() }` when the job finishes. Returns
 * a [Cancellable] handle over the launched job.
 *
 * This is the shared implementation of the `*Async` entry points across the JavaInterop files —
 * a fix to the scope/cancel/leak semantics belongs here rather than in each copy.
 *
 * [CheckResult] for the same reason as [asCancellable]: dropping the handle without cancelling
 * leaks the job (and any upstream subscription) until the scope tears down.
 */
@CheckResult
@JvmSynthetic
internal fun launchCancellable(
    callbackExecutor: Executor?,
    block: suspend () -> Unit,
): Cancellable {
    val scope = newInteropScope(callbackExecutor)
    val job = scope.launch { block() }
    job.invokeOnCompletion { scope.cancel() }
    return job.asCancellable()
}

/**
 * Shared implementation of the `*CoroutineWrapper` `*Async` launch pattern. Runs [block] on the
 * receiver [CoroutineScope] (the wrapper-owned scope), routing outcomes to [onSuccess]/[onError]:
 * a [CXoneException] passes through, any other non-cancellation throw is wrapped in [InternalError]
 * with [errorContext], and a consumer-side [onSuccess] throw deliberately escapes rather than being
 * mislabeled as an SDK error. If the scope is already closed (or a `close()` races the launch), the
 * wrapper-closed error is reported once, synchronously, on the calling thread.
 */
@JvmSynthetic
internal fun CoroutineScope.launchWithCallbacks(
    errorContext: String,
    onSuccess: Runnable?,
    onError: ErrorCallback?,
    block: suspend () -> Unit,
) {
    if (!isActive) {
        // Use-after-close fast path: scope already cancelled.
        onError?.onError(InternalError(WRAPPER_CLOSED_MESSAGE))
        return
    }
    val job = launch {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: CXoneException) {
            onError?.onError(e)
            return@launch
        } catch (expected: Exception) {
            onError?.onError(InternalError(errorContext, expected))
            return@launch
        }
        // Outside the SDK try/catch: consumer bugs in onSuccess do NOT get reported as SDK errors.
        onSuccess?.run()
    }
    if (job.isCancelled) {
        // Race plug: close() landed between the isActive check and launch, so the job is born
        // cancelled and the lambda above will never run. Notify the consumer once, synchronously,
        // on the calling thread — there is no scope left to dispatch on.
        onError?.onError(InternalError(WRAPPER_CLOSED_MESSAGE))
    }
}

/**
 * Collects [source] and forwards each emission to [listener] via [safeAccept]. If the upstream
 * [Flow] itself throws, the exception terminates the collector and is routed to [onError]:
 * [CXoneException] subclasses pass through unchanged, anything else is wrapped in [InternalError]
 * with [errorContext]. [CancellationException] is always rethrown to preserve cancellation.
 *
 * When [onError] is null an upstream throw escapes to the caller — typically the launched
 * coroutine — and from there to the thread's [Thread.UncaughtExceptionHandler].
 */
@JvmSynthetic
@Suppress("ThrowsCount") // Each throw expresses distinct intent: cancellation, null-onError CXone, null-onError unexpected.
internal suspend fun <T> collectWithErrorCallback(
    source: Flow<T>,
    listener: Consumer<T>,
    onError: ErrorCallback?,
    errorContext: String,
) {
    try {
        source.collect { value -> safeAccept(listener, value) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: CXoneException) {
        onError?.onError(e) ?: throw e
    } catch (expected: Exception) {
        val wrapped = InternalError(errorContext, expected)
        onError?.onError(wrapped) ?: throw wrapped
    }
}

/**
 * Invokes [Consumer.accept] inside a try/catch that swallows non-cancellation exceptions, so a
 * throw from a Flow-observer listener aborts only that single emission rather than terminating
 * the entire collector.
 *
 * Caught exceptions are forwarded to the current thread's [Thread.UncaughtExceptionHandler] — the
 * JVM-standard "we caught this defensively but it's still a bug" channel. Per the JDK spec, that
 * accessor is never null on a live thread: it falls back to the thread's [ThreadGroup] which
 * itself ultimately delegates to [Thread.getDefaultUncaughtExceptionHandler]. On stock Android the
 * default is `RuntimeInit.KillApplicationHandler`, which terminates the process — install a
 * custom handler at process start if you need a softer landing (e.g. log to Crashlytics and
 * swallow).
 *
 * [CancellationException] is always re-thrown to preserve structured-concurrency cancellation
 * semantics.
 */
@JvmSynthetic
internal fun <T> safeAccept(listener: Consumer<T>, value: T) {
    try {
        listener.accept(value)
    } catch (e: CancellationException) {
        throw e
    } catch (expected: Exception) {
        // Consumer-side bug in the listener; route through the JVM-standard uncaught-exception
        // channel so it's at least visible to the host process (Logcat / Sentry / etc.).
        // Per JDK spec the handler is non-null on a live thread (falls back to the ThreadGroup
        // which delegates to Thread.getDefaultUncaughtExceptionHandler); the `?.` is a
        // belt-and-braces guard for the (unreachable here) terminated-thread edge case.
        val thread = Thread.currentThread()
        thread.uncaughtExceptionHandler?.uncaughtException(thread, expected)
    }
}
