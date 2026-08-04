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

package com.nice.cxonechat.internal

import androidx.annotation.VisibleForTesting
import com.nice.cxonechat.Authorization
import com.nice.cxonechat.Chat
import com.nice.cxonechat.ChatActionHandler
import com.nice.cxonechat.ChatEventHandler
import com.nice.cxonechat.ChatFieldHandler
import com.nice.cxonechat.ChatMode.LiveChat
import com.nice.cxonechat.ChatMode.MultiThread
import com.nice.cxonechat.ChatMode.SingleThread
import com.nice.cxonechat.ChatStateEvent
import com.nice.cxonechat.ChatStateEvent.Connected
import com.nice.cxonechat.ChatStateEvent.Connecting
import com.nice.cxonechat.ChatStateEvent.Ready
import com.nice.cxonechat.ChatStateEvent.RuntimeException
import com.nice.cxonechat.ChatStateEvent.UnexpectedDisconnect
import com.nice.cxonechat.ChatStateListener
import com.nice.cxonechat.ChatThreadsHandler
import com.nice.cxonechat.TokenDelegateListener
import com.nice.cxonechat.TokenRequestReason
import com.nice.cxonechat.enums.AuthenticationType
import com.nice.cxonechat.event.PageViewEvent
import com.nice.cxonechat.exceptions.RuntimeChatException
import com.nice.cxonechat.exceptions.RuntimeChatException.TokenDelegationFailedException
import com.nice.cxonechat.internal.copy.ConnectionCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.ChatImplDependencies
import com.nice.cxonechat.internal.model.ConfigurationInternal
import com.nice.cxonechat.internal.model.GrantType
import com.nice.cxonechat.internal.model.ThirdPartyOAuthBody
import com.nice.cxonechat.internal.model.TokenRequestBody
import com.nice.cxonechat.internal.model.Visitor
import com.nice.cxonechat.internal.model.asCustomerIdentity
import com.nice.cxonechat.internal.socket.ProxyWebSocketListener
import com.nice.cxonechat.internal.socket.SocketConnectionListener
import com.nice.cxonechat.internal.socket.SocketState
import com.nice.cxonechat.internal.socket.WebSocketSpec
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.error
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.thread.CustomField
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.WebSocket
import kotlin.time.Instant

