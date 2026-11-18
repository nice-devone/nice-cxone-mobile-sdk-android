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
import com.nice.cxonechat.internal.model.TransactionTokenModel
import com.nice.cxonechat.internal.model.mergeThirdParty
import com.nice.cxonechat.internal.socket.SocketState
import com.nice.cxonechat.log.Level
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID
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
        // clearStorage() (signOut) rotates visitorId, so it marks the session this refresh belongs to.
        // A refresh can run on a scope signOut() does not cancel (an integrator's events() call),
        // and must not write the signed-out session's credentials back after the clear.
        val sessionVisitorId = chat.storage.visitorId
        val resolvedBody = resolveTokenRequestBody(chat, sessionVisitorId) ?: return
        val result = try {
            withContext(chat.entrails.threading.ioDispatcher) {
                // Checked on IO, before the request: signOut() may have run while the delegate blocked or in the hop.
                if (isStorageClearedSince(chat, sessionVisitorId)) return@withContext null
                TransactionTokenHelper.getTransactionToken(
                    entrails = chat.entrails,
                    brandId = chat.connection.brandId.toString(),
                    channelId = chat.connection.channelId,
                    visitorId = sessionVisitorId.toString(),
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
        if (result == null || isStorageClearedSince(chat, sessionVisitorId)) return
        when (result) {
            is TransactionTokenHelper.TransactionTokenResult.Success ->
                persistSuccessfulRefresh(chat, resolvedBody, result.transactionTokenModel)

            is TransactionTokenHelper.TransactionTokenResult.Error -> {
                val isRefreshTokenGrant = resolvedBody.requestBody.thirdParty?.grantType == GrantType.REFRESH_TOKEN
                val exception = if (isRefreshTokenGrant) {
                    result.toRefreshTokenGrantException()
                } else {
                    RuntimeChatException.AuthorizationError(result.message)
                }
                reportRefreshFailure(chat, sessionVisitorId, exception)
            }
        }
    }

    /**
     * Whether [ValueStorage.clearStorage][com.nice.cxonechat.storage.ValueStorage.clearStorage] ran since
     * [sessionVisitorId] was read, which rotates the visitor ID. A refresh that outlived its session must
     * neither persist nor report anything, as both would land on the next session.
     */
    private fun isStorageClearedSince(chat: ChatWithParameters, sessionVisitorId: UUID): Boolean =
        (chat.storage.visitorId != sessionVisitorId).also { cleared ->
            if (cleared) {
                chat.entrails.logger.log(
                    Level.Warning,
                    "Discarding token refresh outcome: storage was cleared while it was in flight"
                )
            }
        }

    private fun reportRefreshFailure(chat: ChatWithParameters, sessionVisitorId: UUID, exception: RuntimeChatException) {
        if (isStorageClearedSince(chat, sessionVisitorId)) return
        chat.chatStateListener?.onChatRuntimeException(exception)
        chat.socketListener.reportState(SocketState.CLOSED)
    }

    private fun persistSuccessfulRefresh(
        chat: ChatWithParameters,
        resolvedBody: TokenRequestBodyWithMeta,
        transactionTokenModel: TransactionTokenModel,
    ) {
        // Malformed 200 (no accessToken) must not wipe cached credentials.
        if (!chat.isImplicitOAuthFlow && transactionTokenModel.thirdParty?.accessToken == null) {
            chat.chatStateListener?.onChatRuntimeException(
                RuntimeChatException.AuthorizationError("Refresh token response did not include a new access token")
            )
            chat.socketListener.reportState(SocketState.CLOSED)
            return
        }
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
        // A refresh_token-grant response only populates thirdParty (no transactionToken/
        // expiresIn) — keep the cached transaction token in that case rather than
        // discarding one that may still be valid. Merge (not replace) thirdParty in every branch
        // so a refresh response that omits refresh_token never discards the cached one, regardless
        // of whether the response happens to also carry a transactionToken (see mergeThirdParty).
        val cachedModel = chat.storage.transactionTokenModel
        val mergedThirdParty = mergeThirdParty(cachedModel?.thirdParty, transactionTokenModel.thirdParty)
        chat.storage.transactionTokenModel = transactionTokenModel.transactionToken
            ?.let { transactionTokenModel.copy(thirdParty = mergedThirdParty) }
            ?: cachedModel?.copy(thirdParty = mergedThirdParty)
            ?: transactionTokenModel.copy(thirdParty = mergedThirdParty)
    }

    private fun resolveTokenRequestBody(chat: ChatWithParameters, sessionVisitorId: UUID): TokenRequestBodyWithMeta? {
        val refreshToken = chat.storage.transactionTokenModel?.thirdParty?.refreshToken
        return when {
            chat.isImplicitOAuthFlow -> resolveImplicitRequestBody(chat, sessionVisitorId)
            refreshToken != null -> TokenRequestBodyWithMeta(
                requestBody = TokenRequestBody(
                    thirdParty = ThirdPartyOAuthBody(grantType = GrantType.REFRESH_TOKEN, refreshToken = refreshToken)
                ),
            )

            else -> {
                reportRefreshFailure(
                    chat,
                    sessionVisitorId,
                    RuntimeChatException.AuthorizationError("Missing refresh token for access token refresh."),
                )
                null
            }
        }
    }

    private fun resolveImplicitRequestBody(chat: ChatWithParameters, sessionVisitorId: UUID): TokenRequestBodyWithMeta? {
        if (chat.tokenDelegateListener == null) {
            reportRefreshFailure(
                chat,
                sessionVisitorId,
                RuntimeChatException.AuthorizationError("Implicit flow requires a TokenDelegateListener."),
            )
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
                // The delegate can block for as long as the integrator likes, so signOut() may have run meanwhile.
                reportRefreshFailure(chat, sessionVisitorId, exception)
                null
            }
        )
    }
}
