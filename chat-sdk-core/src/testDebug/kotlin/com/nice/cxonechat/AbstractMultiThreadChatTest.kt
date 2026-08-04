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

import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.tool.SocketFactoryMock
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Base class for tests that operate in MultiThread mode (the default channel configuration).
 *
 * [buildChat] fully connects the chat (open socket + ConsumerAuthorized) before returning.
 * Use [reconnect] to simulate a reconnect in tests that need to observe reconnection behavior.
 */
internal abstract class AbstractMultiThreadChatTest : AbstractChatTest() {

    @Suppress("DEPRECATION")
    fun buildChat(): Chat = runBlocking {
        val factory = SocketFactoryMock(socket, proxyListener)
        chat = ChatBuilder(entrails, factory)
            .setAuthorization(authorization)
            .setDevelopmentMode(true)
            .setChatStateListener(chatStateListener)
            .build()
        val connectJob = testScope.launch { chat.connect() }
        socketServer.open()
        socketServer.sendServerMessage(
            ServerResponse.ConsumerAuthorized(
                firstName = SocketFactoryMock.firstName,
                lastName = SocketFactoryMock.lastName
            )
        )
        connectJob.join()
        chat
    }

    /**
     * Simulates a reconnect. The initial connect was already completed by [buildChat];
     * calling this starts a new connect() and drives it through ConsumerAuthorized.
     */
    fun reconnect() {
        testScope.launch { chat.connect() }
        socketServer.open()
        socketServer.sendServerMessage(
            ServerResponse.ConsumerAuthorized(
                firstName = SocketFactoryMock.firstName,
                lastName = SocketFactoryMock.lastName
            )
        )
    }

    override fun prepare() {
        chat = buildChat()
    }
}
