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

/**
 * Helper utility for handling access token refresh operations.
 * This class provides a centralized location for token refresh logic
 * used by both [ChatEventHandlerTokenGuard] and [ChatThreadEventHandlerTokenGuard].
 */
internal object TokenRefreshHelper {

    /**
     * Refreshes the access token using the third-party OAuth refresh token flow.
     *
     * @param chat The chat instance with storage and configuration
     */
    internal fun refreshAccessToken(chat: ChatWithParameters) {
        val refreshToken = chat.storage.transactionTokenModel?.thirdParty?.refreshToken
        if (refreshToken == null) {
            chat.chatStateListener?.onChatRuntimeException(
                RuntimeChatException.AuthorizationError("Missing refresh token for access token refresh.")
            )
            chat.socketListener.reportState(SocketState.CLOSED)
            return
        }
        val tokenRequestBody = TokenRequestBody(
            thirdParty = ThirdPartyOAuthBody(
                grantType = GrantType.REFRESH_TOKEN,
                refreshToken = refreshToken
            )
        )
        val result = TransactionTokenHelper.getTransactionToken(
            entrails = chat.entrails,
            brandId = chat.connection.brandId.toString(),
            channelId = chat.connection.channelId,
            visitorId = chat.storage.visitorId.toString(),
            tokenRequestBody = tokenRequestBody
        )
        when (result) {
            is TransactionTokenHelper.TransactionTokenResult.Success -> {
                val transactionTokenModel = result.transactionTokenModel
                val accessToken = transactionTokenModel.thirdParty?.accessToken
                val authTokenExpDate = transactionTokenModel.thirdParty?.expiresAt
                chat.storage.authToken = accessToken
                chat.storage.authTokenExpDate = authTokenExpDate
                chat.storage.transactionTokenModel = transactionTokenModel
            }

            is TransactionTokenHelper.TransactionTokenResult.Error -> {
                chat.chatStateListener?.onChatRuntimeException(RuntimeChatException.AuthorizationError(result.message))
                chat.socketListener.reportState(SocketState.CLOSED)
            }
        }
    }
}
