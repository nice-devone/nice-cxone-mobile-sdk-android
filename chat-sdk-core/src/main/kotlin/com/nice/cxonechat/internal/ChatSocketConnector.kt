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

import com.nice.cxonechat.ChatStateListener
import com.nice.cxonechat.OAuthToken
import com.nice.cxonechat.TokenRequestReason.TOKEN_INVALID
import com.nice.cxonechat.enums.AuthenticationType
import com.nice.cxonechat.exceptions.RuntimeChatException
import com.nice.cxonechat.exceptions.RuntimeChatException.AuthorizationError
import com.nice.cxonechat.exceptions.RuntimeChatException.InvalidAccessTokenException
import com.nice.cxonechat.exceptions.RuntimeChatException.TokenDelegationFailedException
import com.nice.cxonechat.internal.TokenRefreshCoordinator.Companion.EXPIRY_BUFFER
import com.nice.cxonechat.internal.TransactionTokenHelper.TransactionTokenResult.Error
import com.nice.cxonechat.internal.TransactionTokenHelper.TransactionTokenResult.Success
import com.nice.cxonechat.internal.model.GrantType
import com.nice.cxonechat.internal.model.ThirdParty
import com.nice.cxonechat.internal.model.ThirdPartyOAuthBody
import com.nice.cxonechat.internal.model.TokenRequestBody
import com.nice.cxonechat.internal.model.TransactionTokenModel
import com.nice.cxonechat.internal.model.mergeThirdParty
import com.nice.cxonechat.internal.socket.ProxyWebSocketListener
import com.nice.cxonechat.internal.socket.SocketFactory
import com.nice.cxonechat.internal.socket.SocketState
import com.nice.cxonechat.internal.socket.WebsocketLogging
import com.nice.cxonechat.storage.ValueStorage
import com.nice.cxonechat.util.expiresWithin
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.withLock
import okhttp3.WebSocket

/**
 * Success payload from a socket connection attempt — carries the connected socket and optional
 * transaction token. Distinct from Kotlin's [Result] wrapper that delivers this payload.
 */
internal data class ChatSocketConnectionResult(
    val webSocket: WebSocket,
    val transactionTokenModel: TransactionTokenModel?,
    /**
     * For implicit flow two-step reconnect: the fresh [OAuthToken] obtained from the delegate
     * on the second attempt. When non-null, [ChatImpl.connect] must store this token (not the
     * one from the initial [TokenRequestBodyWithMeta]) to keep SDK storage consistent.
     */
    val refreshedOAuthToken: OAuthToken? = null,
)

/**
 * Handles the API call and WebSocket setup for ChatImpl, reporting socket state transitions
 * to [com.nice.cxonechat.internal.socket.ProxyWebSocketListener] and runtime exceptions to
 * [ChatStateListener]. Returns the connected socket and optional token as [Result].
 */
