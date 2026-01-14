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
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.debug
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.util.UUIDProvider

internal class ChatAuthorization(
    private val origin: ChatWithParameters,
    private val authorization: Authorization,
) : ChatWithParameters by origin, LoggerScope by LoggerScope("ChatAuthorization", origin.entrails.logger) {

    private val delayedEventHandler: DelayUnauthorizedEventHandler =
        DelayUnauthorizedEventHandler(ChatEventHandlerImpl(this), this)

    private var cancellables = Cancellable.noop

    init {
        if (storage.customerId == null) {
            connection = connection.asCopyable().copy(customerId = UUIDProvider.next().toString())
        }
        origin.eventHandlerProvider = ChatEventHandlerProvider { chat ->
            var handler: ChatEventHandler = delayedEventHandler
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

    private fun getCustomerAuthorized(): Cancellable = scope("EventCustomerAuthorized") {
        origin.socketListener.addCallback(EventCustomerAuthorized) { model ->
            debug("Customer authorized")
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
            delayedEventHandler.triggerDelayedEvents()
        }
    }

    private fun getTokenRefresh(): Cancellable = scope("EventTokenRefreshed") {
        socketListener.addCallback(EventTokenRefreshed) { model ->
            debug("Token refreshed")
            storage.authToken = model.token
            storage.authTokenExpDate = model.expiresAt
            delayedEventHandler.triggerDelayedEvents()
        }
    }

    private fun getCustomerReconnectFailed() = socketListener.addErrorCallback(ConsumerReconnectionFailed) {
        chatStateListener?.onChatRuntimeException(AuthorizationError("Failed to reconnect authorized customer."))
    }

    private fun getTokenRefreshFailed() = socketListener.addErrorCallback(TokenRefreshingFailed) {
        chatStateListener?.onChatRuntimeException(AuthorizationError("Failed to refresh authorization token."))
    }

    private fun authorizeCustomer() = scope("authorizeCustomer") {
        debug("Authorizing customer...")
        delayedEventHandler.delayEvents()
        val event = when (storage.authToken == null) {
            true -> AuthorizeCustomerEvent(authorization.code, authorization.verifier)
            else -> ReconnectCustomerEvent
        }
        debug("Triggering authorization event")
        events().trigger(event)
    }

    override fun close() {
        delayedEventHandler.delayEvents()
        cancellables.cancel()
        origin.close()
    }

    override fun connect(): Cancellable = scope("connect") {
        debug("Connecting with authorization")
        cancellables.cancel()
        cancellables = registerCallbacks()
        origin.connect().let {
            authorizeCustomer()
            Cancellable(it, cancellables)
        }
    }
}
