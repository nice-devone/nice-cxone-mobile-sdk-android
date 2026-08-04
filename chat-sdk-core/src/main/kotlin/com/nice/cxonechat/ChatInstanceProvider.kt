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

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.annotation.WorkerThread
import com.nice.cxonechat.Cancellable.Companion.asCancellable
import com.nice.cxonechat.ChatState.Connected
import com.nice.cxonechat.ChatState.Connecting
import com.nice.cxonechat.ChatState.ConnectionLost
import com.nice.cxonechat.ChatState.Initial
import com.nice.cxonechat.ChatState.Offline
import com.nice.cxonechat.ChatState.Prepared
import com.nice.cxonechat.ChatState.Preparing
import com.nice.cxonechat.ChatState.Ready
import com.nice.cxonechat.ChatState.SdkNotSupported
import com.nice.cxonechat.exceptions.InvalidStateException
import com.nice.cxonechat.exceptions.RuntimeChatException
import com.nice.cxonechat.exceptions.SdkVersionNotSupported
import com.nice.cxonechat.internal.ChannelAvailabilityFailedException
import com.nice.cxonechat.internal.ChannelConfigurationCache
import com.nice.cxonechat.internal.ChatBuilderInternal
import com.nice.cxonechat.internal.PermanentConnectionFailureException
import com.nice.cxonechat.internal.PreSocketReconnectScheduler
import com.nice.cxonechat.internal.TransientConnectFailureException
import com.nice.cxonechat.internal.model.network.DeviceFingerprint
import com.nice.cxonechat.internal.model.network.description
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerNoop
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.debug
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.warning
import com.nice.cxonechat.logger.RemoteLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.ref.WeakReference
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

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
 * @param dispatcher Coroutine dispatcher used for background operations, defaults to [Dispatchers.IO].
 */
