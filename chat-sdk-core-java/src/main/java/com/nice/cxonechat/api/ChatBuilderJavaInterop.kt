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

@file:JvmName("ChatBuilderJavaInterop")

package com.nice.cxonechat.api

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.Chat
import com.nice.cxonechat.ChatBuilder
import com.nice.cxonechat.Public
import com.nice.cxonechat.SocketFactoryConfiguration
import com.nice.cxonechat.exceptions.CXoneException
import com.nice.cxonechat.exceptions.InternalError
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerNoop
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executor
import java.util.function.Consumer

/**
 * Java-friendly blocking factory for [ChatBuilder].
 *
 * Replaces `ChatBuilder.getDefaultBlocking(...)`, which was removed in 4.0 when Java
 * interop helpers were extracted into the `chat-sdk-core-java` artifact.
 *
 * Kotlin consumers should prefer the suspend [ChatBuilder.getDefault] directly.
 *
 * **Must NOT be called on the main thread** — performs blocking I/O for HTTP client and
 * persistent-storage setup.
 *
 * @param context [Context] used for persistent storage of values by the SDK.
 * @param config [SocketFactoryConfiguration] connection configuration of the chat.
 * @param logger [Logger] used by the builder and the SDK; defaults to a no-op implementation.
 * @return A configured [ChatBuilder] ready for use.
 */
@Public
@JvmOverloads
@WorkerThread
fun getDefaultBlocking(
    context: Context,
    config: SocketFactoryConfiguration,
    logger: Logger = LoggerNoop,
): ChatBuilder = runBlocking { ChatBuilder.getDefault(context, config, logger) }

/**
 * Java-friendly asynchronous factory for [ChatBuilder].
 *
 * Internally launches a background coroutine and notifies the supplied callbacks. Returns a
 * [Cancellable] that cancels the underlying work.
 *
 * Kotlin consumers should prefer the suspend [ChatBuilder.getDefault] directly.
 *
 * See [connectAsync] for notes on null `onError` and exceptions thrown by `onSuccess`.
 *
 * @param context [Context] used for persistent storage of values by the SDK.
 * @param config [SocketFactoryConfiguration] connection configuration of the chat.
 * @param onSuccess Invoked when the builder is created successfully.
 * @param onError Invoked when an error occurs. May be null.
 * @param logger [Logger] used by the builder and the SDK; defaults to a no-op implementation.
 * @param callbackExecutor If non-null, the launched coroutine uses this [Executor] as its
 *                         [kotlinx.coroutines.CoroutineDispatcher], so [onSuccess] and [onError]
 *                         resume on the executor's thread (e.g. a main-thread executor for
 *                         Android UI updates). When null, callbacks fire on the SDK's internal
 *                         IO thread.
 * @return A [Cancellable] that cancels the launched coroutine.
 */
@Public
@JvmOverloads
@Suppress("LongParameterList") // Java-interop signature needs every parameter for full control.
fun getDefaultAsync(
    context: Context,
    config: SocketFactoryConfiguration,
    onSuccess: Consumer<ChatBuilder>,
    onError: ErrorCallback? = null,
    logger: Logger = LoggerNoop,
    callbackExecutor: Executor? = null,
): Cancellable = launchCancellable(callbackExecutor) {
    getDefaultChatBuilder(context, config, logger, onError, onSuccess)
}

@VisibleForTesting
@JvmSynthetic
internal suspend fun getDefaultChatBuilder(
    context: Context,
    config: SocketFactoryConfiguration,
    logger: Logger = LoggerNoop,
    onError: ErrorCallback? = null,
    onSuccess: Consumer<ChatBuilder>,
) {
    val result: ChatBuilder = try {
        ChatBuilder.getDefault(context, config, logger)
    } catch (e: CancellationException) {
        throw e
    } catch (e: CXoneException) {
        onError?.onError(e)
        return
    } catch (expected: Exception) {
        onError?.onError(InternalError("Unexpected error creating ChatBuilder", expected))
        return
    }
    onSuccess.accept(result)
}

/**
 * Java-friendly asynchronous variant of [ChatBuilder.build].
 *
 * Internally launches a background coroutine and notifies the supplied callbacks. Returns a
 * [Cancellable] that cancels the underlying work.
 *
 * See [connectAsync] for notes on null `onError` and exceptions thrown by `onSuccess`.
 *
 * @param builder The [ChatBuilder] to build.
 * @param onSuccess Invoked with the built [Chat] instance on success.
 * @param onError Invoked when an error occurs. May be null.
 * @param callbackExecutor If non-null, used as the launched coroutine's dispatcher so
 *                         callbacks run on the executor's thread. When null, callbacks fire on
 *                         the SDK's internal IO thread.
 * @return A [Cancellable] that cancels the launched coroutine.
 */
@Public
@JvmOverloads
fun buildAsync(
    builder: ChatBuilder,
    onSuccess: Consumer<Chat>,
    onError: ErrorCallback? = null,
    callbackExecutor: Executor? = null,
): Cancellable = launchCancellable(callbackExecutor) { buildChat(builder, onError, onSuccess) }

@VisibleForTesting
@JvmSynthetic
internal suspend fun buildChat(
    builder: ChatBuilder,
    onError: ErrorCallback? = null,
    onSuccess: Consumer<Chat>,
) {
    val result: Chat = try {
        builder.build()
    } catch (e: CancellationException) {
        throw e
    } catch (e: CXoneException) {
        onError?.onError(e)
        return
    } catch (expected: Exception) {
        onError?.onError(InternalError("Unexpected error building Chat", expected))
        return
    }
    onSuccess.accept(result)
}
