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

import com.nice.cxonechat.FakeChatStateListener.ChatStateConnection
import com.nice.cxonechat.enums.ErrorType.RecoveringThreadFailed
import com.nice.cxonechat.exceptions.RuntimeChatException
import com.nice.cxonechat.internal.model.ChannelConfiguration
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.server.ServerRequest
import com.nice.cxonechat.server.ServerResponse
import org.junit.Test
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

internal class ChatSingleThreadTest : AbstractChatTest() {

    private var hasMultipleThreadsPerEndUser: Boolean = false

    override val config: ChannelConfiguration?
        get() {
            val config = super.config
            return config?.copy(
                settings = config.settings.copy(hasMultipleThreadsPerEndUser = hasMultipleThreadsPerEndUser)
            )
        }

    @BeforeTest
    fun setup() {
        hasMultipleThreadsPerEndUser = false
    }

    @Test
    fun chat_is_ready_on_no_thread() {
        prepare()
        connect()
        serverResponds(ServerResponse.ErrorResponse(RecoveringThreadFailed.value))
        assertEquals(ChatStateConnection.Ready, chatStateListener.connection)
    }

    @Test
    fun chat_attempts_to_recover_thread() {
        val thread = makeChatThread()
        assertSendTexts(
            ServerRequest.ReconnectConsumer(connection),
            ServerRequest.RecoverThread(connection, null),
        ) {
            connect()
            assertEquals(ChatStateConnection.Connected, chatStateListener.connection)
            serverResponds(ServerResponse.ThreadRecovered(thread = thread))
            assertEquals(ChatStateConnection.Ready, chatStateListener.connection)
        }
    }

    @Test
    fun serverCommunicationError_notReported_inSingleThread_onRecoveringThreadFailed() {
        prepare()
        connect()
        serverResponds(ServerResponse.ErrorResponse(RecoveringThreadFailed.value))
        // Assert that ServerCommunicationError is NOT reported
        val errors = chatStateListener.onChatRuntimeExceptions
        assert(errors.none { it is RuntimeChatException.ServerCommunicationError })
    }

    /**
     * Verify that MultiThread mode is not affected by the filtering implemented for SingleThread mode.
     */
    @Test
    fun serverCommunicationError_reported_inMultiThread_onRecoveringThreadFailed() {
        hasMultipleThreadsPerEndUser = true
        prepare()
        connect()
        serverResponds(ServerResponse.ErrorResponse(RecoveringThreadFailed.value))
        // Assert that ServerCommunicationError IS reported
        val errors = chatStateListener.onChatRuntimeExceptions
        assert(errors.any { it is RuntimeChatException.ServerCommunicationError })
    }
}
