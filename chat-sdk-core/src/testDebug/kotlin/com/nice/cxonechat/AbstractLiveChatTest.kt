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

import com.nice.cxonechat.internal.model.AvailabilityStatus.Online
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.tool.SocketFactoryMock
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Base class for tests that operate in LiveChat mode ([isLiveChat] = true).
 *
 * For **online** mode: [buildChat] launches connect eagerly (so the socket session is
 * initialised before the first suspension point), but does not drive it to completion.
 * Call [connect] to open the socket and send ConsumerAuthorized; then drive the recovery
 * sequence by sending [com.nice.cxonechat.server.ServerResponse.LivechatRecovered] or an error.
 *
 * For **offline** mode: [buildChat] completes the connect synchronously (no WebSocket exchange
 * is needed). Calling [connect] afterwards starts a fresh reconnect.
 */
internal abstract class AbstractLiveChatTest : AbstractChatTest() {

    init {
        isLiveChat = true
    }

    private var pendingConnectJob: Job? = null

    @Suppress("DEPRECATION")
    fun buildChat(): Chat = runBlocking {
        val factory = SocketFactoryMock(socket, proxyListener)
        chat = ChatBuilder(entrails, factory)
            .setAuthorization(authorization)
            .setDevelopmentMode(true)
            .setChatStateListener(chatStateListener)
            .build()
        if (chatAvailability != Online) {
            // Offline: connect completes synchronously with no WebSocket exchange.
            testScope.launch { chat.connect() }
        } else {
            // Online: launch connect eagerly so the socket session is initialised before
            // the first suspension point; connect() drives the pending job forward.
            pendingConnectJob = testScope.launch { chat.connect() }
        }
        chat
    }

    /**
     * Drives the pending connect forward (online initial connect) or starts a fresh connect
     * (offline mode or reconnect after disconnect). After this call, tests send
     * LivechatRecovered / error to complete the sequence.
     */
    fun connect() {
        if (pendingConnectJob == null) {
            testScope.launch { chat.connect() }
        }
        pendingConnectJob = null
        socketServer.open()
        socketServer.sendServerMessage(
            ServerResponse.ConsumerAuthorized(
                firstName = SocketFactoryMock.firstName,
                lastName = SocketFactoryMock.lastName
            )
        )
    }

    override fun prepare() {
        pendingConnectJob = null
        chat = buildChat()
    }
}
