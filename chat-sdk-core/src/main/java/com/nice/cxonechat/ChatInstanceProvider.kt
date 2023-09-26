/*
 * Copyright (c) 2021-2023. NICE Ltd. All rights reserved.
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

import android.content.Context
import com.nice.cxonechat.ChatState.CONNECTED
import com.nice.cxonechat.ChatState.CONNECTING
import com.nice.cxonechat.ChatState.CONNECTION_CLOSED
import com.nice.cxonechat.ChatState.CONNECTION_LOST
import com.nice.cxonechat.ChatState.INITIAL
import com.nice.cxonechat.state.lookup
import com.nice.cxonechat.state.validate
import java.lang.ref.WeakReference

/**
 * ChatRepository owns and maintains the chat object and its state.
 *
 * @param configuration Initial Sdk Configuration to use.
 * @param authorization Initial authorization to use.
 * @param userName Initial user name to use.
 * @param developmentMode True if in development mode to get extra logging.
 * @param deviceTokenProvider Provider of device tokens for push messages, default implementation will
 * disable push notifications.
 */
@Suppress("TooManyFunctions")
@Public
class ChatInstanceProvider(
    configuration: SocketFactoryConfiguration?,
    authorization: Authorization? = null,
    userName: UserName? = null,
    developmentMode: Boolean = false,
    deviceTokenProvider: DeviceTokenProvider? = null,
) : ChatStateListener {
    /** those interested in ChatInstanceProvider updates. */
    interface Listener {
        /**
         * Invoked when the chat object changes.
         *
         * @param chat New Chat object if any or null if none currently exists.
         */
        fun onChatChanged(chat: Chat?) {}

        /**
         * Invoked when the chat state changes.
         *
         * @param chatState New chat state.
         */
        fun onChatStateChanged(chatState: ChatState) {}
    }

    /** Defines provider of device token for push notifications, typically this will [Firebase.messaging.token]. */
    fun interface DeviceTokenProvider {
        /**
         * Create or retrieve a device token for push messages, if it is unavailable, the provider should return
         * null. When the token becomes available it should be passed to the chat or builder by invoking the
         * [onComplete] callback.
         */
        fun requestDeviceToken(onComplete: (String) -> Unit)
    }

    /** Configuration scope used to reconfigure and restart the chat session. */
    interface ConfigurationScope {
        /** True if authorization is required. */
        val authenticationRequired: Boolean

        /** New/current configuration to use. */
        var configuration: SocketFactoryConfiguration?

        /** New/current userName to use. */
        var userName: UserName?

        /** New/current authorization to use. */
        var authorization: Authorization?

        /** Current developmentMode state.  If true, additional debugging is provided */
        var developmentMode: Boolean

        /** Current deviceToken provider. */
        var deviceTokenProvider: DeviceTokenProvider?
    }

    /** Current configuration. */
    var configuration: SocketFactoryConfiguration? = configuration
        private set

    /** Current user name. */
    var userName: UserName? = userName
        private set

    /** Current authorization. */
    var authorization: Authorization? = authorization
        private set

    /** Current developmentMode state.  If true, additional debugging is provided */
    var developmentMode: Boolean = developmentMode
        private set

    /** Current deviceToken provider. */
    var deviceTokenProvider: DeviceTokenProvider? = deviceTokenProvider
        private set

    /** token provided by deviceTokenProvider. */
    private var deviceToken: String? = null
        set(value) {
            field = value
            chat?.setDeviceToken(value)
        }

    /** Cancellable creating chat instance. */
    private var createJob: Cancellable? = null

    /** Cancellable establishing new chat connection. */
    private var reconnectJob: Cancellable? = null

    /** List of listeners to be notified. */
    private var listeners = listOf<WeakReference<Listener>>()

    /** Current chat object. */
    var chat: Chat? = null
        private set(value) {
            if (field != value) {
                field = value
                eachListener { onChatChanged(field) }
            }
        }

    /** Current chat state. */
    var chatState: ChatState = INITIAL
        private set(value) {
            if (field != value) {
                field = value
                eachListener { onChatStateChanged(value) }
            }
        }

    /**
     * Add a listener to receive notifications of chat and state changes.
     *
     * **Note:** `listener` will be maintained via a [WeakReference] and so will not
     * be protected from collection.
     *
     * @param listener Listener to add.
     */
    fun addListener(listener: Listener) {
        listeners = listeners + WeakReference(listener)
    }

    /**
     * Remove a listener no longer concerned with chat and state changes.
     *
     * @param listener Listener to remove.
     */
    fun removeListener(listener: Listener) {
        listeners = listeners.filter { it !== listener }
    }

    /**
     * Reestablish a chat connection if one does not currently exist.
     * @param context Application context for resource access.
     */
    fun start(context: Context) {
        if (setOf(CONNECTED, CONNECTING).contains(chatState)) {
            return
        }

        configuration?.let { configuration ->
            chatState = CONNECTING

            createJob = ChatBuilder(context = context, config = configuration).apply {
                userName?.run {
                    setUserName(first = firstName, last = lastName)
                }
                authorization?.let(::setAuthorization)
                setDevelopmentMode(developmentMode)
                setChatStateListener(this@ChatInstanceProvider)
                deviceTokenProvider?.requestDeviceToken { token ->
                    deviceToken = token
                    setDeviceToken(token)
                }
            }.build { result ->
                chat = result
                deviceToken?.let { chat?.setDeviceToken(it) }
            }
        }
    }

    /**
     * Cancel a pending start request.
     */
    fun cancelStart() {
        createJob?.cancel()
        createJob = null

        chatState = INITIAL
    }

    /**
     * Reconnect a broken connection.
     */
    fun reconnect() {
        reconnectJob = chat?.reconnect()?.also {
            chatState = CONNECTING
        }
    }

    /**
     * Stop any existing chat attempts and reset the state to CONNECTION_CLOSED.
     */
    fun stop() {
        reconnectJob?.cancel()
        reconnectJob = null

        createJob?.cancel()
        createJob = null

        chat?.close()
        chat = null
        chatState = CONNECTION_CLOSED
    }

    /**
     * Sign out/terminate the chat connection and clear any saved credentials.
     */
    fun signOut() {
        synchronized(this) {
            authorization = null
            userName = null

            chatState = CONNECTION_CLOSED
            chat?.signOut()
            chat = null
        }
    }

    /**
     * Set custom values on the current chat instance.
     *
     * Note: This routine can be called any time there is a chat object.  If there
     * is no chat object, it will be silently ignored.
     *
     * @param values Custom values to set.  These will be filtered by the chat's
     * configured available customerCustomFields.
     */
    fun setCustomerValues(values: Map<String, String>) = apply {
        chat?.run {
            val customerCustomFields = configuration.customerCustomFields
            val fields = values.filter {
                customerCustomFields.lookup(it.key) != null
            }

            runCatching { customerCustomFields.validate(fields) }
                .onSuccess {
                    customFields().add(fields)
                }
        }
    }

    /**
     * Update the configuration of chat.
     *
     * 1. Stops any current chat.
     * 2. Executes the configuration actions block.
     * 3. Restarts chat.
     *
     * @param context Application context for resource access.
     * @param actions Actions to reconfigure the chat.
     */
    fun configure(context: Context, actions: ConfigurationScope.() -> Unit) {
        val provider = this

        chat?.signOut()
        chat = null

        object : ConfigurationScope {
            override val authenticationRequired: Boolean
                get() = provider.chat?.configuration?.isAuthorizationEnabled == true

            override var configuration: SocketFactoryConfiguration?
                get() = provider.configuration
                set(value) { provider.configuration = value }

            override var userName: UserName?
                get() = provider.userName
                set(value) {
                    provider.userName = value
                }

            override var authorization: Authorization?
                get() = provider.authorization
                set(value) {
                    provider.authorization = value
                }

            override var developmentMode: Boolean
                get() = provider.developmentMode
                set(value) {
                    provider.developmentMode = value
                }

            override var deviceTokenProvider: DeviceTokenProvider?
                get() = provider.deviceTokenProvider
                set(value) {
                    provider.deviceTokenProvider = value
                }
        }.actions()

        restart(context)
    }

    /**
     * Iterate over the list of listeners, forwarding the given
     * action or removing the listener if it's no longer valid.
     *
     * @param action Action to perform on each listener.
     */
    private fun eachListener(action: Listener.() -> Unit) {
        listeners = listeners.filter {
            it.get()?.run {
                action()
                true
            } ?: false
        }
    }

    /**
     * Stop any current connection under way and start a new connection attempt.
     *
     * @param context Application context for resource access.
     */
    private fun restart(context: Context) {
        stop()
        start(context)
    }

    //
    // ChatStateListener Implementation
    //

    override fun onConnected() {
        chatState = CONNECTED
    }

    override fun onUnexpectedDisconnect() {
        chatState = CONNECTION_LOST
    }

    companion object {
        @Suppress("LateinitUsage")
        private lateinit var instance: ChatInstanceProvider

        /** Fetch the previously create ChatInstanceProvider singleton. */
        fun get() = instance

        /**
         * Create the ChatInstanceProvider singleton.
         *
         * @param configuration Initial Sdk Configuration to use.
         * @param authorization Initial authorization to use.
         * @param userName Initial user name to use.
         * @param developmentMode True if in development mode to get extra logging.
         * @param deviceTokenProvider Provider of device tokens for push messages.
         * @return the newly created ChatInstanceProvider singleton.
         */
        fun create(
            configuration: SocketFactoryConfiguration?,
            authorization: Authorization? = null,
            userName: UserName? = null,
            developmentMode: Boolean = false,
            deviceTokenProvider: DeviceTokenProvider? = null,
        ) = ChatInstanceProvider(
            configuration,
            authorization,
            userName,
            developmentMode,
            deviceTokenProvider,
        ).also {
            instance = it
        }
    }
}
