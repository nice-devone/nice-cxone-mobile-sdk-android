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

import android.content.Context
import com.nice.cxonechat.ChatState.Connected
import com.nice.cxonechat.ChatState.Connecting
import com.nice.cxonechat.ChatState.ConnectionLost
import com.nice.cxonechat.ChatState.Initial
import com.nice.cxonechat.ChatState.Offline
import com.nice.cxonechat.ChatState.Prepared
import com.nice.cxonechat.ChatState.Preparing
import com.nice.cxonechat.ChatState.Ready
import com.nice.cxonechat.exceptions.InvalidStateException
import com.nice.cxonechat.exceptions.RuntimeChatException
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerNoop
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.debug
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.warning
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
 * @param logger The [Logger] used by the SDK, default is no-op implementation.
 * @param customerId Optional customerId of the user.
 * @param chatBuilderProvider **INTERNAL USAGE ONLY** Provides [ChatBuilder].  For internal testing usage only.
 */
@Suppress(
    "TooManyFunctions",
    "LongParameterList"
)
@Public
class ChatInstanceProvider private constructor(
    configuration: SocketFactoryConfiguration?,
    authorization: Authorization?,
    userName: UserName?,
    developmentMode: Boolean,
    deviceTokenProvider: DeviceTokenProvider?,
    logger: Logger,
    customerId: String? = null,
    private val chatBuilderProvider: (Context, SocketFactoryConfiguration, Logger) -> ChatBuilder,
) : ChatStateListener, LoggerScope by LoggerScope(TAG, logger) {
    /** those interested in ChatInstanceProvider updates. */
    @Public
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

        /**
         * Invoked when chat reports runtime exception which was encounter in background thread.
         * @see [ChatStateListener.onChatRuntimeException].
         */
        fun onChatRuntimeException(exception: RuntimeChatException) {}
    }

    /** Defines provider of device token for push notifications, typically this will be `Firebase.messaging.token`. */
    @Public
    fun interface DeviceTokenProvider {
        /**
         * Create or retrieve a device token for push messages, if it is unavailable, the provider should return
         * null. When the token becomes available it should be passed to the chat or builder by invoking the
         * [onComplete] callback.
         */
        fun requestDeviceToken(onComplete: (String) -> Unit)
    }

    /** Configuration scope used to reconfigure and restart the chat session. */
    @Public
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

        /** Current [Logger]. */
        var logger: Logger

        /** Current optional customer id. */
        var customerId: String?
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

    /** Current [Logger]. */
    var logger: Logger = logger
        private set

    override val identity: Logger
        get() = logger

    /** Current optional customerId. */
    var customerId: String? = customerId
        private set

    /** token provided by deviceTokenProvider. */
    private var deviceToken: String? = null
        set(value) {
            field = value
            chat?.setDeviceToken(value)
        }

    /** List of listeners to be notified. */
    private var listeners = listOf<WeakReference<Listener>>()

    /** Current chat object. */
    var chat: Chat? = null
        private set(value) {
            if (field != value) {
                field = value
                eachListener(value, Listener::onChatChanged)
            }
        }

    private data class ChatStateInternal(
        val state: ChatState,
        val cancellable: Cancellable? = null,
    )

    private var state = ChatStateInternal(Initial)

    /** Current chat state. */
    val chatState: ChatState
        get() = state.state

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
        listeners = listeners.filter { it.get() !== listener }
    }

    private fun assertState(state: (ChatState) -> Boolean, generator: () -> String) {
        if (!state(chatState)) {
            throw InvalidStateException(generator())
        }
    }

    private fun assertState(state: ChatState, generator: () -> String) =
        assertState({ it === state }, generator)

    /**
     * Reestablish a chat connection if one does not currently exist.
     * @param context Application context for resource access.
     * @param newConfig Optional configuration which will be used to prepare [Chat] instance. If supplied, it will take precedence over
     * previously set configuration.
     * @throws InvalidStateException if the connection is not in the initial state, i.e.:
     * * it has already been prepared or connected;
     * * the [ChatInstanceProvider] was not provided with a configuration at creation time.
     */
    @Throws(InvalidStateException::class)
    @JvmOverloads
    fun prepare(context: Context, newConfig: SocketFactoryConfiguration? = null) = scope("prepare") {
        if (state.state == Prepared) {
            warning("Ignoring prepare in PREPARED state")
            return@scope
        }

        assertState(Initial) {
            "ChatInstanceProvider.prepare called in an incorrect state ($chatState). " +
                    "It is only valid from the INITIAL state."
        }

        val currentConfig = configuration
        val configuration = newConfig ?: currentConfig ?: throw InvalidStateException(
            "ChatInstanceProvider called with no valid configuration.  Insure the ChatInstanceProvider is " +
                    "properly configured before calling prepare."
        )

        chatBuilderProvider(context, configuration, logger)
            .setChatStateListener(this@ChatInstanceProvider)
            .setDevelopmentMode(developmentMode)
            .apply {
                userName?.run {
                    setUserName(first = firstName, last = lastName)
                }
            }
            .apply {
                authorization?.let(::setAuthorization)
            }
            .apply {
                deviceTokenProvider?.requestDeviceToken { token ->
                    deviceToken = token
                    setDeviceToken(token)
                }
            }
            .apply {
                customerId?.let(::setCustomerId)
            }
            .build { result: Result<Chat> ->
                result.onSuccess { newChat ->
                    chat = newChat
                    advanceState(Prepared)
                    deviceToken?.let { chat?.setDeviceToken(it) }
                }.onFailure {
                    warning("Failed to prepare Chat", it)
                    chat = null
                    advanceState(Initial)
                }
            }
            .also {
                // if build is synchronous, the chat will have already advanced
                // to PREPARED, so just skip PREPARING.
                if (it != Cancellable.noop) {
                    advanceState(Preparing, it)
                }
            }
    }

    /**
     * Connect the chat web socket so chat functions are available.
     * @throws InvalidStateException if the connection is not in the correct state:
     * * it has not been prepared;
     * * it is already connected or connecting.
     */
    @Throws(InvalidStateException::class)
    fun connect() = scope("connect") {
        if (state.state == Connected) {
            warning("Ignoring connect in CONNECTED state")
            return@scope
        }

        assertState({ setOf(Prepared, ConnectionLost).contains(it) }) {
            "ChatInstanceProvider.connect called in invalid state ($chatState). " +
                    "It is only allowed when the connection is either PREPARED, LOST_CONNECTION, or OFFLINE."
        }

        doConnect()
    }

    private fun doConnect() {
        chat?.run {
            val cancellable = connect()

            if (cancellable != Cancellable.noop) {
                // if connect is synchronous skip CONNECTING state
                advanceState(Connecting, cancellable = cancellable, cancel = false)
            }
        }
    }

    /**
     * Reconnect a broken connection.
     * @throws InvalidStateException if the connection was not previously reported as lost
     * or [connect] has already been called since it was reported lost.
     */
    @Throws(InvalidStateException::class)
    @Deprecated(
        "ChatInstanceProvider.reconnect() has been deprecated.  Replace with ChatInstanceProvider.connect()",
        replaceWith = ReplaceWith("connect()")
    )
    fun reconnect() {
        assertState(ConnectionLost) {
            "ChatInstanceProvider.reconnect called in invalid state ($chatState). " +
                    "It is only allowed after when the connection has been closed by the server. "
        }

        doConnect()
    }

    /**
     * Close any connected chat web sockets.
     *
     * After `close()` is called, only usage of [Chat.events] is allowed.
     *
     * The [state] is moved to [Prepared].
     */
    fun close() {
        chat?.close()

        advanceState(Prepared)
    }

    /**
     * Cancel any pending prepare or connect action and return the state
     * to an appropriate starting point.
     */
    fun cancel() {
        when (chatState) {
            Initial -> Unit
            Preparing -> advanceState(Initial)
            Prepared -> Unit
            Connecting -> advanceState(Prepared)
            Connected -> Unit
            ConnectionLost -> advanceState(Prepared)
            Offline -> advanceState(Prepared)
            Ready -> Unit
        }
    }

    /**
     * Sign out/terminate the chat connection and clear any saved credentials.
     */
    fun signOut() {
        synchronized(this) {
            authorization = null
            userName = null

            chat?.signOut()
            chat = null

            advanceState(Initial)
        }
    }

    /**
     * Set custom values on the current chat instance.
     *
     * Note: This routine can be called any time there is a chat object.  If there
     * is no chat object, it will be silently ignored.
     *
     * @param values Custom values to set.
     */
    fun setCustomerValues(values: Map<String, String>) = apply {
        chat?.customFields()?.add(values)
    }

    /**
     * Update the configuration of chat.
     *
     * 1. Stops any current chat.  This will result in discarding any stored [Authorization]
     * or [UserName].  Any such details must be provided in the configuration block once again.
     * 2. Executes the configuration actions block.
     * 3. Restarts chat.
     *
     * @param context Application context for resource access.
     * @param actions Actions to reconfigure the chat.
     */
    fun configure(context: Context, actions: ConfigurationScope.() -> Unit) {
        val provider = this

        val scope = object : ConfigurationScope {
            override val authenticationRequired = provider.chat?.configuration?.isAuthorizationEnabled == true

            override var configuration: SocketFactoryConfiguration?
                get() = provider.configuration
                set(value) {
                    provider.configuration = value
                }

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

            override var logger: Logger
                get() = provider.logger
                set(value) {
                    provider.logger = value
                }

            override var customerId: String?
                get() = provider.customerId
                set(value) {
                    provider.customerId = value
                }
        }

        signOut()

        scope.actions()

        prepare(context)
    }

    @JvmSynthetic
    internal fun advanceState(next: ChatState, cancellable: Cancellable? = null, cancel: Boolean = true) {
        debug("advanceState: $chatState -> $next")
        if (chatState != next) {
            if (cancel) {
                state.cancellable?.cancel()
            }

            if (next in setOf(Preparing, Connecting)) {
                assert(cancellable != null) {
                    "Internal error: advanceState($next) requires a cancellable."
                }
            } else {
                assert(cancellable == null) {
                    "Internal error: advanceState($next) prohibits a cancellable."
                }
            }

            state = ChatStateInternal(next, cancellable)

            eachListener(next, Listener::onChatStateChanged)
        }
    }

    /**
     * Iterate over the list of listeners, forwarding the given
     * action or removing the listener if it's no longer valid.
     *
     * @param T An object which is passed to all listeners as part of an [action].
     * @param actionParameter An object with will be passed to the action.
     * @param action Action to perform on each listener.
     */
    private fun <T> eachListener(actionParameter: T, action: Listener.(T) -> Unit) {
        listeners = listeners.filter { listenerWeakReference ->
            listenerWeakReference.get()?.also { listener -> listener.action(actionParameter) } != null
        }
    }

    //
    // ChatStateListener Implementation
    //

    override fun onConnected() {
        advanceState(Connected)
    }

    override fun onReady() {
        if (requireNotNull(chat).isChatAvailable) {
            advanceState(Ready)
        } else {
            advanceState(Offline)
        }
    }

    override fun onUnexpectedDisconnect() {
        advanceState(ConnectionLost)
    }

    override fun onChatRuntimeException(exception: RuntimeChatException) {
        eachListener(exception, Listener::onChatRuntimeException)
    }

    /**
     * Sets [UserName] which will be used during creation of [Chat] instance
     * and will apply it to current instance of [Chat], if it exists.
     * The username will be applied only if the chat channel configuration allows it.
     *
     * @param name A username which should be set.
     * @see [Chat.setUserName].
     */
    fun setUserName(name: UserName) {
        userName = name
        chat?.setUserName(name.firstName, name.lastName)
    }

    @Public
    companion object {
        private const val TAG = "ChatInstanceProvider"

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
         * @param logger [Logger] to be used by the ChatInstanceProvider and Chat.
         * @param customerId Optional, customerId of the user.
         * @return the newly created ChatInstanceProvider singleton.
         */
        @Suppress(
            "LongParameterList" // Most of the parameters have default values provided.
        )
        @JvmOverloads
        fun create(
            configuration: SocketFactoryConfiguration?,
            authorization: Authorization? = null,
            userName: UserName? = null,
            developmentMode: Boolean = false,
            deviceTokenProvider: DeviceTokenProvider? = null,
            logger: Logger = LoggerNoop,
            customerId: String? = null,
        ) = create(
            configuration,
            authorization,
            userName,
            developmentMode,
            deviceTokenProvider,
            logger,
            customerId,
            ChatBuilder.Companion::invoke,
        )

        /**
         * Create the ChatInstanceProvider singleton.
         *
         * @param configuration Initial Sdk Configuration to use.
         * @param authorization Initial authorization to use.
         * @param userName Initial user name to use.
         * @param developmentMode True if in development mode to get extra logging.
         * @param deviceTokenProvider Provider of device tokens for push messages.
         * @param logger [Logger] to be used by the ChatInstanceProvider and Chat.
         * @param customerId Optional customerId of the user.
         * @param chatBuilderProvider **INTERNAL USAGE ONLY** Provides [ChatBuilder].  For internal testing usage only.
         * @return the newly created ChatInstanceProvider singleton.
         */
        @Suppress("LongParameterList")
        @JvmSynthetic
        internal fun create(
            configuration: SocketFactoryConfiguration?,
            authorization: Authorization? = null,
            userName: UserName? = null,
            developmentMode: Boolean = false,
            deviceTokenProvider: DeviceTokenProvider? = null,
            logger: Logger = LoggerNoop,
            customerId: String? = null,
            chatBuilderProvider: (Context, SocketFactoryConfiguration, Logger) -> ChatBuilder,
        ) = ChatInstanceProvider(
            configuration,
            authorization,
            userName,
            developmentMode,
            deviceTokenProvider,
            logger,
            customerId,
            chatBuilderProvider,
        ).also {
            instance = it
        }
    }
}
