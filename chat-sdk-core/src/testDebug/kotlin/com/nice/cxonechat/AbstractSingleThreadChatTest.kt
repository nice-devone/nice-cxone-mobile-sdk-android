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

import com.nice.cxonechat.internal.model.ChannelConfiguration
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.tool.SocketFactoryMock
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Base class for tests that operate in SingleThread mode
 * ([ChannelConfiguration.Settings.hasMultipleThreadsPerEndUser] = false).
 *
 * [buildChat] only constructs the Chat instance; it does not connect.
 * Tests must call [connect] explicitly, then drive the recovery sequence by sending
 * [com.nice.cxonechat.server.ServerResponse.ThreadRecovered] or an error response.
 */
internal abstract class AbstractSingleThreadChatTest : AbstractChatTest() {

    override val config: ChannelConfiguration?
        get() = super.config?.let { base ->
            base.copy(settings = base.settings.copy(hasMultipleThreadsPerEndUser = false))
        }

    @Suppress("DEPRECATION")
    fun buildChat(): Chat = runBlocking {
        val factory = SocketFactoryMock(socket, proxyListener)
        chat = ChatBuilder(entrails, factory)
            .setAuthorization(authorization)
            .setDevelopmentMode(true)
            .setChatStateListener(chatStateListener)
            .build()
        // No connect — tests call connect() explicitly.
        chat
    }

    /**
     * Initial connect for SingleThread mode. Opens the socket and drives auth to ConsumerAuthorized.
     * After this call, tests must send [com.nice.cxonechat.server.ServerResponse.ThreadRecovered]
     * or an error response to complete the connect sequence.
     */
    fun connect() {
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
