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
import com.nice.cxonechat.enums.AuthenticationType
import com.nice.cxonechat.exceptions.RuntimeChatException
import com.nice.cxonechat.internal.model.TokenRequestBody
import com.nice.cxonechat.internal.model.TransactionTokenModel
import com.nice.cxonechat.internal.socket.SocketFactory
import com.nice.cxonechat.internal.socket.SocketState
import com.nice.cxonechat.internal.socket.WebsocketLogging
import com.nice.cxonechat.state.Configuration
import com.nice.cxonechat.storage.ValueStorage
import okhttp3.WebSocket

/**
 * Result of a socket connection attempt.
 */
internal data class ChatSocketConnectionResult(
    val webSocket: WebSocket?,
    val transactionTokenModel: TransactionTokenModel?,
    val error: Throwable? = null,
)

/**
 * Handles the API call and WebSocket setup for ChatImpl.
 * No side effects: returns all data needed for ChatImpl to update its state.
 */
internal class ChatSocketConnector(
    private val chat: ChatWithParameters,
    private val socketFactory: SocketFactory,
    private val chatStateListener: ChatStateListener?,
) {
    internal fun connect(tokenRequestBody: TokenRequestBody?): ChatSocketConnectionResult {
        val configuration = chat.configuration
        val entrails = chat.entrails
        val storage = entrails.storage
        val socketListener = chat.socketListener
        val isSecuredSessionsEnabled = configuration.hasFeature(Configuration.Feature.SecuredSessions)
        val transactionTokenModel = storage.transactionTokenModel

        return when {
            // Case 1: Secured sessions enabled and valid transaction token exists
            isSecuredSessionsEnabled && transactionTokenModel?.isExpired == false ->
                connectWithTransactionToken(socketListener, storage, entrails, transactionTokenModel)
            // Case 2: Secured sessions enabled but token expired and using ThirdPartyOAuth - cannot refresh token automatically
            isSecuredSessionsEnabled && transactionTokenModel?.isExpired == true &&
                    chat.configuration.authenticationType == AuthenticationType.ThirdPartyOAuth -> {
                val exception = RuntimeChatException.ConnectionTokenFailed("Expired transaction token")
                chatStateListener?.onChatRuntimeException(exception)
                ChatSocketConnectionResult(
                    webSocket = null,
                    transactionTokenModel = null,
                    error = exception
                )
            }
            // Case 3: Secured sessions enabled, need to fetch new transaction token
            isSecuredSessionsEnabled && tokenRequestBody != null ->
                connectWithNewTransactionToken(tokenRequestBody, socketListener, storage, entrails)
            // Case 4: Secured sessions not enabled, connect without transaction token
            else ->
                connectWithoutSecuredSession(socketListener, storage, entrails)
        }
    }

    private fun connectWithTransactionToken(
        socketListener: com.nice.cxonechat.internal.socket.ProxyWebSocketListener,
        storage: ValueStorage,
        entrails: ChatEntrails,
        transactionTokenModel: TransactionTokenModel,
    ): ChatSocketConnectionResult {
        val webSocket = WebsocketLogging(
            socket = socketFactory.create(
                listener = socketListener,
                visitorId = storage.visitorId.toString(),
                transactionToken = transactionTokenModel.transactionToken
            ).also {
                socketListener.reportState(SocketState.CONNECTED)
            },
            logger = entrails.logger
        )
        return ChatSocketConnectionResult(
            webSocket = webSocket,
            transactionTokenModel = transactionTokenModel,
            error = null
        )
    }

    private fun connectWithNewTransactionToken(
        tokenRequestBody: TokenRequestBody,
        socketListener: com.nice.cxonechat.internal.socket.ProxyWebSocketListener,
        storage: ValueStorage,
        entrails: ChatEntrails,
    ): ChatSocketConnectionResult {
        val connection = chat.connection
        val result = TransactionTokenHelper.getTransactionToken(
            entrails = entrails,
            brandId = connection.brandId.toString(),
            channelId = connection.channelId,
            visitorId = storage.visitorId.toString(),
            tokenRequestBody = tokenRequestBody
        )
        return when (result) {
            is TransactionTokenHelper.TransactionTokenResult.Error -> {
                val exception = RuntimeChatException.AuthorizationError(result.message)
                chatStateListener?.onChatRuntimeException(exception)
                socketListener.reportState(SocketState.CLOSED)
                ChatSocketConnectionResult(
                    webSocket = null,
                    transactionTokenModel = null,
                    error = exception
                )
            }

            is TransactionTokenHelper.TransactionTokenResult.Success -> {
                val newTransactionTokenModel = result.transactionTokenModel
                val transactionToken = newTransactionTokenModel.transactionToken
                val webSocket = WebsocketLogging(
                    socket = socketFactory.create(socketListener, storage.visitorId.toString(), transactionToken).also {
                        socketListener.reportState(SocketState.CONNECTED)
                    },
                    logger = entrails.logger
                )
                ChatSocketConnectionResult(
                    webSocket = webSocket,
                    transactionTokenModel = newTransactionTokenModel,
                    error = null
                )
            }
        }
    }

    private fun connectWithoutSecuredSession(
        socketListener: com.nice.cxonechat.internal.socket.ProxyWebSocketListener,
        storage: ValueStorage,
        entrails: ChatEntrails,
    ): ChatSocketConnectionResult {
        val webSocket = WebsocketLogging(
            socket = socketFactory.create(socketListener, storage.visitorId.toString()).also {
                socketListener.reportState(SocketState.CONNECTED)
            },
            logger = entrails.logger
        )
        return ChatSocketConnectionResult(
            webSocket = webSocket,
            transactionTokenModel = null,
            error = null
        )
    }
}
