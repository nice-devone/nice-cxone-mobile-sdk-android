/*
 * Copyright (c) 2021-2024. NICE Ltd. All rights reserved.
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

import com.nice.cxonechat.ChatMode.MultiThread
import com.nice.cxonechat.ChatMode.SingleThread
import com.nice.cxonechat.state.Configuration
import com.nice.cxonechat.state.Environment
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.CustomField

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
     * Determine if a chat is available.
     */
    fun getChannelAvailability(callback: (Boolean) -> Unit): Cancellable

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
    fun signOut()

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
     */
    override fun close()

    /**
     * Attempts to restart the chat session using existing configuration, the attempt will be made on background thread.
     * Successful connection will be announced to the [ChatStateListener.onConnected] which was
     * supplied in [ChatBuilder].
     * If there is an issue during/after reconnection the [ChatStateListener.onUnexpectedDisconnect] will be called,
     * it is responsibility of application to perform a repeated reconnection attempt, if it is desirable.
     * Reconnect attempts should be performed only if the device is connected to the internet.
     * If the repeated reconnection attempts are made, they should be called with exponential backoff,
     * in order to prevent backend overload.
     *
     * @return An instance of [Cancellable] which can be used to interrupt background operation.
     */
    @Deprecated("Deprecated, use connect() instead.", replaceWith = ReplaceWith("connect()"))
    fun reconnect(): Cancellable

    /**
     * Attempts to connect the chat session using existing configuration, the attempt will be made on background thread.
     * Successful connection will be announced to the [ChatStateListener.onConnected] which was
     * supplied in [ChatBuilder].
     * If there is an issue during/after reconnection the [ChatStateListener.onUnexpectedDisconnect] will be called,
     * it is responsibility of application to perform a repeated reconnection attempt, if it is desirable.
     * Reconnect attempts should be performed only if the device is connected to the internet.
     * If the repeated reconnection attempts are made, they should be called with exponential backoff,
     * in order to prevent backend overload.
     *
     * @return An instance of [Cancellable] which can be used to interrupt background operation.
     */
    fun connect(): Cancellable

    /**
     * Attempts to change username if the channel configuration allows setting of the username.
     * All subsequent events sent from chat will have new value filled out.
     */
    fun setUserName(firstName: String, lastName: String)
}
