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
import com.nice.cxonechat.api.AuthService
import com.nice.cxonechat.internal.model.ConfigurationInternal
import com.nice.cxonechat.internal.socket.SocketFactory
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.storage.ValueStorage
import io.mockk.every
import io.mockk.mockk
import okhttp3.WebSocket
import org.junit.Before

/**
 * Shared fixtures for the [ChatSocketConnector] test classes: the mocked chat/socket/auth graph
 * and the [Before] setup that wires it together and constructs the [connector] under test.
 */
internal abstract class AbstractChatSocketConnectorTest {
    protected lateinit var chat: ChatWithParameters
    protected lateinit var socketFactory: SocketFactory
    protected lateinit var chatStateListener: ChatStateListener
    protected lateinit var connector: ChatSocketConnector
    protected lateinit var webSocket: WebSocket
    protected lateinit var logger: Logger
    protected lateinit var entrails: ChatEntrails
    protected lateinit var storage: ValueStorage
    protected lateinit var authService: AuthService
    protected lateinit var configuration: ConfigurationInternal
    protected lateinit var connection: Connection

    @Before
    fun setUp() {
        chat = mockk(relaxed = true)
        socketFactory = mockk(relaxed = true)
        chatStateListener = mockk(relaxed = true)
        webSocket = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        entrails = mockk(relaxed = true)
        storage = mockk(relaxed = true)
        authService = mockk(relaxed = true)
        configuration = mockk(relaxed = true)
        connection = mockk(relaxed = true)

        every { chat.configuration } returns configuration
        every { chat.connection } returns connection
        every { chat.entrails } returns entrails
        every { chat.socketListener } returns mockk(relaxed = true)
        every { entrails.storage } returns storage
        every { entrails.logger } returns logger
        every { entrails.authService } returns authService
        connector = ChatSocketConnector(chat, socketFactory, chatStateListener)
    }
}
