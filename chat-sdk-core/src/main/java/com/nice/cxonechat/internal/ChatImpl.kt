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

import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.ChatActionHandler
import com.nice.cxonechat.ChatEventHandler
import com.nice.cxonechat.ChatFieldHandler
import com.nice.cxonechat.ChatMode.LiveChat
import com.nice.cxonechat.ChatMode.MultiThread
import com.nice.cxonechat.ChatMode.SingleThread
import com.nice.cxonechat.ChatStateListener
import com.nice.cxonechat.ChatThreadsHandler
import com.nice.cxonechat.enums.AuthenticationType
import com.nice.cxonechat.event.PageViewEvent
import com.nice.cxonechat.internal.copy.ConnectionCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.ChatImplDependencies
import com.nice.cxonechat.internal.model.ConfigurationInternal
import com.nice.cxonechat.internal.model.GrantType
import com.nice.cxonechat.internal.model.ThirdPartyOAuthBody
import com.nice.cxonechat.internal.model.TokenRequestBody
import com.nice.cxonechat.internal.model.Visitor
import com.nice.cxonechat.internal.model.asCustomerIdentity
import com.nice.cxonechat.internal.socket.ProxyWebSocketListener
import com.nice.cxonechat.internal.socket.SocketState
import com.nice.cxonechat.internal.socket.WebSocketSpec
import com.nice.cxonechat.state.Configuration.Feature
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.thread.CustomField
import okhttp3.WebSocket
import java.util.concurrent.atomic.AtomicReference

@Suppress("TooManyFunctions")
internal class ChatImpl(
    override var connection: Connection,
    override val entrails: ChatEntrails,
    private val dependencies: ChatImplDependencies,
    override val configuration: ConfigurationInternal,
    override val chatStateListener: ChatStateListener?,
) : ChatWithParameters, AutoCloseable {

    private val socketFactory get() = dependencies.socketFactory
    private val callback get() = dependencies.callback
    private val authorization get() = dependencies.authorization

    override val socketListener: ProxyWebSocketListener = socketFactory.createProxyListener()

    override val socket: WebSocket?
        get() = socketSession.get()

    override var fields = listOf<CustomField>()
    override val environment get() = entrails.environment

    private val actions = ChatActionHandlerImpl(this)

    private val socketSession: AtomicReference<WebSocket?> = AtomicReference(null)

    override var lastPageViewed: PageViewEvent? = null

    override var isChatAvailable: Boolean = true

    override var eventHandlerProvider = ChatEventHandlerProvider()

    private val retryApiHandler = RetryApiHandler(maxRetries = 2, retryIntervalMs = 30_000L)

    override fun setDeviceToken(token: String?) {
        val currentToken = entrails.storage.deviceToken
        if (currentToken == token) return
        entrails.storage.deviceToken = token
        entrails.threading.background {
            sendVisitorInfo(token)
        }
    }

    private fun sendVisitorInfo(token: String?) {
        val createOrUpdateVisitor = entrails.service.createOrUpdateVisitor(
            brandId = connection.brandId,
            visitorId = entrails.storage.visitorId.toString(),
            visitor = Visitor(connection, deviceToken = token)
        )
        val params = createVisitorRetryParams(createOrUpdateVisitor, callback, chatStateListener)
        retryApiHandler.executeWithRetry(params.action, params.onSuccess, params.onFailure)
    }

    private fun cancelVisitorHandle() {
        retryApiHandler.cancel()
    }

    override fun threads(): ChatThreadsHandler {
        var handler: ChatThreadsHandler
        handler = ChatThreadsHandlerImpl(this, configuration.preContactSurvey)
        handler = ChatThreadsHandlerReplayLastEmpty(handler)
        handler = when (chatMode) {
            SingleThread -> ChatThreadsHandlerSingle(this, handler)
            MultiThread -> ChatThreadsHandlerMulti(this, handler)
            LiveChat -> ChatThreadsHandlerLive(this, handler)
        }
        handler = ChatThreadsHandlerConfigProxy(handler, this)
        handler = ChatThreadsHandlerMessages(handler)
        handler = ChatThreadsHandlerMemoizeHandlers(handler)
        return handler
    }

    override fun events(): ChatEventHandler = eventHandlerProvider.events(this)

    override fun customFields(): ChatFieldHandler = ChatFieldHandlerGlobal(this)

    override fun actions(): ChatActionHandler = actions

    override fun signOut() {
        storage.clearStorage()
        cookieJar.clearAllCookies()
        close()
    }

    override fun close() {
        cancelVisitorHandle()
        socketSession.getAndSet(null)?.close(WebSocketSpec.CLOSE_NORMAL_CODE, null)
    }

    override fun connect(): Cancellable {
        socketListener.reportState(SocketState.CONNECTING)
        chatStateListener?.onConnecting()
        val authenticationType = configuration.authenticationType
        val isSecuredSessionsEnabled = configuration.hasFeature(Feature.SecuredSessions)
        val tokenRequestBody = if (isSecuredSessionsEnabled) createAuthRequestBody(authenticationType) else null
        val connector = ChatSocketConnector(
            chat = this,
            socketFactory = socketFactory,
            chatStateListener = chatStateListener
        )
        val result = connector.connect(tokenRequestBody)
        if (result.error != null) {
            // Error already reported by connector
            return Cancellable.noop
        }
        if (isSecuredSessionsEnabled) {
            val customerId = result.transactionTokenModel?.customerIdentity?.idOnExternalPlatform
            storage.authToken = result.transactionTokenModel?.thirdParty?.accessToken
            storage.authTokenExpDate = result.transactionTokenModel?.thirdParty?.expiresAt
            storage.transactionTokenModel = result.transactionTokenModel
            connection = connection.asCopyable().copy(customerId = customerId)
            storage.customerId = customerId
        }
        socketSession.set(result.webSocket)
        return Cancellable.noop
    }

    internal fun createAuthRequestBody(authenticationType: AuthenticationType): TokenRequestBody {
        return when (authenticationType) {
            AuthenticationType.SecuredCookie -> TokenRequestBody()
            AuthenticationType.Anonymous -> TokenRequestBody(
                type = authenticationType.name,
                customerIdentity = connection.customerId?.let { connection.asCustomerIdentity() }
            )

            AuthenticationType.ThirdPartyOAuth -> TokenRequestBody(
                thirdParty = ThirdPartyOAuthBody(
                    grantType = GrantType.AUTHORIZATION_CODE,
                    authorizationCode = authorization.code,
                    codeVerifier = authorization.verifier
                )
            )
        }
    }

    override fun setUserName(firstName: String, lastName: String) {
        if (!configuration.isAuthorizationEnabled) {
            connection = connection.asCopyable().copy(firstName = firstName, lastName = lastName)
        }
    }

    override fun getChannelAvailability(callback: (Boolean) -> Unit): Cancellable {
        callback(isChatAvailable)
        return Cancellable.noop
    }
}