// LargeClass: tracked in DE-173313; don't remove without closing it. Not baselined — a single
// deliberate exception, not a bulk grandfather from adopting a new rule.
@Suppress(
    "TooManyFunctions",
    "LongParameterList",
    "LargeClass",
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
    private val chatBuilderProvider: suspend (Context, SocketFactoryConfiguration, Logger) -> ChatBuilder,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ChatStateListener, LoggerScope by LoggerScope(TAG, logger) {

    private var coroutineScope = CoroutineScope(dispatcher + SupervisorJob())

    /**
     * Schedules a backoff-limited auto-retry of [connect] after a pre-socket REST connect failure.
     * Owns its own coroutine scope — see [PreSocketReconnectScheduler]'s KDoc for why it must not
     * share [coroutineScope], which [signOut] reassigns.
     */
    @VisibleForTesting
    internal val preSocketReconnectScheduler = PreSocketReconnectScheduler(
        dispatcher = dispatcher,
        loggerScope = this,
        connect = ::connect,
        onExhausted = ::onUnexpectedDisconnect,
    )

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

    /** Authentication scope for configuring identity and token-related settings. */
    @Public
    interface AuthenticationScope {
        /** True if authorization is required. */
        val authenticationRequired: Boolean

        /** New/current userName to use. */
        var userName: UserName?

        /** New/current authorization to use. */
        var authorization: Authorization?

        /** Current optional customer id. */
        var customerId: String?

        /** Current optional token delegate listener for implicit OAuth flow. */
        var tokenDelegateListener: TokenDelegateListener?
    }

    /** Configuration scope used to reconfigure and restart the chat session. */
    @Public
    interface ConfigurationScope : AuthenticationScope {
        /** New/current configuration to use. */
        var configuration: SocketFactoryConfiguration?

        /** Current developmentMode state.  If true, additional debugging is provided */
        var developmentMode: Boolean

        /** Current deviceToken provider. */
        var deviceTokenProvider: DeviceTokenProvider?

        /** Current [Logger]. */
        var logger: Logger
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

    /** Current optional token delegate listener for implicit OAuth flow. */
    var tokenDelegateListener: TokenDelegateListener? = null
        private set

    /** token provided by deviceTokenProvider. */
    private var deviceToken: String? = null
        set(value) {
            field = value
            chat?.setDeviceToken(value)
        }

    /** List of listeners to be notified. */
    private var listeners = listOf<WeakReference<Listener>>()

    /**
     * Application context captured at [prepare] — required for per-session Chat rebuilds.
     * Held via a [WeakReference] only, so the provider never keeps a [Context] alive on its own.
     * The application context is a process-lifetime singleton, so this resolves for as long as the
     * app is running; a cleared reference means [prepare] was never called (or the app is gone).
     */
    private var appContext: WeakReference<Context>? = null

    /** Configuration the current Chat instance was prepared with — rebuilds must reuse it. */
    private var preparedConfiguration: SocketFactoryConfiguration? = null

    /**
     * Set by [close]: the session ended, so the next [connect] must rebuild the Chat instance —
     * closing cancels the session-scoped coroutines inside the instance's handler graph, and the
     * cached handlers cannot be revived. ConnectionLost/Offline reconnects never set this flag
     * (the instance was not closed, autonomous reconnect keeps it).
     */
    @Volatile
    private var chatNeedsRebuild = false

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

    // Lock to prevent concurrent write to the state.
    private val stateSyncLock: ReadWriteLock = ReentrantReadWriteLock()

    /** Current chat state. */
    val chatState: ChatState
        get() = stateSyncLock.readLock().withLock {
            state.state
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
        listeners = listeners.filter { it.get() !== listener }
    }

    private fun assertState(state: (ChatState) -> Boolean, generator: () -> String) {
        if (!state(chatState)) {
            throw InvalidStateException(generator())
        }
    }

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
        if (chatState === Prepared) {
            warning("Ignoring prepare in PREPARED state")
            return@scope
        }

        assertState({ it === Initial }) {
            "ChatInstanceProvider.prepare called in an incorrect state ($chatState). " +
                    "It is only valid from the INITIAL state."
        }

        val currentConfig = configuration
        val configuration = newConfig ?: currentConfig ?: throw InvalidStateException(
            "ChatInstanceProvider called with no valid configuration.  Insure the ChatInstanceProvider is " +
                    "properly configured before calling prepare."
        )
        appContext = WeakReference(context.applicationContext)
        preparedConfiguration = configuration
        chatNeedsRebuild = false

        // LAZY start: same ordering fix as doConnect() — ensures advanceState(Preparing) runs before the body.
        val job = coroutineScope.launch(start = CoroutineStart.LAZY) {
            try {
                val newChat = buildChat(context, configuration, preferCachedConfiguration = false)
                chat = newChat
                advanceState(Prepared)
                deviceToken?.let { chat?.setDeviceToken(it) }
                logRemoteChat(configuration, newChat)
            } catch (throwable: SdkVersionNotSupported) {
                advanceState(SdkNotSupported)
                warning("Failed to prepare Chat", throwable)
                chat = null
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (expected: Exception) {
                advanceState(Initial)
                warning("Failed to prepare Chat", expected)
                chat = null
            }
        }
        advanceState(Preparing, job.asCancellable())
        job.start()
    }

    /**
     * Builds a new [Chat] instance, replaying the provider's credentials and settings into the
     * builder. Used both by [prepare] (fresh configuration fetch) and by per-session rebuilds in
     * [doConnect] (which reuse the configuration fetched at prepare time via
     * [ChannelConfigurationCache] — the getChannel REST call is costly and its result is fixed for
     * the lifetime of a prepared provider).
     */
    @Suppress("DEPRECATION")
    private suspend fun buildChat(
        context: Context,
        configuration: SocketFactoryConfiguration,
        preferCachedConfiguration: Boolean,
    ): Chat {
        val builder = chatBuilderProvider(context, configuration, logger)
            .setChatStateListener(this)
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
                customerId?.let(::setCustomerId)
                tokenDelegateListener?.let(::setTokenDelegateListener)
            }
        if (preferCachedConfiguration) {
            (builder as? ChatBuilderInternal)?.setPreferCachedConfiguration(true)
        }
        return builder.build()
    }

    /**
     * Connect the chat web socket so chat functions are available.
     * @throws InvalidStateException if the connection is not in the correct state:
     * * it has not been prepared;
     * * it is already connected or connecting.
     */
    @Throws(InvalidStateException::class)
    fun connect() = scope("connect") {
        if (chatState === Connected) {
            warning("Ignoring connect in CONNECTED state")
            return@scope
        }

        /*
         * Offline state is a special case where we allow doConnect to be called since
         * the live chat implementation of Chat will re-check the availability of the chat,
         * if the cached availability information is expired and will fetch a fresh version,
         * if it is required.
         * In case that the availability information will allow it the connection attempt will be made.
         *
         * Independent on the state of the availability information the instance provider
         *  will be notified via the onReady callback once the procedure is finished.
         */
        assertState({ setOf(Prepared, ConnectionLost, Offline).contains(it) }) {
            "ChatInstanceProvider.connect called in invalid state ($chatState). " +
                    "It is only allowed when the connection is either PREPARED, LOST_CONNECTION, or OFFLINE."
        }

        doConnect()
    }

    private fun logRemoteChat(configuration: SocketFactoryConfiguration, chat: Chat) {
        RemoteLogger.setData(
            brandId = configuration.brandId,
            loggerUrl = chat.environment.loggerUrl,
            chatUrl = chat.environment.chatUrl,
            deviceFingerprint = DeviceFingerprint(deviceToken = deviceToken).description()
        )
    }

    private fun doConnect() {
        val currentChat = chat ?: return
        // LAZY start ensures advanceState(Connecting) runs before the coroutine body executes.
        // Without this, connect() can fail and trigger onConnectLost() while chatState is still
        // Prepared, causing the guard `if (chatState === Connecting)` to miss; the subsequent
        // advanceState(Connecting) call then overwrites ConnectionLost, leaving the provider stuck.
        val job = coroutineScope.launch(start = CoroutineStart.LAZY) { attemptConnect(currentChat) }
        if (tryAdvanceToConnecting(job.asCancellable())) {
            job.start()
        } else {
            // Lost the race into Connecting (e.g. the pre-socket auto-retry scheduler beat this
            // caller to it) — drop this job unstarted; cancel()/close() can't reach it once running.
            debug("connect() lost the race into Connecting; another connect attempt is already in progress")
            job.cancel()
        }
    }

    /**
     * Atomically transitions to [Connecting] only if not already there, returning whether this call
     * performed the transition. `connect()`'s state guard alone is not enough to prevent two
     * concurrent callers (e.g. the UI and the pre-socket auto-retry scheduler) from both reaching
     * [doConnect] — this makes the actual state transition the single point of arbitration, so only
     * the winner's job starts and becomes the tracked [Cancellable].
     */
    private fun tryAdvanceToConnecting(cancellable: Cancellable): Boolean =
        stateSyncLock.writeLock().withLock {
            if (chatState === Connecting) {
                false
            } else {
                advanceState(Connecting, cancellable = cancellable, cancel = false)
                true
            }
        }

    /**
     * Runs a single connect attempt (rebuilding the [Chat] instance first if needed) and routes every
     * failure mode to the appropriate state transition. Extracted out of [doConnect] to keep that
     * function's own cognitive complexity under the detekt threshold — this class is already at the
     * LargeClass limit (see the NOTE below), so new failure handling must live in its own function.
     */
    private suspend fun attemptConnect(currentChat: Chat) {
        try {
            val target = if (chatNeedsRebuild) rebuildChat() else currentChat
            target.connect()
        } catch (throwable: SdkVersionNotSupported) {
            // Rebuild refused by the backend — mirror prepare()'s handling.
            warning("Failed to rebuild Chat", throwable)
            chat = null
            advanceState(SdkNotSupported)
        } catch (e: PermanentConnectionFailureException) {
            // Non-retriable credential failure (e.g. ConsumerReconnectionFailed from server,
            // or expired ThirdPartyOAuth token from ChatImpl). Caught explicitly before
            // CancellationException (extends it) — do NOT rethrow; state machine transitions
            // to ConnectionLost.
            onConnectLost(e)
        } catch (e: TransientConnectFailureException) {
            // Transient failure (socket-open error or token API outage).
            // Provider transitions to ConnectionLost; connect() may be called again to retry.
            onConnectLost(e)
        } catch (e: CancellationException) {
            // Mid-auth socket drop (e.g. from ChatAuthorization) or scope shutdown.
            onConnectLost(e)
            throw e
        } catch (e: ChannelAvailabilityFailedException) {
            // Pre-socket failure (e.g. offline) — no socket ever opened, so ReconnectingListener
            // never sees it; schedule an auto-retry instead of relying on the UI. Only if this
            // attempt is still current: the pre-socket REST call is a blocking, non-cancellable
            // execute(), so it can fail *after* the caller already cancelled (state moved off
            // Connecting) — scheduling a retry in that case would silently reconnect a session the
            // caller explicitly gave up on.
            if (onConnectLost(e)) {
                preSocketReconnectScheduler.onPreSocketConnectFailure(e)
            }
        } catch (expected: Exception) {
            // Rebuild failure (e.g. transient network error while re-creating the session's Chat
            // instance). ConnectionLost lets the UI retry via connect(), which attempts the
            // rebuild again — the rebuild flag is cleared only on success.
            // NOTE (DE-172157 review E3): this generic catch currently also covers
            // currentChat.connect() on the non-rebuild path. Scoping it to rebuildChat() only is
            // deferred together with the ChatInstanceProvider extraction (LargeClass / review #8),
            // since scoping it here pushes this already-at-limit class over the detekt threshold.
            onConnectLost(expected)
        }
    }

    /**
     * Rebuilds the [Chat] instance for a new session after [close] ended the previous one.
     * Reuses the channel configuration fetched at [prepare] time (no getChannel REST call) and
     * fires [Listener.onChatChanged] with the fresh instance before connecting it.
     */
    private suspend fun rebuildChat(): Chat {
        val context = checkNotNull(appContext?.get()) {
            "Chat rebuild requested without a prior prepare() — application context is unknown."
        }
        val configuration = checkNotNull(preparedConfiguration) {
            "Chat rebuild requested without a configuration."
        }
        debug("Rebuilding Chat instance for a new session")
        val newChat = buildChat(context, configuration, preferCachedConfiguration = true)
        chatNeedsRebuild = false
        chat = newChat
        deviceToken?.let { newChat.setDeviceToken(it) }
        logRemoteChat(configuration, newChat)
        return newChat
    }

    /**
     * Transitions to [ConnectionLost] if still [Connecting], returning whether it did. The return
     * value lets [attemptConnect]'s [ChannelAvailabilityFailedException] catch distinguish "this
     * failure is for the still-current connect attempt" from "the caller already moved on (e.g.
     * `cancel()`) before this pre-socket failure landed" — the latter must not schedule a retry.
     */
    private fun onConnectLost(cause: Exception): Boolean {
        debug("Connection lost while connecting", cause)
        return if (chatState === Connecting) {
            advanceState(ConnectionLost)
            true
        } else {
            false
        }
    }

    /**
     * Close any connected chat web sockets.
     *
     * After `close()` is called, only usage of [Chat.events] is allowed.
     *
     * The [state] is moved to [Prepared]. The session has ended — the next [connect] call
     * transparently rebuilds the [Chat] instance (notifying [Listener.onChatChanged]), because the
     * session-scoped handler graph of a closed instance cannot be revived.
     */
    @WorkerThread
    fun close(): Unit = runBlocking { closeSuspending() }

    /**
     * Coroutine-native equivalent of [close] -- prefer this from a coroutine context.
     */
    suspend fun closeSuspending() = scope("closeSuspending") {
        preSocketReconnectScheduler.reset()
        chat?.also {
            it.closeSuspending()
            chatNeedsRebuild = true
        }

        advanceState(Prepared)
    }

    /**
     * Cancel any pending prepare or connect action and return the state
     * to an appropriate starting point.
     */
    fun cancel() = scope("cancel") {
        preSocketReconnectScheduler.reset()
        when (chatState) {
            Initial -> Unit
            Preparing -> advanceState(Initial)
            Prepared -> Unit
            Connecting -> advanceState(Prepared)
            Connected -> Unit
            ConnectionLost -> advanceState(Prepared)
            Offline -> advanceState(Prepared)
            Ready -> Unit
            SdkNotSupported -> Unit
        }
    }

    /**
     * Sign out/terminate the chat connection and clear any saved credentials.
     */
    suspend fun signOut() = scope("signOut") {
        val chatToSignOut = synchronized(this) {
            preSocketReconnectScheduler.reset()
            coroutineScope.cancel("Signed out")
            coroutineScope = CoroutineScope(dispatcher + SupervisorJob())
            authorization = null
            userName = null
            customerId = null
            chatNeedsRebuild = false
            preparedConfiguration = null
            ChannelConfigurationCache.clear()
            chat.also { chat = null }
        }
        // chat.signOut() suspends (persists storage/cookies) -- must run outside synchronized(),
        // since the compiler forbids suspending while holding a JVM monitor. `chat` was already
        // nulled above, so advanceState(Initial) must run in finally: a failure/cancellation here
        // must not leave chat=null paired with a stale, non-Initial chatState forever.
        try {
            chatToSignOut?.signOut()
        } finally {
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
    suspend fun configure(context: Context, actions: ConfigurationScope.() -> Unit) {
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

            override var tokenDelegateListener: TokenDelegateListener?
                get() = provider.tokenDelegateListener
                set(value) {
                    provider.tokenDelegateListener = value
                }
        }

        signOut()

        scope.actions()

        prepare(context)
    }

    @JvmSynthetic
    internal fun advanceState(next: ChatState, cancellable: Cancellable? = null, cancel: Boolean = true) {
        stateSyncLock.writeLock().withLock {
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

    override fun onConnected() = scope("onConnected") {
        preSocketReconnectScheduler.reset()
        if (chatState === Connecting) { // if not in Connecting means it was cancelled. no need to update in that case
            // cancel=false: the connect job (registered as Connecting's cancellable) continues running
            // after the state advances to Connected, awaiting recovery before calling onReady().
            advanceState(Connected, cancel = false)
        } else {
            debug("Ignoring onConnected in $chatState state")
        }
    }

    override fun onReady() = scope("onReady") {
        preSocketReconnectScheduler.reset()
        if (requireNotNull(chat).isChatAvailable) {
            advanceState(Ready)
        } else {
            advanceState(Offline)
        }
    }

    override fun onUnexpectedDisconnect() = scope("onUnexpectedDisconnect") {
        advanceState(ConnectionLost)
    }

    override fun onConnecting() = scope("onConnecting") {
        advanceState(Connecting, Cancellable.noop, false)
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

    /**
     * Sets or clears the [TokenDelegateListener] for the implicit OAuth flow.
     *
     * The listener is applied on the next [prepare] call. To update a running session,
     * use [configure] which will restart the session with the new listener in place.
     *
     * The listener's [TokenDelegateListener.onNewTokenRequested] is invoked on a background
     * thread and must block until the token is ready or the attempt fails. Throwing from the
     * listener causes the SDK to report
     * [com.nice.cxonechat.exceptions.RuntimeChatException.TokenDelegationFailedException].
     *
     * @param listener The listener to register, or `null` to clear it.
     */
    fun setTokenDelegateListener(listener: TokenDelegateListener?) {
        tokenDelegateListener = listener
    }

    @Public
    @Suppress(
        "UndocumentedPublicClass", // Companion objects don't require documentation.
    )
    companion object {
        private const val TAG = "ChatInstanceProvider"

        private val readWriteLock: ReadWriteLock = ReentrantReadWriteLock()

        private var instance: ChatInstanceProvider? = null

        /**
         *  Fetch the previously created ChatInstanceProvider singleton.
         *
         *  @throws IllegalStateException if the ChatInstanceProvider has not been created yet.
         */
        fun get(): ChatInstanceProvider = readWriteLock.readLock().withLock {
            checkNotNull(instance) {
                "ChatInstanceProvider has not been created yet.  Call ChatInstanceProvider.create() first."
            }
        }

        /**
         * Create the ChatInstanceProvider singleton.
         * If the ChatInstanceProvider has already been created,
         * this will replace current singleton instance with a new one.
         *
         * Old instance is considered invalid and should not be used anymore, [Chat] instance won't be affected,
         * but should be closed before calling this method to avoid unexpected behavior.
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
            configuration = configuration,
            authorization = authorization,
            userName = userName,
            developmentMode = developmentMode,
            deviceTokenProvider = deviceTokenProvider,
            logger = logger,
            customerId = customerId,
            chatBuilderProvider = { context, config, log -> ChatBuilder(context, config, log) },
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
         * @param dispatcher Coroutine dispatcher for background operations, defaults to [Dispatchers.IO].
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
            chatBuilderProvider: suspend (Context, SocketFactoryConfiguration, Logger) -> ChatBuilder,
            dispatcher: CoroutineDispatcher = Dispatchers.IO,
        ) = ChatInstanceProvider(
            configuration,
            authorization,
            userName,
            developmentMode,
            deviceTokenProvider,
            logger,
            customerId,
            chatBuilderProvider,
            dispatcher,
        ).also {
            readWriteLock.writeLock().withLock { instance = it }
        }
    }
}
