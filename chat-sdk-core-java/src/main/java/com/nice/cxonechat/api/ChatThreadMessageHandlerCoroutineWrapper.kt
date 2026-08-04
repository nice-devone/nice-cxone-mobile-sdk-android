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

package com.nice.cxonechat.api

import com.nice.cxonechat.ChatThreadMessageHandler
import com.nice.cxonechat.Public
import com.nice.cxonechat.exceptions.CXoneException
import com.nice.cxonechat.exceptions.InternalError
import com.nice.cxonechat.message.ContentDescriptor
import com.nice.cxonechat.message.OutboundMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import java.util.function.Consumer

/**
 * Java-friendly wrapper for [ChatThreadMessageHandler] that uses coroutines internally,
 * exposing a callback-based API for Java consumers.
 *
 * This wrapper implements [AutoCloseable]. Consumers **must** call [close] when the wrapper
 * is no longer needed to cancel the internal coroutine scope and prevent resource leaks.
 *
 * Example usage from Java (callbacks dispatched on the Android main thread):
 * ```java
 * private final ChatThreadMessageHandlerCoroutineWrapper wrapper =
 *         new ChatThreadMessageHandlerCoroutineWrapper(handler, ContextCompat.getMainExecutor(context));
 *
 * void sendChatMessage(OutboundMessage message) {
 *     wrapper.sendAsync(message, (id) -> {
 *         // Success — resumes on the main thread; id is the assigned message ID
 *     }, (exception) -> {
 *         // Error — resumes on the main thread
 *     });
 * }
 *
 * void cleanup() { wrapper.close(); }
 * ```
 *
 * **Note:** Do not use try-with-resources — closing the wrapper cancels all in-flight
 * async operations. Instead, tie the wrapper's lifecycle to the owning component.
 *
 * @param handler The underlying [ChatThreadMessageHandler] to wrap.
 * @param coroutineScope Internal-only — the scope the wrapper launches work on and cancels in
 *                       [close]. Java consumers should use the public constructor instead;
 *                       it builds a fresh wrapper-owned scope via [newInteropScope].
 */
@Public
class ChatThreadMessageHandlerCoroutineWrapper private constructor(
    private val handler: ChatThreadMessageHandler,
    private val coroutineScope: CoroutineScope,
) : ChatThreadMessageHandler by handler, AutoCloseable {

    /**
     * Java-facing constructor; the wrapper owns and cancels its own coroutine scope on [close].
     *
     * @param handler The underlying [ChatThreadMessageHandler] to wrap.
     * @param callbackExecutor If non-null, the wrapper's coroutine scope uses this [Executor] as
     *                         its [kotlinx.coroutines.CoroutineDispatcher], so all callbacks
     *                         resume on the executor's thread. When null, callbacks fire on the
     *                         SDK's internal IO thread.
     */
    @JvmOverloads
    @Suppress(
        "Unused" // Public API
    )
    constructor(handler: ChatThreadMessageHandler, callbackExecutor: Executor? = null) : this(
        handler = handler,
        // Public path: the wrapper's scope IS the freshly-created interop scope, so close()
        // cancels everything the wrapper allocated — the SupervisorJob and the dispatcher
        // wrapping callbackExecutor.
        coroutineScope = newInteropScope(callbackExecutor),
    )

    @JvmSynthetic
    override suspend fun send(message: OutboundMessage): String = handler.send(message)

    @JvmSynthetic
    override suspend fun send(
        message: String,
        postback: String?,
    ): String = handler.send(message, postback)

    @JvmSynthetic
    override suspend fun send(
        attachments: Iterable<ContentDescriptor>,
        message: String,
        postback: String?,
    ): String = handler.send(attachments, message, postback)

    /**
     * Sends a message asynchronously, with callbacks for success and error.
     *
     * @param message The [OutboundMessage] to send.
     * @param onSuccess Callback invoked with the message ID when the `send` succeeds. May be null.
     * @param onError Callback invoked when an error occurs. May be null.
     */
    @JvmOverloads
    fun sendAsync(
        message: OutboundMessage,
        onSuccess: Consumer<String>? = null,
        onError: ErrorCallback? = null,
    ) {
        if (!coroutineScope.isActive) {
            onError?.onError(InternalError(WRAPPER_CLOSED_MESSAGE))
            return
        }
        val job = coroutineScope.launch {
            val id: String = try {
                send(message)
            } catch (e: CancellationException) {
                throw e
            } catch (e: CXoneException) {
                onError?.onError(e)
                return@launch
            } catch (expected: Exception) {
                onError?.onError(InternalError("Unexpected error sending message", expected))
                return@launch
            }
            onSuccess?.accept(id)
        }
        if (job.isCancelled) {
            // Race plug: close() landed between the isActive check and launch.
            onError?.onError(InternalError(WRAPPER_CLOSED_MESSAGE))
        }
    }

    /**
     * Simplified method to send a text message asynchronously.
     *
     * @param message message to send to agent.
     * @param postback optional "postback" to send with the message.
     * @param onSuccess Callback invoked with the message ID when the `send` succeeds. May be null.
     * @param onError Callback invoked when an error occurs. May be null.
     */
    @JvmOverloads
    fun sendAsync(
        message: String,
        postback: String? = null,
        onSuccess: Consumer<String>? = null,
        onError: ErrorCallback? = null,
    ) {
        sendAsync(OutboundMessage(message, postback = postback), onSuccess, onError)
    }

    /**
     * Simplified method to send attachments asynchronously.
     *
     * @param attachments attachments to send.
     * @param message optional message to send with attachments.
     * @param postback optional "postback" to send with attachments.
     * @param onSuccess Callback invoked with the message ID when the `send` succeeds. May be null.
     * @param onError Callback invoked when an error occurs. May be null.
     */
    @JvmOverloads
    fun sendAsync(
        attachments: Iterable<ContentDescriptor>,
        message: String = "",
        postback: String? = null,
        onSuccess: Consumer<String>? = null,
        onError: ErrorCallback? = null,
    ) {
        sendAsync(OutboundMessage(attachments, message, postback), onSuccess, onError)
    }

    override fun close() {
        coroutineScope.cancel()
    }

    /**
     * Companion holds an internal `invoke` operator so Kotlin unit tests can construct the wrapper
     * with a [CoroutineScope]. `@JvmSynthetic` keeps it invisible to Java consumers.
     */
    internal companion object {
        @JvmSynthetic
        @PublishedApi
        internal operator fun invoke(
            handler: ChatThreadMessageHandler,
            parentScope: CoroutineScope,
        ): ChatThreadMessageHandlerCoroutineWrapper =
            ChatThreadMessageHandlerCoroutineWrapper(
                handler = handler,
                // Internal/test path: caller owns parentScope. Build a child SupervisorJob so
                // close() cancels only the wrapper's work, not the caller's scope.
                coroutineScope = CoroutineScope(
                    parentScope.coroutineContext + SupervisorJob(parentScope.coroutineContext[Job])
                ),
            )
    }
}