internal class ChatSocketConnector(
    private val chat: ChatWithParameters,
    private val socketFactory: SocketFactory,
    private val chatStateListener: ChatStateListener?,
) {
    internal suspend fun connect(tokenRequestBody: TokenRequestBody): Result<ChatSocketConnectionResult> {
        val configuration = chat.configuration
        val entrails = chat.entrails
        val storage = entrails.storage
        val socketListener = chat.socketListener
        val transactionTokenModel = storage.transactionTokenModel

        return when (transactionTokenModel?.isExpired) {
            // Case 1: Valid cached transaction token exists
            false ->
                connectWithTransactionToken(socketListener, storage, entrails, transactionTokenModel)
            // Case 2: ThirdPartyOAuth + expired transaction token
            true if configuration.authenticationType == AuthenticationType.ThirdPartyOAuth ->
                if (chat.isImplicitOAuthFlow) {
                    // Implicit flow: ask the delegate for a fresh JWT, then fetch a new transaction token
                    connectWithImplicitJwt(tokenRequestBody, socketListener, storage, entrails)
                } else {
                    // Explicit flow: transaction token expired, but the access token backing it may
                    // still be valid — reuse it via an implicit grant before spending the refresh token.
                    // Derived from transactionTokenModel.thirdParty (not the separately-written
                    // storage.authTokenExpDate) so the expiry and the access token used below always
                    // come from the same refresh generation: TokenRefreshHelper.persistSuccessfulRefresh
                    // writes authTokenExpDate and transactionTokenModel as two separate storage writes,
                    // so a concurrent proactive refresh could otherwise be observed mid-write here.
                    val accessTokenExpDate = transactionTokenModel.thirdParty?.expiresAt
                    val isAccessTokenValid = accessTokenExpDate != null && !accessTokenExpDate.expiresWithin(EXPIRY_BUFFER)
                    if (isAccessTokenValid) {
                        connectWithValidAccessToken(transactionTokenModel, socketListener, storage, entrails)
                    } else {
                        connectWithRefreshToken(transactionTokenModel, socketListener, storage, entrails)
                    }
                }
            // Case 3: Fetch new transaction token
            else -> fetchTransactionToken(tokenRequestBody, socketListener, storage, entrails)
        }
    }

    private fun connectWithTransactionToken(
        socketListener: ProxyWebSocketListener,
        storage: ValueStorage,
        entrails: ChatEntrails,
        transactionTokenModel: TransactionTokenModel,
    ): Result<ChatSocketConnectionResult> =
        createSocket(socketListener, entrails) {
            socketFactory.create(
                listener = socketListener,
                visitorId = storage.visitorId.toString(),
                transactionToken = transactionTokenModel.transactionToken
            )
        }.map { ChatSocketConnectionResult(webSocket = it, transactionTokenModel = transactionTokenModel) }

    private fun requestTransactionToken(
        tokenRequestBody: TokenRequestBody,
        storage: ValueStorage,
        entrails: ChatEntrails,
    ): TransactionTokenHelper.TransactionTokenResult {
        val connection = chat.connection
        return TransactionTokenHelper.getTransactionToken(
            entrails = entrails,
            brandId = connection.brandId.toString(),
            channelId = connection.channelId,
            visitorId = storage.visitorId.toString(),
            tokenRequestBody = tokenRequestBody
        )
    }

    private fun buildTokenErrorException(
        tokenResult: Error,
        socketListener: ProxyWebSocketListener,
        reportError: Boolean,
    ): RuntimeChatException {
        var exception: RuntimeChatException = AuthorizationError(tokenResult.message)
        if (reportError) {
            chatStateListener?.onChatRuntimeException(exception)
            socketListener.reportState(SocketState.CLOSED)
        } else if (tokenResult.isTokenRejection) {
            exception = InvalidAccessTokenException(
                "Backend rejected the OAuth-delegated JWT. " +
                        "Obtain a fresh token via OAuth and reconnect."
            )
        }
        return exception
    }

    private fun fetchTransactionToken(
        tokenRequestBody: TokenRequestBody,
        socketListener: ProxyWebSocketListener,
        storage: ValueStorage,
        entrails: ChatEntrails,
        reportError: Boolean = true,
    ): Result<ChatSocketConnectionResult> =
        when (val tokenResult = requestTransactionToken(tokenRequestBody, storage, entrails)) {
            is Error -> Result.failure(buildTokenErrorException(tokenResult, socketListener, reportError))
            is Success -> {
                val newTransactionTokenModel = tokenResult.transactionTokenModel
                val transactionToken = newTransactionTokenModel.transactionToken
                if (transactionToken != null && newTransactionTokenModel.expiresAt != null) {
                    createSocket(socketListener, entrails) {
                        socketFactory.create(socketListener, storage.visitorId.toString(), transactionToken)
                    }.map { websocket ->
                        ChatSocketConnectionResult(
                            webSocket = websocket,
                            transactionTokenModel = newTransactionTokenModel
                        )
                    }
                } else {
                    Result.failure(
                        buildTokenErrorException(
                            Error("Transaction token response did not include a transaction token or expiry"),
                            socketListener,
                            reportError,
                        )
                    )
                }
            }
        }

    /**
     * Explicit flow, expired transaction token, but the cached OAuth access token is still valid:
     * trade it directly for a new transaction token via an implicit grant, skipping the refresh token.
     *
     * The backend's response to this internally-triggered implicit grant isn't guaranteed to echo
     * back [ThirdParty] (implicit-flow consumers already hold their own token, so the backend has no
     * reason to return it) — the access token wasn't refreshed here, so the correct third-party state
     * afterward is simply the one already cached. Patch it onto the result rather than trusting the
     * response, so `ChatImpl` persists the right access token/expiry/refresh token.
     *
     * The access token is locally unexpired, but the backend may still reject it (revoked
     * elsewhere, session invalidated, clock skew). On that rejection, fall back to spending the
     * refresh token instead of surfacing an [AuthorizationError] the SDK can never recover from —
     * mirroring the two-step retry already used by [connectWithImplicitJwt].
     */
    private suspend fun connectWithValidAccessToken(
        expiredModel: TransactionTokenModel,
        socketListener: ProxyWebSocketListener,
        storage: ValueStorage,
        entrails: ChatEntrails,
    ): Result<ChatSocketConnectionResult> {
        val accessToken = expiredModel.thirdParty?.accessToken
            ?: return connectWithRefreshToken(expiredModel, socketListener, storage, entrails)
        val implicitRequestBody = TokenRequestBody(
            thirdParty = ThirdPartyOAuthBody(grantType = GrantType.IMPLICIT, accessToken = accessToken)
        )
        val result = fetchTransactionToken(implicitRequestBody, socketListener, storage, entrails, reportError = false)
            .withEffectiveThirdParty(expiredModel.thirdParty)
        return if (result.exceptionOrNull() is InvalidAccessTokenException) {
            connectWithRefreshToken(expiredModel, socketListener, storage, entrails)
        } else {
            if (result.isFailure) {
                (result.exceptionOrNull() as? RuntimeChatException)?.let { chatStateListener?.onChatRuntimeException(it) }
                socketListener.reportState(SocketState.CLOSED)
            }
            result
        }
    }

    /**
     * Explicit flow, expired transaction token AND expired access token: spend the refresh token to
     * mint a new access token, then trade that new access token for the transaction token used for
     * the socket via a second, implicit-grant call.
     *
     * The freshly minted access token/expiry/refresh token come from the first (refresh-grant) response;
     * the second (implicit-grant) response isn't guaranteed to echo them back (see
     * [connectWithValidAccessToken]), so the result is patched with the known-correct third-party state
     * rather than whatever that second response returned.
     *
     * The refresh call and the resulting credential persist happen under [Threading.Guards.tokenRefreshMutex] —
     * the same lock [TokenRefreshCoordinator] uses for its proactive, event-send-triggered refresh —
     * so a reconnect and that proactive refresh can never both spend the same refresh token.
     * Right after the lock is acquired, the cached third-party is re-read: if it changed since
     * [expiredModel] was captured (the other side of that race already rotated it while this call
     * was waiting) *and* the access token it now holds is still locally valid, that credential is
     * reused directly instead of spending its refresh token a second time. If it's unchanged — e.g.
     * this call is [connectWithValidAccessToken]'s own fallback after the backend just rejected that
     * exact access token — the local expiry is not trusted and the refresh proceeds regardless.
     * On an actual refresh, all three credential fields (`authToken`, `authTokenExpDate`,
     * `transactionTokenModel`) are persisted together before the lock is released: leaving
     * `authTokenExpDate` stale would make [TokenRefreshCoordinator] think another refresh is still
     * needed even though this one just succeeded, spending the just-minted refresh token a second
     * time for no reason. The credential is persisted before the second (implicit-grant) step
     * runs, too: with a rotating refresh token, the first call may have already invalidated the one
     * previously in storage, so if the implicit-grant call or socket creation below fails, storage
     * must still hold the new one — otherwise the next reconnect attempt would retry with an
     * already-rotated-out refresh token and fail permanently.
     */
    private suspend fun connectWithRefreshToken(
        expiredModel: TransactionTokenModel,
        socketListener: ProxyWebSocketListener,
        storage: ValueStorage,
        entrails: ChatEntrails,
    ): Result<ChatSocketConnectionResult> {
        val credential = chat.guards.tokenRefreshMutex.withLock {
            val cachedModel = storage.transactionTokenModel
            val cachedThirdParty = cachedModel?.thirdParty
            val rotatedByOther = cachedThirdParty != null && cachedThirdParty != expiredModel.thirdParty
            val (modelToRefresh, reusable) = if (rotatedByOther) {
                val rotated = cachedModel ?: expiredModel
                rotated to rotated.thirdParty?.asRefreshedAccessTokenIfValid()
            } else {
                expiredModel to null
            }
            if (reusable != null) {
                Result.success(ReusableCredential(reusable, reusedCredential = true, modelToRefresh))
            } else {
                refreshAccessToken(modelToRefresh, socketListener, storage, entrails)
                    .onSuccess {
                        storage.authToken = it.thirdParty.accessToken
                        storage.authTokenExpDate = it.thirdParty.expiresAt
                        storage.transactionTokenModel = modelToRefresh.copy(thirdParty = it.thirdParty)
                    }
                    .map { ReusableCredential(it, reusedCredential = false, modelToRefresh) }
            }
        }
        return credential.fold(
            onFailure = { Result.failure(it) },
            onSuccess = { (refreshedAccessToken, reusedCredential, modelToRefresh) ->
                val implicitRequestBody = TokenRequestBody(
                    thirdParty = ThirdPartyOAuthBody(grantType = GrantType.IMPLICIT, accessToken = refreshedAccessToken.accessToken)
                )
                val result = fetchTransactionToken(
                    implicitRequestBody,
                    socketListener,
                    storage,
                    entrails,
                    reportError = !reusedCredential,
                ).withEffectiveThirdParty(refreshedAccessToken.thirdParty)

                // The access token above was reused directly from a concurrently-rotated
                // credential rather than just minted, so — mirroring
                // connectWithValidAccessToken's own fallback — a backend rejection of it is
                // recoverable by falling back to modelToRefresh's refresh token instead of
                // surfacing an unrecoverable AuthorizationError.
                when {
                    !reusedCredential -> result
                    result.exceptionOrNull() is InvalidAccessTokenException ->
                        connectWithRefreshToken(modelToRefresh, socketListener, storage, entrails)
                    else -> {
                        if (result.isFailure) {
                            (result.exceptionOrNull() as? RuntimeChatException)?.let { chatStateListener?.onChatRuntimeException(it) }
                            socketListener.reportState(SocketState.CLOSED)
                        }
                        result
                    }
                }
            }
        )
    }

    private fun refreshAccessToken(
        expiredModel: TransactionTokenModel,
        socketListener: ProxyWebSocketListener,
        storage: ValueStorage,
        entrails: ChatEntrails,
    ): Result<RefreshedAccessToken> {
        val refreshToken = expiredModel.thirdParty?.refreshToken
            ?: return reportConnectionTokenFailed("No refresh token available", chatStateListener, socketListener)
        val refreshRequestBody = TokenRequestBody(
            thirdParty = ThirdPartyOAuthBody(
                grantType = GrantType.REFRESH_TOKEN,
                refreshToken = refreshToken
            )
        )
        return when (val refreshResult = requestTransactionToken(refreshRequestBody, storage, entrails)) {
            is Error -> {
                val exception = refreshResult.toRefreshTokenGrantException()
                chatStateListener?.onChatRuntimeException(exception)
                socketListener.reportState(SocketState.CLOSED)
                Result.failure(exception)
            }
            is Success -> {
                // The response's thirdParty may omit refresh_token (backend doesn't rotate it on every
                // refresh) — merge with the cached one instead of replacing it wholesale, so a still-valid
                // refresh token survives for the next reconnect cycle. See mergeThirdParty.
                val thirdParty = mergeThirdParty(expiredModel.thirdParty, refreshResult.transactionTokenModel.thirdParty)
                thirdParty?.accessToken?.let { accessToken -> Result.success(RefreshedAccessToken(accessToken, thirdParty)) }
                    ?: reportConnectionTokenFailed(
                        "Refresh token response did not include a new access token",
                        chatStateListener,
                        socketListener,
                    )
            }
        }
    }

    /**
     * Implicit flow reconnect: expired transaction token.
     * Step 1: try with the JWT already in tokenRequestBody (covers app-restart with valid cached JWT).
     * Step 2: if backend rejects it (HTTP 400), ask the delegate for a fresh JWT and retry once.
     */
    private fun connectWithImplicitJwt(
        storedTokenRequestBody: TokenRequestBody,
        socketListener: ProxyWebSocketListener,
        storage: ValueStorage,
        entrails: ChatEntrails,
    ): Result<ChatSocketConnectionResult> {
        val firstAttempt = fetchTransactionToken(
            tokenRequestBody = storedTokenRequestBody,
            socketListener = socketListener,
            storage = storage,
            entrails = entrails,
            reportError = false,
        )
        val shouldCallDelegate = firstAttempt.exceptionOrNull() is InvalidAccessTokenException
        if (!shouldCallDelegate) {
            if (firstAttempt.isFailure) {
                (firstAttempt.exceptionOrNull() as? RuntimeChatException)?.let { chatStateListener?.onChatRuntimeException(it) }
                socketListener.reportState(SocketState.CLOSED)
            }
            return firstAttempt
        }

        // Stored JWT was rejected by the backend — ask the delegate for a fresh one
        return chat.requestImplicitToken(reason = TOKEN_INVALID).fold(
            onSuccess = { oauthToken ->
                val result = fetchTransactionToken(
                    tokenRequestBody = TokenRequestBody(
                        thirdParty = ThirdPartyOAuthBody(
                            grantType = GrantType.IMPLICIT,
                            accessToken = oauthToken.accessToken
                        )
                    ),
                    socketListener = socketListener,
                    storage = storage,
                    entrails = entrails,
                    reportError = false,
                )
                result.map {
                    it.copy(refreshedOAuthToken = oauthToken)
                }.onFailure {
                    if (it is RuntimeChatException) {
                        chatStateListener?.onChatRuntimeException(it)
                    }
                    socketListener.reportState(SocketState.CLOSED)
                }
            },
            onFailure = { cause ->
                val exception = cause as? TokenDelegationFailedException
                    ?: TokenDelegationFailedException(cause.message ?: "Token delegation failed", cause)
                chatStateListener?.onChatRuntimeException(exception)
                socketListener.reportState(SocketState.CLOSED)
                Result.failure(exception)
            }
        )
    }

    private fun createSocket(
        socketListener: ProxyWebSocketListener,
        entrails: ChatEntrails,
        create: () -> WebSocket,
    ): Result<WebsocketLogging> {
        val result = try {
            Result.success(
                WebsocketLogging(
                    socket = create().also { socketListener.reportState(SocketState.CONNECTED) },
                    logger = entrails.logger
                )
            )
        } catch (expected: CancellationException) {
            throw expected
        } catch (expected: Exception) {
            Result.failure(expected)
        }
        if (result.isFailure) socketListener.reportState(SocketState.CLOSED)
        return result
    }
}

