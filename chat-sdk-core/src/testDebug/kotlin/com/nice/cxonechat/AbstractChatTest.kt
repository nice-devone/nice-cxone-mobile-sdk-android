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

package com.nice.cxonechat

import com.nice.cxonechat.FakeChatStateListener.ChatStateConnection.Connected
import com.nice.cxonechat.FakeChatStateListener.ChatStateConnection.Initial
import com.nice.cxonechat.FakeChatStateListener.ChatStateConnection.Ready
import com.nice.cxonechat.FakeChatStateListener.ChatStateConnection.UnexpectedDisconnect
import com.nice.cxonechat.exceptions.RuntimeChatException
import com.nice.cxonechat.internal.ChatWithParameters
import com.nice.cxonechat.state.Connection

/**
 * Common base for mode-specific chat test base classes.
 *
 * Provides shared state (`chat`, `chatStateListener`, `connection`, `authorization`) used across
 * all chat tests. Mode-specific setup (connect lifecycle, config overrides) lives in the
 * concrete subclasses:
 * - [AbstractMultiThreadChatTest] — default MultiThread mode
 * - [AbstractSingleThreadChatTest] — SingleThread mode
 * - [AbstractLiveChatTest] — LiveChat mode
 */
internal abstract class AbstractChatTest : AbstractChatTestSubstrate() {

    protected lateinit var chat: Chat

    protected var chatStateListener = FakeChatStateListener()
    protected val connection: Connection
        get() = (chat as ChatWithParameters).connection

    protected open val authorization
        get() = Authorization.None
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