@Suppress("TooManyFunctions")
internal class ChatImpl(
    override var connection: Connection,
    override val entrails: ChatEntrails,
    @get:VisibleForTesting @set:VisibleForTesting internal var dependencies: ChatImplDependencies,
    override val configuration: ConfigurationInternal,
    private val rawChatStateListener: ChatStateListener?,
    override val tokenDelegateListener: TokenDelegateListener? = null,
) : ChatWithParameters,
    LoggerScope by LoggerScope<Chat>(entrails.logger),
    AutoCloseable {

    private val socketFactory get() = dependencies.socketFactory
    private val callback get() = dependencies.callback
    private val authorization get() = dependencies.authorization

    override val isImplicitOAuthFlow: Boolean
        get() = tokenDelegateListener != null && dependencies.authorization == Authorization.None

    override val socketListener: ProxyWebSocketListener = socketFactory.createProxyListener()

    private val _socketFlow = MutableStateFlow<WebSocket?>(null)

    override val socket: WebSocket?
        get() = _socketFlow.value

    override val socketFlow: StateFlow<WebSocket?> = _socketFlow.asStateFlow()

    override var fields = listOf<CustomField>()
    override val environment get() = entrails.environment

    private val actions = ChatActionHandlerImpl(this)

    private val threadsHandler: ChatThreadsHandler by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        var handler: ChatThreadsHandler = ChatThreadsHandlerImpl(this, configuration.preContactSurvey)
        handler = ChatThreadsHandlerReplayLastEmpty(handler)
        handler = when (chatMode) {
            SingleThread -> ChatThreadsHandlerSingle(this, handler)
            MultiThread -> ChatThreadsHandlerMulti(this, handler)
            LiveChat -> ChatThreadsHandlerLive(this, handler)
        }
        handler = ChatThreadsHandlerConfigProxy(handler, this)
        // The per-thread message cache (ChatThreadsHandlerMessages) backfills threads that arrive
        // without messages (single-/multi-thread list responses). In LiveChat the recovered thread
        // already carries its messages, so the cache is unused there — and the per-thread watcher it
        // would build constructs a duplicate LiveChat decorator stack whose welcome/recovery socket
        // subscriptions leak (DE-166650). Omit the decorator entirely in LiveChat.
        if (chatMode != LiveChat) {
            handler = ChatThreadsHandlerMessages(handler, entrails.threading.coroutineScope, this)
        }
        handler = ChatThreadsHandlerMemoizeHandlers(handler)
        handler
    }

    override var lastPageViewed: PageViewEvent? = null

    override var isChatAvailable: Boolean = true

    override val tokenRefreshCoordinator = TokenRefreshCoordinator()

    override var eventHandlerProvider = ChatEventHandlerProvider { chat ->
        var handler: ChatEventHandler
        handler = ChatEventHandlerImpl(chat)
        handler = ChatEventHandlerTokenGuard(handler, chat, tokenRefreshCoordinator)
        handler = ChatEventHandlerVisitGuard(handler, chat)
        handler = ChatEventHandlerTimeOnPage(handler, chat)
        handler = ChatEventHandlerThreading(handler, chat)
        handler
    }

    private val _stateFlow = MutableSharedFlow<ChatStateEvent>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val stateFlow: SharedFlow<ChatStateEvent> = _stateFlow.asSharedFlow()

    @VisibleForTesting
    internal var retryApiHandler = RetryApiHandler(maxRetries = 2, retryIntervalMs = 30_000L)

    @Suppress("DEPRECATION")
    override val chatStateListener: ChatStateListener = object : ChatStateListener {
        override fun onConnecting() {
            _stateFlow.tryEmit(Connecting)
            rawChatStateListener?.onConnecting()
        }

        override fun onConnected() {
            _stateFlow.tryEmit(Connected)
            rawChatStateListener?.onConnected()
        }

        override fun onReady() {
            _stateFlow.tryEmit(Ready)
            rawChatStateListener?.onReady()
        }

        override fun onUnexpectedDisconnect() {
            _stateFlow.tryEmit(UnexpectedDisconnect)
            rawChatStateListener?.onUnexpectedDisconnect()
        }

        override fun onChatRuntimeException(exception: RuntimeChatException) {
            _stateFlow.tryEmit(
                object : RuntimeException {
                    override val exception = exception
                }
            )
            rawChatStateListener?.onChatRuntimeException(exception)
        }
    }

    override fun setDeviceToken(token: String?) {
        val currentToken = entrails.storage.deviceToken
        if (currentToken == token) return
        entrails.storage.deviceToken = token
        entrails.threading.coroutineScope.safeLaunch(childScope("setDeviceToken")) {
            sendVisitorInfo(token)
        }
    }

    private suspend fun sendVisitorInfo(token: String?) {
        val createOrUpdateVisitor = {
            entrails.service.createOrUpdateVisitor(
                brandId = connection.brandId,
                visitorId = entrails.storage.visitorId.toString(),
                visitor = Visitor(connection, deviceToken = token)
            )
        }
        val params = createVisitorRetryParams(createOrUpdateVisitor, callback, chatStateListener)
        withContext(entrails.threading.ioDispatcher) {
            retryApiHandler.executeWithRetry(this, params.action, params.onSuccess, params.onFailure)
        }
    }

    override fun threads(): ChatThreadsHandler = threadsHandler

    override fun events(): ChatEventHandler = eventHandlerProvider.events(this)

    override fun customFields(): ChatFieldHandler = ChatFieldHandlerGlobal(this, this)

    override fun actions(): ChatActionHandler = actions

    override suspend fun signOut() {
        // Cancel/drain in-flight session work (e.g. a token refresh already past its network
        // await) and close the socket BEFORE touching storage/cookies -- otherwise a coroutine
        // that's already past its await can still reach a property setter and persist fresh
        // credentials via storageWriteScope after clearStorage() already ran, re-persisting them
        // after logout. closeSuspending() cancels that work first, so clearStorage() below is not
        // racing anything that could still write.
        closeSuspending()
        storage.clearStorage()
        cookieJar.clearAllCookies()
    }

    /**
     * AutoCloseable requires a non-suspend close(). Bridges into the real, suspend implementation
     * so Java/try-with-resources callers still get a genuine blocking close (matches the
     * @WorkerThread contract). Coroutine callers should call closeSuspending() directly instead.
     */
    override fun close(): Unit = runBlocking { closeSuspending() }

    override suspend fun closeSuspending(): Unit = scope("closeSuspending") {
        _socketFlow.getAndUpdate { null }?.close(WebSocketSpec.CLOSE_NORMAL_CODE, null)
        val storageScope = entrails.threading.storageWriteScope
        val storageJob = try {
            requireNotNull(storageScope.coroutineContext[Job]) {
                "storageWriteScope must carry a Job — Threading contract violation"
            }
        } catch (contractViolation: IllegalArgumentException) {
            error("Threading contract violation during close() — storageWriteScope has no Job", contractViolation)
            entrails.threading.coroutineScope.coroutineContext.cancelChildren()
            null
        }
        if (storageJob != null) {
            try {
                // Snapshot the session's coroutines at close() time and cancel them immediately
                // (reconnect loops, network ops, etc.). storageWriteScope is excluded so it stays
                // alive for the drain below. The SNAPSHOT matters: close() keeps connect() usable
                // on this instance, so jobs launched by a later connect() must never be swept here.
                val childrenAtClose = entrails.threading.coroutineScope.coroutineContext[Job]?.children
                    ?.filter { it !== storageJob }
                    ?.toList()
                    .orEmpty()
                childrenAtClose.forEach { it.cancel() }
                // Cancellation only lands at a coroutine's next suspend point -- a job past its
                // last one (e.g. a token refresh right after its network await) can still launch a
                // storage write before noticing. Join here so any such write already exists by the
                // time storageJob.children is snapshotted below, instead of racing past the drain.
                childrenAtClose.joinAll()
                // Wait for all active write coroutines to finish before returning. The storage
                // scope is intentionally NOT cancelled — storage must keep working for a later
                // connect() on this instance and for integrators still using events(). The
                // close-time snapshot was already cancelled above; jobs a later connect() launches
                // are intentionally left untouched.
                cookieJar.awaitPendingWrites()
                // Excludes self when the caller is itself a child of storageJob (test-only; no-op
                // in production where closeSuspending() is never invoked from storageWriteScope) --
                // otherwise joinAll() would wait on its own currently-suspended Job forever.
                val thisJob = currentCoroutineContext()[Job]
                storageJob.children.filter { it !== thisJob }.toList().joinAll()
            } catch (cancellation: CancellationException) {
                // A cancelled closeSuspending() must propagate, not be logged and silently
                // swallowed -- callers (e.g. a ViewModel torn down mid-close) need to see the
                // cancellation, not a false "completed" return.
                throw cancellation
            } catch (expected: Exception) {
                error("close() drain failed while awaiting pending storage/cookie writes", expected)
                entrails.threading.coroutineScope.coroutineContext.cancelChildren()
            }
        }
    }

    @Suppress(
        "DEPRECATION",
        "LongMethod" // Temporary suppression for the merge
    )
    override suspend fun connect() {
        socketListener.reportState(SocketState.CONNECTING)
        chatStateListener.onConnecting()
        val tokenRequestBodyWithMeta = try {
            createAuthRequestBody(configuration.authenticationType)
        } catch (exception: TokenDelegationFailedException) {
            chatStateListener.onChatRuntimeException(exception)
            socketListener.reportState(SocketState.CLOSED)
            return
        }

        // Register listener BEFORE creating socket to avoid any race condition.
        // Handles both successful open and failure so connect() never suspends indefinitely.
        val openDeferred = CompletableDeferred<Unit>()
        val openListener = SocketConnectionListener(
            onConnected = { openDeferred.complete(Unit) },
            onFailed = { t -> openDeferred.completeExceptionally(t) },
        )
        socketListener.addListener(openListener)
        try {
            val connector = ChatSocketConnector(
                chat = this,
                socketFactory = socketFactory,
                chatStateListener = chatStateListener
            )
            // runCatching captures raw throws from connector (e.g. IOException from execute()).
            // getOrElse re-throws JVM Errors and wraps Exceptions into Result.failure.
            val connectionResult = runCatching { connector.connect(tokenRequestBodyWithMeta.requestBody) }
                .getOrElse { t ->
                    if (t is Error || t is CancellationException) throw t
                    Result.failure(t)
                }
                .mapFailure { t ->
                    // ConnectionTokenFailed means ThirdPartyOAuth token expired — not retriable.
                    // All other errors (e.g. 5xx outages) are transient.
                    when (t) {
                        is RuntimeChatException.ConnectionTokenFailed ->
                            PermanentConnectionFailureException("Expired token requires re-authentication", t)

                        else ->
                            TransientConnectFailureException("Connection failed", t)
                    }
                }
                .getOrThrow()
            val customerId = connectionResult.transactionTokenModel?.customerIdentity?.idOnExternalPlatform
            // For implicit flow prefer the token actually accepted by the backend. On a two-step
            // reconnect (stored JWT rejected → delegate called with TOKEN_INVALID → fresh JWT accepted)
            // result.refreshedOAuthToken holds the fresh token; fall back to the initial delegate token
            // for the single-step path where the stored JWT was accepted on the first attempt.
            val effectiveOAuthToken = connectionResult.refreshedOAuthToken ?: tokenRequestBodyWithMeta.oauthToken
            storage.authToken = if (isImplicitOAuthFlow) {
                // For implicit flow, store the JWT from the delegate — it is the credential used
                // by the Transaction Token API and must be re-used for SDK events.
                effectiveOAuthToken?.accessToken
            } else {
                connectionResult.transactionTokenModel?.thirdParty?.accessToken
            }
            storage.authTokenExpDate = if (isImplicitOAuthFlow) {
                // Expiry comes from the OAuthToken returned by the delegate, not the server response.
                effectiveOAuthToken?.expiryDateMillis?.let { Instant.fromEpochMilliseconds(it) }
            } else {
                connectionResult.transactionTokenModel?.thirdParty?.expiresAt
            }
            storage.transactionTokenModel = connectionResult.transactionTokenModel
            connection = connection.asCopyable().copy(customerId = customerId)
            storage.customerId = customerId
            _socketFlow.value = connectionResult.webSocket
            try {
                openDeferred.await()
            } catch (e: CancellationException) {
                throw e
            } catch (loggedException: Exception) {
                // Socket failed to open; chatStateListener notification handled upstream by
                // ReconnectingListener.onFailure — do not double-report here.
                // Clear stale socket reference so a subsequent reconnect starts clean.
                _socketFlow.value = null
                throw TransientConnectFailureException("Socket failed to open", loggedException)
            }
        } finally {
            socketListener.removeListener(openListener)
        }
    }

    internal fun createAuthRequestBody(authenticationType: AuthenticationType): TokenRequestBodyWithMeta {
        return when (authenticationType) {
            AuthenticationType.SecuredCookie -> TokenRequestBodyWithMeta(TokenRequestBody())
            AuthenticationType.Anonymous -> TokenRequestBodyWithMeta(
                TokenRequestBody(
                    type = authenticationType.name,
                    customerIdentity = connection.customerId?.let { connection.asCustomerIdentity() }
                )
            )

            AuthenticationType.ThirdPartyOAuth ->
                if (isImplicitOAuthFlow) {
                    // Implicit flow: delegate supplies the JWT + expiry
                    val oauthToken = requestImplicitToken(TokenRequestReason.UNSPECIFIED).getOrElse { cause ->
                        throw cause as? TokenDelegationFailedException
                            ?: TokenDelegationFailedException(cause.message ?: "Token delegation failed", cause)
                    }
                    TokenRequestBodyWithMeta(
                        requestBody = TokenRequestBody(
                            thirdParty = ThirdPartyOAuthBody(
                                grantType = GrantType.IMPLICIT,
                                accessToken = oauthToken.accessToken
                            )
                        ),
                        oauthToken = oauthToken,
                    )
                } else {
                    // Explicit flow: auth code + code verifier provided via setAuthorization()
                    TokenRequestBodyWithMeta(
                        TokenRequestBody(
                            thirdParty = ThirdPartyOAuthBody(
                                grantType = GrantType.AUTHORIZATION_CODE,
                                authorizationCode = authorization.code,
                                codeVerifier = authorization.verifier
                            )
                        )
                    )
                }
        }
    }

    override fun setUserName(firstName: String, lastName: String) {
        if (!configuration.isAuthorizationEnabled) {
            connection = connection.asCopyable().copy(firstName = firstName, lastName = lastName)
        }
    }

    override suspend fun getChannelAvailability(): Boolean =
        isChatAvailable
}
