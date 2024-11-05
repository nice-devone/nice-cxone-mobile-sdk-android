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

package com.nice.cxonechat.internal

import com.nice.cxonechat.Authorization
import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.ChatEventHandler
import com.nice.cxonechat.enums.ErrorType.ConsumerReconnectionFailed
import com.nice.cxonechat.enums.ErrorType.TokenRefreshingFailed
import com.nice.cxonechat.event.AuthorizeCustomerEvent
import com.nice.cxonechat.event.ReconnectCustomerEvent
import com.nice.cxonechat.exceptions.RuntimeChatException.AuthorizationError
import com.nice.cxonechat.internal.copy.ConnectionCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.network.EventCustomerAuthorized
import com.nice.cxonechat.internal.model.network.EventTokenRefreshed
import com.nice.cxonechat.internal.socket.ErrorCallback.Companion.addErrorCallback
import com.nice.cxonechat.internal.socket.EventCallback.Companion.addCallback
import com.nice.cxonechat.util.UUIDProvider

internal class ChatAuthorization(
    private val origin: ChatWithParameters,
    private val authorization: Authorization,
) : ChatWithParameters by origin {

    private var cancellables = registerCallbacks()
    private var delayEventsPendingAuthorization = true
    private var delayedEventHandler: DelayUnauthorizedEventHandler =
        DelayUnauthorizedEventHandler(ChatEventHandlerImpl(this), this)

    init {
        if (storage.customerId == null) {
            connection = connection.asCopyable().copy(customerId = UUIDProvider.next().toString())
        }
        authorizeCustomer()
        origin.eventHandlerProvider = ChatEventHandlerProvider { chat ->
            var handler: ChatEventHandler = if (delayEventsPendingAuthorization) delayedEventHandler else ChatEventHandlerImpl(chat)
            handler = ChatEventHandlerTokenGuard(handler, chat)
            handler = ChatEventHandlerVisitGuard(handler, chat)
            handler = ChatEventHandlerTimeOnPage(handler, chat)
            handler = ChatEventHandlerThreading(handler, chat)
            handler
        }
    }

    private fun registerCallbacks() = Cancellable(
        getCustomerAuthorized(),
        getTokenRefresh(),
        getCustomerReconnectFailed(),
        getTokenRefreshFailed()
    )

    private fun getCustomerAuthorized() = origin.socketListener.addCallback(EventCustomerAuthorized) { model ->
        delayEventsPendingAuthorization = false
        val authorizationEnabled = origin.configuration.isAuthorizationEnabled
        connection = connection.asCopyable().copy(
            firstName = if (authorizationEnabled) {
                model.firstName ?: connection.firstName
            } else {
                connection.firstName
            },
            lastName = if (authorizationEnabled) {
                model.lastName ?: connection.lastName
            } else {
                connection.lastName
            },
            customerId = model.id
        )
        storage.authToken = model.token
        storage.authTokenExpDate = model.tokenExpiresAt
        storage.customerId = connection.customerId
        delayedEventHandler.triggerDelayedEvents(!authorizationEnabled)
    }

    private fun getTokenRefresh() = socketListener.addCallback(EventTokenRefreshed) { model ->
        delayEventsPendingAuthorization = false
        storage.authToken = model.token
        storage.authTokenExpDate = model.expiresAt
        delayedEventHandler.triggerDelayedEvents(false)
    }

    private fun getCustomerReconnectFailed() = socketListener.addErrorCallback(ConsumerReconnectionFailed) {
        chatStateListener?.onChatRuntimeException(AuthorizationError("Failed to reconnect authorized customer."))
    }

    private fun getTokenRefreshFailed() = socketListener.addErrorCallback(TokenRefreshingFailed) {
        chatStateListener?.onChatRuntimeException(AuthorizationError("Failed to refresh authorization token."))
    }

    private fun authorizeCustomer() {
        delayEventsPendingAuthorization = true
        val event = when (storage.authToken == null) {
            true -> AuthorizeCustomerEvent(authorization.code, authorization.verifier)
            else -> ReconnectCustomerEvent
        }
        events().trigger(event)
    }

    override fun close() {
        cancellables.cancel()
        origin.close()
    }

    override fun connect(): Cancellable = origin.connect().also {
        cancellables.cancel()
        cancellables = registerCallbacks()
        authorizeCustomer()
    }
}
