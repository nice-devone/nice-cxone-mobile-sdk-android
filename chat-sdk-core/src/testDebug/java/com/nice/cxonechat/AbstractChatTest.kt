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

package com.nice.cxonechat

import com.nice.cxonechat.FakeChatStateListener.ChatStateConnection.Connected
import com.nice.cxonechat.FakeChatStateListener.ChatStateConnection.Initial
import com.nice.cxonechat.FakeChatStateListener.ChatStateConnection.Ready
import com.nice.cxonechat.FakeChatStateListener.ChatStateConnection.UnexpectedDisconnect
import com.nice.cxonechat.exceptions.RuntimeChatException
import com.nice.cxonechat.internal.ChatWithParameters
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.tool.SocketFactoryMock
import com.nice.cxonechat.tool.awaitResult
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

internal abstract class AbstractChatTest : AbstractChatTestSubstrate() {

    protected lateinit var chat: Chat

    protected var chatStateListener = FakeChatStateListener()
    protected val connection: Connection
        get() = (chat as ChatWithParameters).connection

    protected open val authorization
        get() = Authorization.None

    fun buildChat() = awaitResult(100.milliseconds) { finished ->
        val factory = SocketFactoryMock(socket, proxyListener)
        ChatBuilder(entrails, factory)
            .setAuthorization(authorization)
            .setDevelopmentMode(true)
            .setChatStateListener(chatStateListener)
            .build { result: Result<Chat> ->
                chat = result.getOrThrow()
                chat.connect()
                finished(chat)
            }
    }

    override fun prepare() {
        chat = buildChat()
    }

    fun connect() {
        assertEquals(Initial, chatStateListener.connection)
        chat.connect()
        socketServer.open()
    }
}

internal class FakeChatStateListener : ChatStateListener {

    var connection: ChatStateConnection = Initial
    val onChatRuntimeExceptions = mutableListOf<RuntimeChatException>()
    override fun onUnexpectedDisconnect() {
        connection = UnexpectedDisconnect
    }

    override fun onConnected() {
        connection = Connected
    }

    override fun onReady() {
        connection = Ready
    }

    override fun onChatRuntimeException(exception: RuntimeChatException) {
        onChatRuntimeExceptions.add(exception)
    }

    enum class ChatStateConnection {
        Initial,
        UnexpectedDisconnect,
        Connected,
        Ready,
    }
}
