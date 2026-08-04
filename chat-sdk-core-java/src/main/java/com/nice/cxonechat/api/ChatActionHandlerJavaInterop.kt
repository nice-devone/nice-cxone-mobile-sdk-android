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

@file:JvmName("ChatActionHandlerJavaInterop")

package com.nice.cxonechat.api

import androidx.annotation.VisibleForTesting
import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.ChatActionHandler
import com.nice.cxonechat.PopupEvent
import com.nice.cxonechat.Public
import java.util.concurrent.Executor
import java.util.function.Consumer

/**
 * Java-friendly observer for [ChatActionHandler.popupFlow].
 *
 * See [observeState] for notes on listener-throw isolation, upstream-error routing through
 * [onError], and the **mandatory** cancellation contract — the returned [Cancellable] MUST
 * be invoked to release listener resources.
 *
 * @param handler The [ChatActionHandler] whose popups to observe.
 * @param listener Invoked for each [PopupEvent] emission.
 * @param onError Invoked once with an upstream-flow error before the collector terminates. May be null.
 * @param callbackExecutor If non-null, used as the collector coroutine's dispatcher so
 *                         [listener] runs on the executor's thread. When null, the listener
 *                         fires on the SDK's internal IO thread.
 * @return A [Cancellable] that cancels the collector. Must be invoked to release resources.
 */
@Public
@JvmOverloads
fun onPopup(
    handler: ChatActionHandler,
    listener: Consumer<PopupEvent>,
    onError: ErrorCallback? = null,
    callbackExecutor: Executor? = null,
): Cancellable = launchCancellable(callbackExecutor) { collectPopupFlow(handler, listener, onError) }

@VisibleForTesting
@JvmSynthetic
internal suspend fun collectPopupFlow(
    handler: ChatActionHandler,
    listener: Consumer<PopupEvent>,
    onError: ErrorCallback? = null,
) {
    collectWithErrorCallback(handler.popupFlow, listener, onError, "Unexpected error in popupFlow observer")
}
