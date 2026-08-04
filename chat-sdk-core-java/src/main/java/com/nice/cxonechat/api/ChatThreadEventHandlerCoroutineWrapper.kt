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

import com.nice.cxonechat.ChatThreadEventHandler
import com.nice.cxonechat.ChatThreadEventHandlerActions.loadMetadata
import com.nice.cxonechat.ChatThreadEventHandlerActions.markThreadRead
import com.nice.cxonechat.ChatThreadEventHandlerActions.sendTranscript
import com.nice.cxonechat.ChatThreadEventHandlerActions.triggerAction
import com.nice.cxonechat.ChatThreadEventHandlerActions.typingEnd
import com.nice.cxonechat.ChatThreadEventHandlerActions.typingStart
import com.nice.cxonechat.EventResponse
import com.nice.cxonechat.Public
import com.nice.cxonechat.event.thread.ChatThreadEvent
import com.nice.cxonechat.exceptions.CXoneException
import com.nice.cxonechat.exceptions.InternalError
import com.nice.cxonechat.message.Action
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
 * Java-friendly wrapper for [ChatThreadEventHandler] that uses coroutines internally to dispatch
 * events asynchronously, exposing a callback-based API for Java consumers.
 *
 * This class bridges the gap between Kotlin coroutines and Java callback-based code,
 * allowing Java consumers to benefit from the coroutine-based implementation while maintaining
 * familiar callback patterns.
 *
 * This wrapper implements [AutoCloseable]. Consumers **must** call [close] when the wrapper
 * is no longer needed to cancel the internal coroutine scope and prevent resource leaks.
 *
 * Example usage from Java (callbacks dispatched on the Android main thread):
 * ```java
 * private final ChatThreadEventHandlerCoroutineWrapper wrapper =
 *         new ChatThreadEventHandlerCoroutineWrapper(handler, ContextCompat.getMainExecutor(context));
 *
 * void onUserTyping(ChatThreadEvent event) {
 *     wrapper.triggerAsync(event, () -> {
 *         // Success callback — resumes on the main thread
 *     }, (exception) -> {
 *         // Error callback — resumes on the main thread
 *     });
 * }
 *
 * void cleanup() { wrapper.close(); }
 * ```
 *
 * **Note:** Do not use try-with-resources — closing the wrapper cancels all in-flight
 * async operations. Instead, tie the wrapper's lifecycle to the owning component.
 *
 * @param handler The underlying [ChatThreadEventHandler] to wrap.
 * @param coroutineScope Internal-only — the scope the wrapper launches work on and cancels in
 *                       [close]. Java consumers should use the public `constructor` instead;
 *                       it builds a fresh wrapper-owned scope via [newInteropScope].
 */
