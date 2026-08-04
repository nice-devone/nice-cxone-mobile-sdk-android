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
import com.nice.cxonechat.exceptions.RuntimeChatException.FeatureUnavailableException
import com.nice.cxonechat.exceptions.RuntimeChatException.InvalidAccessTokenException
import com.nice.cxonechat.exceptions.RuntimeChatException.TokenDelegationFailedException
import com.nice.cxonechat.internal.TransactionTokenHelper.TransactionTokenResult.Error
import com.nice.cxonechat.internal.TransactionTokenHelper.TransactionTokenResult.Success
import com.nice.cxonechat.internal.model.GrantType
import com.nice.cxonechat.internal.model.ThirdPartyOAuthBody
import com.nice.cxonechat.internal.model.TokenRequestBody
import com.nice.cxonechat.internal.model.TransactionTokenModel
import com.nice.cxonechat.internal.socket.ProxyWebSocketListener
import com.nice.cxonechat.internal.socket.SocketFactory
import com.nice.cxonechat.internal.socket.SocketState
import com.nice.cxonechat.internal.socket.WebsocketLogging
import com.nice.cxonechat.storage.ValueStorage
import kotlinx.coroutines.CancellationException
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
    internal fun connect(tokenRequestBody: TokenRequestBody): Result<ChatSocketConnectionResult> {
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
                    // Explicit flow: use refresh_token to obtain a new transaction token
                    connectWithRefreshToken(transactionTokenModel, socketListener, storage, entrails)
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

    private fun fetchTransactionToken(
        tokenRequestBody: TokenRequestBody,
        socketListener: ProxyWebSocketListener,
        storage: ValueStorage,
        entrails: ChatEntrails,
        reportError: Boolean = true,
    ): Result<ChatSocketConnectionResult> {
        val connection = chat.connection
        val tokenResult = TransactionTokenHelper.getTransactionToken(
            entrails = entrails,
            brandId = connection.brandId.toString(),
            channelId = connection.channelId,
            visitorId = storage.visitorId.toString(),
            tokenRequestBody = tokenRequestBody
        )
        return when (tokenResult) {
            is Error -> {
                // Only treat API-level failures (httpStatusCode != -1) as FeatureUnavailableException.
                // Network/transport errors (httpStatusCode == -1) are transient and retryable — keep them as AuthorizationError.
                val isExplicitFlow = tokenRequestBody.thirdParty?.grantType == GrantType.AUTHORIZATION_CODE
                var exception: RuntimeChatException = if (isExplicitFlow && tokenResult.httpStatusCode != -1) {
                    FeatureUnavailableException(
                        "Explicit OAuth flow is not available: ${tokenResult.message}"
                    )
                } else {
                    AuthorizationError(tokenResult.message)
                }
                if (reportError) {
                    chatStateListener?.onChatRuntimeException(exception)
                    socketListener.reportState(SocketState.CLOSED)
                } else if (tokenResult.isTokenRejection) {
                    exception = InvalidAccessTokenException(
                        "Backend rejected the OAuth-delegated JWT. " +
                                "Obtain a fresh token via OAuth and reconnect."
                    )
                }
                Result.failure(exception)
            }

            is Success -> {
                val newTransactionTokenModel = tokenResult.transactionTokenModel
                createSocket(socketListener, entrails) {
                    socketFactory.create(socketListener, storage.visitorId.toString(), newTransactionTokenModel.transactionToken)
                }.map { websocket ->
                    ChatSocketConnectionResult(
                        webSocket = websocket,
                        transactionTokenModel = newTransactionTokenModel
                    )
                }
            }
        }
    }

    private fun connectWithRefreshToken(
        expiredModel: TransactionTokenModel,
        socketListener: ProxyWebSocketListener,
        storage: ValueStorage,
        entrails: ChatEntrails,
    ): Result<ChatSocketConnectionResult> {
        val refreshToken = expiredModel.thirdParty?.refreshToken
        if (refreshToken == null) {
            val exception = RuntimeChatException.ConnectionTokenFailed("No refresh token available")
            chatStateListener?.onChatRuntimeException(exception)
            socketListener.reportState(SocketState.CLOSED)
            return Result.failure(exception)
        }
        val tokenRequestBody = TokenRequestBody(
            thirdParty = ThirdPartyOAuthBody(
                grantType = GrantType.REFRESH_TOKEN,
                refreshToken = refreshToken
            )
        )
        return fetchTransactionToken(tokenRequestBody, socketListener, storage, entrails)
    }

    /**
     * Implicit flow reconnect: expired transaction token.
     * Step 1: try with the JWT already in tokenRequestBody (covers app-restart with valid cached JWT).
     * Step 2: if backend rejects it (HTTP 401), ask the delegate for a fresh JWT and retry once.
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
                chatStateListener?.onChatRuntimeException(firstAttempt.exceptionOrNull() as RuntimeChatException)
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