private fun <T> reportConnectionTokenFailed(
    message: String,
    chatStateListener: ChatStateListener?,
    socketListener: ProxyWebSocketListener,
): Result<T> {
    val exception = RuntimeChatException.ConnectionTokenFailed(message)
    chatStateListener?.onChatRuntimeException(exception)
    socketListener.reportState(SocketState.CLOSED)
    return Result.failure(exception)
}

private data class RefreshedAccessToken(val accessToken: String, val thirdParty: ThirdParty)

private fun ThirdParty.asRefreshedAccessTokenIfValid(): RefreshedAccessToken? {
    val token = accessToken ?: return null
    return if (expiresAt.expiresWithin(EXPIRY_BUFFER)) null else RefreshedAccessToken(token, this)
}

/**
 * Outcome of [ChatSocketConnector.connectWithRefreshToken]'s credential-acquisition step: the
 * access token to trade via an implicit grant, whether it was [reusedCredential] from a
 * concurrently-rotated credential rather than just minted, and the model to fall back to
 * refreshing ([modelToRefresh]) if the backend rejects a reused token.
 */
private data class ReusableCredential(
    val refreshedAccessToken: RefreshedAccessToken,
    val reusedCredential: Boolean,
    val modelToRefresh: TransactionTokenModel,
)

private fun Result<ChatSocketConnectionResult>.withEffectiveThirdParty(thirdParty: ThirdParty?): Result<ChatSocketConnectionResult> =
    map { it.copy(transactionTokenModel = it.transactionTokenModel?.copy(thirdParty = thirdParty)) }
