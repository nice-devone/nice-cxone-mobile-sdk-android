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

import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.ChatActionHandler
import com.nice.cxonechat.ChatEventHandler
import com.nice.cxonechat.ChatFieldHandler
import com.nice.cxonechat.ChatMode.LiveChat
import com.nice.cxonechat.ChatMode.MultiThread
import com.nice.cxonechat.ChatMode.SingleThread
import com.nice.cxonechat.ChatStateListener
import com.nice.cxonechat.ChatThreadsHandler
import com.nice.cxonechat.event.PageViewEvent
import com.nice.cxonechat.internal.copy.ConnectionCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.ConfigurationInternal
import com.nice.cxonechat.internal.model.Visitor
import com.nice.cxonechat.internal.socket.ProxyWebSocketListener
import com.nice.cxonechat.internal.socket.SocketFactory
import com.nice.cxonechat.internal.socket.WebSocketSpec
import com.nice.cxonechat.internal.socket.WebsocketLogging
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.thread.CustomField
import okhttp3.WebSocket
import retrofit2.Callback
import java.util.concurrent.atomic.AtomicReference

@Suppress("TooManyFunctions")
internal class ChatImpl(
    override var connection: Connection,
    override val entrails: ChatEntrails,
    private val socketFactory: SocketFactory,
    override val configuration: ConfigurationInternal,
    private val callback: Callback<Void>,
    override val chatStateListener: ChatStateListener?,
) : ChatWithParameters, AutoCloseable {

    override val socketListener: ProxyWebSocketListener = socketFactory.createProxyListener()

    override val socket: WebSocket?
        get() = socketSession.get()

    override var fields = listOf<CustomField>()
    override val environment get() = entrails.environment

    private val actions = ChatActionHandlerImpl(this)

    private val socketSession: AtomicReference<WebSocket?> = AtomicReference(null)

    override var lastPageViewed: PageViewEvent? = null

    override var isChatAvailable: Boolean = true

    override fun setDeviceToken(token: String?) {
        val currentToken = entrails.storage.deviceToken
        val newToken = token
        if (currentToken == newToken) return
        entrails.storage.deviceToken = newToken
        entrails.service.createOrUpdateVisitor(
            brandId = connection.brandId,
            visitorId = entrails.storage.visitorId.toString(),
            visitor = Visitor(connection, deviceToken = newToken)
        ).enqueue(callback)
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

    override fun events(): ChatEventHandler {
        var handler: ChatEventHandler
        handler = ChatEventHandlerImpl(this)
        handler = ChatEventHandlerTokenGuard(handler, this)
        handler = ChatEventHandlerVisitGuard(handler, this)
        handler = ChatEventHandlerTimeOnPage(handler, this)
        handler = ChatEventHandlerThreading(handler, this)
        return handler
    }

    override fun customFields(): ChatFieldHandler = ChatFieldHandlerGlobal(this)

    override fun actions(): ChatActionHandler = actions

    override fun signOut() {
        storage.clearStorage()
        close()
    }

    override fun close() {
        socketSession.getAndSet(null)?.close(WebSocketSpec.CLOSE_NORMAL_CODE, null)
    }

    @Deprecated("Deprecated in Chat", replaceWith = ReplaceWith("connect()"))
    override fun reconnect() = connect()

    override fun connect(): Cancellable {
        socketSession.set(
            WebsocketLogging(
                socket = socketFactory.create(socketListener),
                logger = entrails.logger,
            )
        )
        return Cancellable.noop
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