@Public
@Suppress("TooManyFunctions")
class ChatThreadEventHandlerCoroutineWrapper private constructor(
    private val handler: ChatThreadEventHandler,
    private val coroutineScope: CoroutineScope,
) : ChatThreadEventHandler by handler, AutoCloseable {

    /**
     * Java-facing constructor; the wrapper owns and cancels its own coroutine scope on [close].
     *
     * @param handler The underlying [ChatThreadEventHandler] to wrap.
     * @param callbackExecutor If non-null, the wrapper's coroutine scope uses this [Executor] as
     *                         its [kotlinx.coroutines.CoroutineDispatcher], so all callbacks
     *                         resume on the executor's thread. When null, callbacks fire on the
     *                         SDK's internal IO thread.
     */
    @JvmOverloads
    @Suppress(
        "Unused" // Public API
    )
    constructor(handler: ChatThreadEventHandler, callbackExecutor: Executor? = null) : this(
        handler = handler,
        // Public path: the wrapper's scope IS the freshly-created interop scope, so close()
        // cancels everything the wrapper allocated — the SupervisorJob and the dispatcher
        // wrapping callbackExecutor.
        coroutineScope = newInteropScope(callbackExecutor),
    )

    @JvmSynthetic
    override suspend fun trigger(event: ChatThreadEvent): EventResponse? = handler.trigger(event)

    /**
     * Triggers a [ChatThreadEvent] asynchronously using coroutines, with callbacks for success and error.
     *
     * @param event The [ChatThreadEvent] to trigger.
     * @param onSuccess Callback invoked when the event is sent successfully. May be null.
     * @param onError Callback invoked when an error occurs. May be null.
     */
    @JvmOverloads
    fun triggerAsync(
        event: ChatThreadEvent,
        onSuccess: Runnable? = null,
        onError: ErrorCallback? = null,
    ) = coroutineScope.launchWithCallbacks(
        errorContext = "Unexpected error triggering event",
        onSuccess = onSuccess,
        onError = onError,
    ) { trigger(event) }

    /**
     * Marks the thread as read asynchronously.
     *
     * @param onSuccess Callback invoked when the thread is marked as read successfully. May be null.
     * @param onError Callback invoked when an error occurs. May be null.
     */
    @JvmOverloads
    fun markThreadReadAsync(
        onSuccess: Runnable? = null,
        onError: ErrorCallback? = null,
    ) = coroutineScope.launchWithCallbacks(
        errorContext = "Unexpected error marking thread as read",
        onSuccess = onSuccess,
        onError = onError,
    ) { markThreadRead() }

    /**
     * Notifies the server that the user has stopped typing, asynchronously.
     *
     * @param onSuccess Callback invoked when the event is sent successfully. May be null.
     * @param onError Callback invoked when an error occurs. May be null.
     */
    @JvmOverloads
    fun typingEndAsync(
        onSuccess: Runnable? = null,
        onError: ErrorCallback? = null,
    ) = coroutineScope.launchWithCallbacks(
        errorContext = "Unexpected error sending typing end event",
        onSuccess = onSuccess,
        onError = onError,
    ) { typingEnd() }

    /**
     * Notifies the agent that the user has started typing, asynchronously.
     *
     * @param onSuccess Callback invoked when the event is sent successfully. May be null.
     * @param onError Callback invoked when an error occurs. May be null.
     */
    @JvmOverloads
    fun typingStartAsync(
        onSuccess: Runnable? = null,
        onError: ErrorCallback? = null,
    ) = coroutineScope.launchWithCallbacks(
        errorContext = "Unexpected error sending typing start event",
        onSuccess = onSuccess,
        onError = onError,
    ) { typingStart() }

    /**
     * Requests additional thread metadata asynchronously.
     *
     * @param onSuccess Callback invoked when metadata is loaded successfully. May be null.
     * @param onError Callback invoked when an error occurs. May be null.
     */
    @JvmOverloads
    fun loadMetadataAsync(
        onSuccess: Runnable? = null,
        onError: ErrorCallback? = null,
    ) = coroutineScope.launchWithCallbacks(
        errorContext = "Unexpected error loading metadata",
        onSuccess = onSuccess,
        onError = onError,
    ) { loadMetadata() }

    /**
     * Triggers an action event asynchronously based on the provided [Action].
     *
     * @param action The [Action] that defines the event to be triggered.
     * @param onSuccess Callback invoked when the action is triggered successfully. May be null.
     * @param onError Callback invoked when an error occurs. May be null.
     */
    @JvmOverloads
    fun triggerActionAsync(
        action: Action,
        onSuccess: Runnable? = null,
        onError: ErrorCallback? = null,
    ) = coroutineScope.launchWithCallbacks(
        errorContext = "Unexpected error triggering action",
        onSuccess = onSuccess,
        onError = onError,
    ) { triggerAction(action) }

    /**
     * Sends the chat transcript to the specified email address asynchronously.
     *
     * @param email The email address to which the transcript will be sent.
     * @param onSuccess Callback invoked with the [EventResponse] when the transcript is sent successfully. May be null.
     * @param onError Callback invoked when an error occurs. May be null.
     */
    @JvmOverloads
    fun sendTranscriptAsync(
        email: String,
        onSuccess: Consumer<EventResponse?>? = null,
        onError: ErrorCallback? = null,
    ) {
        if (!coroutineScope.isActive) {
            onError?.onError(InternalError(WRAPPER_CLOSED_MESSAGE))
            return
        }
        val job = coroutineScope.launch {
            val result: EventResponse? = try {
                sendTranscript(email)
            } catch (e: CancellationException) {
                throw e
            } catch (e: CXoneException) {
                onError?.onError(e)
                return@launch
            } catch (expected: Exception) {
                onError?.onError(InternalError("Unexpected error sending transcript", expected))
                return@launch
            }
            onSuccess?.accept(result)
        }
        if (job.isCancelled) {
            onError?.onError(InternalError(WRAPPER_CLOSED_MESSAGE))
        }
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
        internal operator fun invoke(
            handler: ChatThreadEventHandler,
            parentScope: CoroutineScope,
        ): ChatThreadEventHandlerCoroutineWrapper =
            ChatThreadEventHandlerCoroutineWrapper(
                handler = handler,
                // Internal/test path: caller owns parentScope. Build a child SupervisorJob so
                // close() cancels only the wrapper's work, not the caller's scope.
                coroutineScope = CoroutineScope(
                    parentScope.coroutineContext + SupervisorJob(parentScope.coroutineContext[Job])
                ),
            )
    }
}
