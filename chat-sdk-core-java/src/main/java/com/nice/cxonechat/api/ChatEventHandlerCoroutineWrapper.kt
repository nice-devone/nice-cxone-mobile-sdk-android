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

import com.nice.cxonechat.ChatEventHandler
import com.nice.cxonechat.ChatEventHandlerActions.chatWindowOpen
import com.nice.cxonechat.ChatEventHandlerActions.conversion
import com.nice.cxonechat.ChatEventHandlerActions.customVisitor
import com.nice.cxonechat.ChatEventHandlerActions.event
import com.nice.cxonechat.ChatEventHandlerActions.pageView
import com.nice.cxonechat.ChatEventHandlerActions.pageViewEnded
import com.nice.cxonechat.ChatEventHandlerActions.proactiveActionClick
import com.nice.cxonechat.ChatEventHandlerActions.proactiveActionDisplay
import com.nice.cxonechat.ChatEventHandlerActions.proactiveActionFailure
import com.nice.cxonechat.ChatEventHandlerActions.proactiveActionSuccess
import com.nice.cxonechat.Public
import com.nice.cxonechat.analytics.ActionMetadata
import com.nice.cxonechat.event.ChatEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.util.UUID
import java.util.concurrent.Executor
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Java-friendly wrapper for [ChatEventHandler] that uses coroutines internally to dispatch
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
 * private final ChatEventHandlerCoroutineWrapper wrapper =
 *         new ChatEventHandlerCoroutineWrapper(handler, ContextCompat.getMainExecutor(context));
 *
 * void trackPageView(String title, String uri) {
 *     wrapper.pageViewAsync(title, uri, new Date(), () -> {
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
 * @param handler The underlying [ChatEventHandler] to wrap.
 * @param coroutineScope Internal-only — the scope the wrapper launches work on and cancels in
 *                       [close]. Java consumers should use the public constructor instead;
 *                       it builds a fresh wrapper-owned scope via [newInteropScope].
 */
@Public
@Suppress(
    "TooManyFunctions", // Copying public API from ChatEventHandlerActions for Java consumers
)
class ChatEventHandlerCoroutineWrapper private constructor(
    private val handler: ChatEventHandler,
    private val coroutineScope: CoroutineScope,
) : ChatEventHandler by handler, AutoCloseable {

    /**
     * Java-facing constructor; the wrapper owns and cancels its own coroutine scope on [close].
     *
     * @param handler The underlying [ChatEventHandler] to wrap.
     * @param callbackExecutor If non-null, the wrapper's coroutine scope uses this [Executor] as
     *                         its [kotlinx.coroutines.CoroutineDispatcher], so all `onSuccess` and
     *                         `onError` callbacks resume on the executor's thread (e.g. a
     *                         main-thread executor for Android UI updates). When null, callbacks
     *                         fire on the SDK's internal IO thread.
     */
    @JvmOverloads
    constructor(handler: ChatEventHandler, callbackExecutor: Executor? = null) : this(
        handler = handler,
        // Public path: the wrapper's scope IS the freshly-created interop scope, so close()
        // cancels everything the wrapper allocated — the SupervisorJob and the dispatcher
        // wrapping callbackExecutor. No parent/child layering.
        coroutineScope = newInteropScope(callbackExecutor),
    )

    @JvmSynthetic
    override suspend fun trigger(event: ChatEvent<*>) {
        handler.trigger(event)
    }

    /**
     * Triggers a [ChatEvent] asynchronously using coroutines, with callbacks for success and error.
     *
     * @param event The [ChatEvent] to trigger.
     * @param onSuccess Callback invoked when the event is sent successfully. May be null.
     * @param onError Callback invoked when an error occurs. May be null.
     */
    @JvmOverloads
    fun triggerAsync(
        event: ChatEvent<*>,
        onSuccess: Runnable? = null,
        onError: ErrorCallback? = null,
    ) = coroutineScope.launchWithCallbacks(
        errorContext = "Unexpected error triggering event",
        onSuccess = onSuccess,
        onError = onError,
    ) { trigger(event) }

    /**
     * Sends a ChatWindowOpen event to the analytics server asynchronously.
     *
     * @param instant A moment when the event has happened.
     * @param onSuccess Callback invoked when the event is sent successfully. May be null.
     * @param onError Callback invoked when an error occurs. May be null.
     */
    @JvmOverloads
    fun chatWindowOpenAsync(
        instant: Instant = Clock.System.now(),
        onSuccess: Runnable? = null,
        onError: ErrorCallback? = null,
    ) = coroutineScope.launchWithCallbacks(
        errorContext = "Unexpected error sending chat window open event",
        onSuccess = onSuccess,
        onError = onError,
    ) { chatWindowOpen(instant) }

    /**
     * Sends a conversion event to the analytics server asynchronously.
     *
     * @param type Application-specific string reflecting the type of conversion.
     * @param value Application-specific value of the conversion.
     * @param instant A moment when the conversion event has happened.
     * @param onSuccess Callback invoked when the event is sent successfully. May be null.
     * @param onError Callback invoked when an error occurs. May be null.
     */
    @JvmOverloads
    fun conversionAsync(
        type: String,
        value: Number,
        instant: Instant = Clock.System.now(),
        onSuccess: Runnable? = null,
        onError: ErrorCallback? = null,
    ) = coroutineScope.launchWithCallbacks(
        errorContext = "Unexpected error sending conversion event",
        onSuccess = onSuccess,
        onError = onError,
    ) { conversion(type, value, instant) }

    /**
     * Sends a custom visitor analytics event asynchronously.
     *
     * @param data Data to be sent in the custom event. Must be JSON-encodable.
     * @param onSuccess Callback invoked when the event is sent successfully. May be null.
     * @param onError Callback invoked when an error occurs. May be null.
     */
    @JvmOverloads
    fun customVisitorAsync(
        data: Any,
        onSuccess: Runnable? = null,
        onError: ErrorCallback? = null,
    ) = coroutineScope.launchWithCallbacks(
        errorContext = "Unexpected error sending custom visitor event",
        onSuccess = onSuccess,
        onError = onError,
    ) { customVisitor(data) }

    /**
     * Sends a page view event to the analytics server asynchronously.
     *
     * @param title Application-defined title uniquely identifying the page viewed.
     * @param uri Application-defined URI uniquely identifying the page viewed.
     * @param instant A moment when the event has happened.
     * @param onSuccess Callback invoked when the event is sent successfully. May be null.
     * @param onError Callback invoked when an error occurs. May be null.
     */
    @JvmOverloads
    fun pageViewAsync(
        title: String,
        uri: String,
        instant: Instant = Clock.System.now(),
        onSuccess: Runnable? = null,
        onError: ErrorCallback? = null,
    ) = coroutineScope.launchWithCallbacks(
        errorContext = "Unexpected error sending page view event",
        onSuccess = onSuccess,
        onError = onError,
    ) { pageView(title, uri, instant) }

    /**
     * Sends a page view ended event to the analytics server asynchronously.
     *
     * @param title Application-defined title uniquely identifying the page viewed.
     * @param uri Application-defined URI uniquely identifying the page viewed.
     * @param instant A moment when the event has happened.
     * @param onSuccess Callback invoked when the event is sent successfully. May be null.
     * @param onError Callback invoked when an error occurs. May be null.
     */
    @JvmOverloads
    fun pageViewEndedAsync(
        title: String,
        uri: String,
        instant: Instant = Clock.System.now(),
        onSuccess: Runnable? = null,
        onError: ErrorCallback? = null,
    ) = coroutineScope.launchWithCallbacks(
        errorContext = "Unexpected error sending page view ended event",
        onSuccess = onSuccess,
        onError = onError,
    ) { pageViewEnded(title, uri, instant) }

    /**
     * Sends a proactive action click event to the analytics asynchronously.
     *
     * @param data [ActionMetadata] provided in the proactive action popup.
     * @param instant A moment when the event has happened.
     * @param onSuccess Callback invoked when the event is sent successfully. May be null.
     * @param onError Callback invoked when an error occurs. May be null.
     */
    @JvmOverloads
    fun proactiveActionClickAsync(
        data: ActionMetadata,
        instant: Instant = Clock.System.now(),
        onSuccess: Runnable? = null,
        onError: ErrorCallback? = null,
    ) = coroutineScope.launchWithCallbacks(
        errorContext = "Unexpected error sending proactive action click event",
        onSuccess = onSuccess,
        onError = onError,
    ) { proactiveActionClick(data, instant) }

    /**
     * Sends a proactive action display event to the analytics asynchronously.
     *
     * @param data [ActionMetadata] provided in the proactive action popup.
     * @param instant A moment when the event has happened.
     * @param onSuccess Callback invoked when the event is sent successfully. May be null.
     * @param onError Callback invoked when an error occurs. May be null.
     */
    @JvmOverloads
    fun proactiveActionDisplayAsync(
        data: ActionMetadata,
        instant: Instant = Clock.System.now(),
        onSuccess: Runnable? = null,
        onError: ErrorCallback? = null,
    ) = coroutineScope.launchWithCallbacks(
        errorContext = "Unexpected error sending proactive action display event",
        onSuccess = onSuccess,
        onError = onError,
    ) { proactiveActionDisplay(data, instant) }

    /**
     * Sends a proactive action failure event to the analytics asynchronously.
     *
     * @param data [ActionMetadata] provided in the proactive action popup.
     * @param instant A moment when the event has happened.
     * @param onSuccess Callback invoked when the event is sent successfully. May be null.
     * @param onError Callback invoked when an error occurs. May be null.
     */
    @JvmOverloads
    fun proactiveActionFailureAsync(
        data: ActionMetadata,
        instant: Instant = Clock.System.now(),
        onSuccess: Runnable? = null,
        onError: ErrorCallback? = null,
    ) = coroutineScope.launchWithCallbacks(
        errorContext = "Unexpected error sending proactive action failure event",
        onSuccess = onSuccess,
        onError = onError,
    ) { proactiveActionFailure(data, instant) }

    /**
     * Sends a proactive action success event to the analytics asynchronously.
     *
     * @param data [ActionMetadata] provided in the proactive action popup.
     * @param instant A moment when the event has happened.
     * @param onSuccess Callback invoked when the event is sent successfully. May be null.
     * @param onError Callback invoked when an error occurs. May be null.
     */
    @JvmOverloads
    fun proactiveActionSuccessAsync(
        data: ActionMetadata,
        instant: Instant = Clock.System.now(),
        onSuccess: Runnable? = null,
        onError: ErrorCallback? = null,
    ) = coroutineScope.launchWithCallbacks(
        errorContext = "Unexpected error sending proactive action success event",
        onSuccess = onSuccess,
        onError = onError,
    ) { proactiveActionSuccess(data, instant) }

    /**
     * Triggers a custom event specified by ID asynchronously.
     *
     * @param id ID of event to trigger per representative instructions.
     * @param onSuccess Callback invoked when the event is triggered successfully. May be null.
     * @param onError Callback invoked when an error occurs. May be null.
     */
    @JvmOverloads
    fun eventAsync(
        id: UUID,
        onSuccess: Runnable? = null,
        onError: ErrorCallback? = null,
    ) = coroutineScope.launchWithCallbacks(
        errorContext = "Unexpected error triggering custom event",
        onSuccess = onSuccess,
        onError = onError,
    ) { event(id) }

    override fun close() {
        coroutineScope.cancel()
    }

    /**
     * Companion holds an internal `invoke` operator so Kotlin unit tests can construct the wrapper
     * with a [CoroutineScope] (e.g. `runTest`'s `TestScope`). The operator is `@JvmSynthetic`, so
     * Java consumers cannot reach it — the only Java entry points are the public constructors.
     */
    internal companion object {
        @JvmSynthetic
        internal operator fun invoke(
            handler: ChatEventHandler,
            parentScope: CoroutineScope,
        ): ChatEventHandlerCoroutineWrapper =
            ChatEventHandlerCoroutineWrapper(
                handler = handler,
                // Internal/test path: caller owns parentScope (e.g. runTest's TestScope).
                // Build a child SupervisorJob so close() cancels only the wrapper's work,
                // not the caller's scope.
                coroutineScope = CoroutineScope(
                    parentScope.coroutineContext + SupervisorJob(parentScope.coroutineContext[Job])
                ),
            )
    }
}
