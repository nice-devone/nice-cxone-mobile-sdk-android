/*
 * Copyright (c) 2021-2023. NICE Ltd. All rights reserved.
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

import com.nice.cxonechat.FakeChatStateListener.ChatStateConnection.CONNECTED
import com.nice.cxonechat.FakeChatStateListener.ChatStateConnection.INITIAL
import com.nice.cxonechat.FakeChatStateListener.ChatStateConnection.READY
import com.nice.cxonechat.FakeChatStateListener.ChatStateConnection.UNEXPECTED_DISCONNECT
import com.nice.cxonechat.exceptions.RuntimeChatException
import com.nice.cxonechat.internal.ChatWithParameters
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.tool.SocketFactoryMock
import com.nice.cxonechat.tool.awaitResult
import kotlin.time.Duration.Companion.milliseconds

internal abstract class AbstractChatTest : AbstractChatTestSubstrate() {

    protected lateinit var chat: Chat

    protected var chatStateListener = FakeChatStateListener()
    protected val connection: Connection
        get() = (chat as ChatWithParameters).connection

    protected open val authorization
        get() = Authorization.None

    override fun prepare() {
        chat = awaitResult(100.milliseconds) { finished ->
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
    }
}

internal class FakeChatStateListener : ChatStateListener {

    var connection: ChatStateConnection = INITIAL
    val onChatRuntimeExceptions = mutableListOf<RuntimeChatException>()
    override fun onUnexpectedDisconnect() {
        connection = UNEXPECTED_DISCONNECT
    }

    override fun onConnected() {
        connection = CONNECTED
    }

    override fun onReady() {
        connection = READY
    }

    override fun onChatRuntimeException(exception: RuntimeChatException) {
        onChatRuntimeExceptions.add(exception)
    }

    enum class ChatStateConnection {
        INITIAL,
        UNEXPECTED_DISCONNECT,
        CONNECTED,
        READY,
    }
}
