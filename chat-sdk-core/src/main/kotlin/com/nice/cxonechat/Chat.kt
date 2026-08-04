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

package com.nice.cxonechat

import androidx.annotation.WorkerThread
import com.nice.cxonechat.ChatMode.MultiThread
import com.nice.cxonechat.ChatMode.SingleThread
import com.nice.cxonechat.state.Configuration
import com.nice.cxonechat.state.Environment
import com.nice.cxonechat.thread.CustomField
import kotlinx.coroutines.flow.SharedFlow

/**
 * Current chat instance. Client is not limited to a single instance of any given chat.
 * They can create (or rather [build][ChatBuilder.build]) as many instances as they like.
 * They are though advised that creating *CONFLICTING* (or identical) instances with the
 * same configuration may lead to unexpected consequences.
 *
 * After creating an instance via [build][ChatBuilder.build], the customer is automatically
 * authorized if the authorization is enabled and the customer was not previously authorized,
 * otherwise reconnects the customer.
 * The created chat instance also starts listening for the authorization and updates customer details
 * after customer authorization. The time it takes to reach the device is undefined.
 *
 * Moreover, it starts listening to events that modify welcome messages. Therefore, a welcome
 * message is only available after we receive the given event and requires no actions from
 * the client side. The time it takes to reach the device is undefined.
 * */
@Public
@Suppress(
    "ComplexInterface",
    "TooManyFunctions",
)
interface Chat : AutoCloseable {

    /**
     * Current environment provided by the [SocketFactoryConfiguration]. It will remain
     * unchanged for the duration while this object exists.
     * */
    val environment: Environment

    /**
     * Current options of the channel as received from the backend.
     * It will remain unchanged while this [Chat] instance exists.
     */
    val configuration: Configuration

    /**
     * Current collection of global customer [CustomField]s set in SDK.
     * Instance provided by the method won't be updated if the [CustomField]s collection is updated in the [Chat], it should
     * be considered as a snapshot of current state.
     * Values can be updated from backend, after [ChatThreadsHandler.refresh] call.
     */
    val fields: Collection<CustomField>

    /**
     * Current mode of the chat.
     */
    val chatMode: ChatMode
        get() = when {
            configuration.isLiveChat -> ChatMode.LiveChat
            configuration.hasMultipleThreadsPerEndUser -> MultiThread
            else -> SingleThread
        }

    /**
     * Last known channel availability.
     */
    val isChatAvailable: Boolean

    /**
     * Flow of chat state events. Observe this to receive notifications about
     * connection state changes, readiness, and runtime exceptions.
     *
     * New collectors receive the most recent state event upon subscription if one has already
     * been emitted; otherwise, collection suspends until the first event is emitted.
     *
     * @see ChatStateEvent
     */
    val stateFlow: SharedFlow<ChatStateEvent>

    /**
     * Determine if a chat is available.
     *
     * The function may perform a network request if the cached availability status has expired; otherwise,
     * it returns the cached status immediately.
     *
     * @return `true` if the chat channel is available, `false` otherwise.
     *
     * @see [isChatAvailable] for accessing the current availability status directly.
     */
    suspend fun getChannelAvailability(): Boolean

    /**
     * Sets device token (notification push token) to this instance and transmits it
     * to the server. It's not guaranteed that the token is delivered to the server
     * though. Therefore, clients are strongly advised to call this method every time
     * they receive new tokens and reconnect to this instance.
     * */
    fun setDeviceToken(token: String?)

    /**
     * Signs out the user, clears all stored values and closes the connection. Client
     * doesn't need to call [close] explicitly.
     *
     * This instance is considered _dead_ after calling this method.
     */
    suspend fun signOut()

    /**
     * Creates new instance to observe and interact with [Threads][ChatThread].
     * @see ChatThreadsHandler
     * */
    fun threads(): ChatThreadsHandler

    /**
     * Creates new instance to interact with this instance.
     * @see ChatEventHandler
     * */
    fun events(): ChatEventHandler

    /**
     * Creates new instance to modify global custom fields.
     * @see ChatFieldHandler
     * */
    fun customFields(): ChatFieldHandler

    /**
     * Returns new instance of an action handler for this [Chat].
     * @see ChatActionHandler
     * */
    fun actions(): ChatActionHandler

    /**
     * Closes the connection to the chat backend and removes all listeners, even those
     * the client forgot to unregister.
     *
     * Further interaction with any handlers or methods other than [events()] or [connect()] in this class can lead
     * to unspecified, untested and further unwanted behavior.
     *
     * This action will perform a blocking operation and should be called from a background thread.
     * From a coroutine context, prefer [closeSuspending] instead.
     */
    @WorkerThread
    override fun close()

    /**
     * Coroutine-native equivalent of [close] -- suspends until the same cleanup (storage/cookie
     * persistence, socket teardown) completes, without blocking the calling thread. Prefer this
     * over [close] whenever a coroutine context is available.
     */
    suspend fun closeSuspending()

    /**
     * Attempts to connect the chat session.
     *
     * This is a suspend function — call it from a coroutine context.
     * State changes during connection are emitted to [stateFlow].
     *
     * Connection failures (e.g., network unreachable, TLS error, authorization failure) are reported via [stateFlow]
     * and by cancelling this coroutine with [kotlinx.coroutines.CancellationException]; they are
     * not reported as other exception types to callers. Severe errors (e.g., [OutOfMemoryError])
     * propagate normally. After a permanent authorization failure, the session is closed;
     * create a new instance via [ChatBuilder.build] to retry.
     *
     * Post-connection failures (server disconnect after a successful open) are handled
     * internally with automatic reconnection using exponential backoff (up to 20 attempts).
     * If all automatic attempts are exhausted, [ChatStateEvent.UnexpectedDisconnect] is emitted
     * to [stateFlow]. The application may attempt a manual reconnection at that point,
     * but should verify internet connectivity before doing so.
     */
    suspend fun connect()

    /**
     * Attempts to change username if the channel configuration allows setting of the username.
     * All subsequent events sent from chat will have new value filled out.
     */
    fun setUserName(firstName: String, lastName: String)
}
