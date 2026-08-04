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

@file:JvmName("ChatThreadHandlerJavaInterop")

package com.nice.cxonechat.api

import androidx.annotation.VisibleForTesting
import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.Public
import com.nice.cxonechat.exceptions.CXoneException
import com.nice.cxonechat.exceptions.InternalError
import com.nice.cxonechat.thread.ChatThread
import kotlinx.coroutines.CancellationException
import java.util.concurrent.Executor
import java.util.function.Consumer

/**
 * Java-friendly asynchronous variant of [ChatThreadHandler.archive].
 *
 * Internally launches a background coroutine and notifies the supplied callbacks. Returns a
 * [Cancellable] that cancels the underlying work.
 *
 * See [connectAsync] for notes on null `onError` and exceptions thrown by `onSuccess`.
 *
 * @param handler The [ChatThreadHandler] whose thread will be archived.
 * @param onSuccess Invoked with the boolean archive-success flag.
 * @param onError Invoked when an error occurs. May be null.
 * @param callbackExecutor If non-null, used as the launched coroutine's dispatcher so
 *                         callbacks run on the executor's thread. When null, callbacks fire on
 *                         the SDK's internal IO thread.
 * @return A [Cancellable] that cancels the launched coroutine.
 */
@Public
@JvmOverloads
fun archiveAsync(
    handler: ChatThreadHandler,
    onSuccess: Consumer<Boolean>,
    onError: ErrorCallback? = null,
    callbackExecutor: Executor? = null,
): Cancellable = launchCancellable(callbackExecutor) { archiveThread(handler, onError, onSuccess) }

@VisibleForTesting
@JvmSynthetic
internal suspend fun archiveThread(
    handler: ChatThreadHandler,
    onError: ErrorCallback? = null,
    onSuccess: Consumer<Boolean>,
) {
    val result: Boolean = try {
        handler.archive()
    } catch (e: CancellationException) {
        throw e
    } catch (e: CXoneException) {
        onError?.onError(e)
        return
    } catch (expected: Exception) {
        onError?.onError(InternalError("Unexpected error archiving thread", expected))
        return
    }
    onSuccess.accept(result)
}

/**
 * Java-friendly observer for [ChatThreadHandler.threadFlow].
 *
 * See [observeState] for notes on listener-throw isolation, upstream-error routing through
 * [onError], and the **mandatory** cancellation contract — the returned [Cancellable] MUST
 * be invoked to release listener resources.
 *
 * @param handler The [ChatThreadHandler] whose thread to observe.
 * @param listener Invoked with each [ChatThread] emission.
 * @param onError Invoked once with an upstream-flow error before the collector terminates. May be null.
 * @param callbackExecutor If non-null, used as the collector coroutine's dispatcher so
 *                         [listener] runs on the executor's thread. When null, the listener
 *                         fires on the SDK's internal IO thread.
 * @return A [Cancellable] that cancels the collector. Must be invoked to release resources.
 */
@Public
@JvmOverloads
fun observeThread(
    handler: ChatThreadHandler,
    listener: Consumer<ChatThread>,
    onError: ErrorCallback? = null,
    callbackExecutor: Executor? = null,
): Cancellable = launchCancellable(callbackExecutor) { collectThreadFlow(handler, listener, onError) }

@VisibleForTesting
@JvmSynthetic
internal suspend fun collectThreadFlow(
    handler: ChatThreadHandler,
    listener: Consumer<ChatThread>,
    onError: ErrorCallback? = null,
) {
    collectWithErrorCallback(handler.threadFlow, listener, onError, "Unexpected error in threadFlow observer")
}
