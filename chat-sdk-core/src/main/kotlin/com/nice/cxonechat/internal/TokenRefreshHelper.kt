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

import com.nice.cxonechat.exceptions.RuntimeChatException
import com.nice.cxonechat.internal.model.GrantType
import com.nice.cxonechat.internal.model.ThirdPartyOAuthBody
import com.nice.cxonechat.internal.model.TokenRequestBody
import com.nice.cxonechat.internal.socket.SocketState
import com.nice.cxonechat.log.Level
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.time.Instant

/**
 * Helper utility for handling access token refresh operations.
 * This class provides a centralized location for token refresh logic
 * used by both [ChatEventHandlerTokenGuard] and [ChatThreadEventHandlerTokenGuard].
 */
internal object TokenRefreshHelper {

    /**
     * Refreshes the access token. For the implicit flow, calls the integrator delegate which
     * must return an [com.nice.cxonechat.OAuthToken] containing both the new JWT and its expiry date. Both are
     * persisted to storage for use by future events.
     *
     * @param chat The chat instance with storage and configuration
     */
    @Suppress("TooGenericExceptionCaught")
    internal suspend fun refreshAccessToken(chat: ChatWithParameters) {
        val resolvedBody = resolveTokenRequestBody(chat) ?: return
        val result = try {
            withContext(chat.entrails.threading.ioDispatcher) {
                TransactionTokenHelper.getTransactionToken(
                    entrails = chat.entrails,
                    brandId = chat.connection.brandId.toString(),
                    channelId = chat.connection.channelId,
                    visitorId = chat.storage.visitorId.toString(),
                    tokenRequestBody = resolvedBody.requestBody
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            chat.entrails.logger.log(Level.Error, "Network error during token refresh", e)
            TransactionTokenHelper.TransactionTokenResult.Error(
                e.message ?: "Network error during token refresh: ${e::class.simpleName}"
            )
        } catch (e: RuntimeException) {
            chat.entrails.logger.log(Level.Error, "Unexpected error during token refresh", e)
            TransactionTokenHelper.TransactionTokenResult.Error(
                e.message ?: "Unexpected error during token refresh: ${e::class.simpleName}"
            )
        }
        when (result) {
            is TransactionTokenHelper.TransactionTokenResult.Success -> {
                val transactionTokenModel = result.transactionTokenModel
                if (chat.isImplicitOAuthFlow) {
                    // Store the JWT from the delegate response — it is the credential used
                    // by future SDK events. The expiry comes directly from the delegate's
                    // OAuthToken so we never rely on the server-side ThirdParty expiry field
                    // (which is absent for implicit flow).
                    chat.storage.authToken = resolvedBody.oauthToken?.accessToken
                    chat.storage.authTokenExpDate = resolvedBody.oauthToken?.expiryDateMillis
                        ?.let { Instant.fromEpochMilliseconds(it) }
                } else {
                    chat.storage.authToken = transactionTokenModel.thirdParty?.accessToken
                    chat.storage.authTokenExpDate = transactionTokenModel.thirdParty?.expiresAt
                }
                chat.storage.transactionTokenModel = transactionTokenModel
            }

            is TransactionTokenHelper.TransactionTokenResult.Error -> {
                chat.chatStateListener?.onChatRuntimeException(RuntimeChatException.AuthorizationError(result.message))
                chat.socketListener.reportState(SocketState.CLOSED)
            }
        }
    }

    private fun resolveTokenRequestBody(chat: ChatWithParameters): TokenRequestBodyWithMeta? {
        val refreshToken = chat.storage.transactionTokenModel?.thirdParty?.refreshToken
        return when {
            chat.isImplicitOAuthFlow -> resolveImplicitRequestBody(chat)
            refreshToken != null -> TokenRequestBodyWithMeta(
                requestBody = TokenRequestBody(
                    thirdParty = ThirdPartyOAuthBody(grantType = GrantType.REFRESH_TOKEN, refreshToken = refreshToken)
                ),
            )

            else -> {
                chat.chatStateListener?.onChatRuntimeException(
                    RuntimeChatException.AuthorizationError("Missing refresh token for access token refresh.")
                )
                chat.socketListener.reportState(SocketState.CLOSED)
                null
            }
        }
    }

    private fun resolveImplicitRequestBody(chat: ChatWithParameters): TokenRequestBodyWithMeta? {
        if (chat.tokenDelegateListener == null) {
            chat.chatStateListener?.onChatRuntimeException(
                RuntimeChatException.AuthorizationError("Implicit flow requires a TokenDelegateListener.")
            )
            chat.socketListener.reportState(SocketState.CLOSED)
            return null
        }
        return chat.requestImplicitToken().fold(
            onSuccess = { oauthToken ->
                TokenRequestBodyWithMeta(
                    requestBody = TokenRequestBody(
                        thirdParty = ThirdPartyOAuthBody(
                            grantType = GrantType.IMPLICIT,
                            accessToken = oauthToken.accessToken,
                        )
                    ),
                    oauthToken = oauthToken,
                )
            },
            onFailure = { cause ->
                val exception = cause as? RuntimeChatException.TokenDelegationFailedException
                    ?: RuntimeChatException.TokenDelegationFailedException(cause.message ?: "Token delegation failed", cause)
                chat.chatStateListener?.onChatRuntimeException(exception)
                chat.socketListener.reportState(SocketState.CLOSED)
                null
            }
        )
    }
}
