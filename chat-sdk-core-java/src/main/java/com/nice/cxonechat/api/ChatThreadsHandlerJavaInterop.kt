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

@file:JvmName("ChatThreadsHandlerJavaInterop")

package com.nice.cxonechat.api

import androidx.annotation.VisibleForTesting
import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.ChatThreadsHandler
import com.nice.cxonechat.Public
import com.nice.cxonechat.thread.ChatThread
import java.util.concurrent.Executor
import java.util.function.Consumer

/**
 * Java-friendly observer for [ChatThreadsHandler.threadsFlow].
 *
 * See [observeState] for notes on listener-throw isolation, upstream-error routing through
 * [onError], and the **mandatory** cancellation contract — the returned [Cancellable] MUST
 * be invoked to release listener resources.
 *
 * @param handler The [ChatThreadsHandler] whose threads to observe.
 * @param listener Invoked with each emitted list of [ChatThread]s.
 * @param onError Invoked once with an upstream-flow error before the collector terminates. May be null.
 * @param callbackExecutor If non-null, used as the collector coroutine's dispatcher so
 *                         [listener] runs on the executor's thread. When null, the listener
 *                         fires on the SDK's internal IO thread.
 * @return A [Cancellable] that cancels the collector. Must be invoked to release resources.
 */
@Public
@JvmOverloads
fun observeThreads(
    handler: ChatThreadsHandler,
    listener: Consumer<List<ChatThread>>,
    onError: ErrorCallback? = null,
    callbackExecutor: Executor? = null,
): Cancellable = launchCancellable(callbackExecutor) { collectThreadsFlow(handler, listener, onError) }

@VisibleForTesting
@JvmSynthetic
internal suspend fun collectThreadsFlow(
    handler: ChatThreadsHandler,
    listener: Consumer<List<ChatThread>>,
    onError: ErrorCallback? = null,
) {
    collectWithErrorCallback(handler.threadsFlow, listener, onError, "Unexpected error in threadsFlow observer")
}
