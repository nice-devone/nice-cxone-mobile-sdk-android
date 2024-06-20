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

import com.nice.cxonechat.FakeChatStateListener.ChatStateConnection
import com.nice.cxonechat.FakeChatStateListener.ChatStateConnection.Ready
import com.nice.cxonechat.enums.ErrorType.RecoveringLivechatFailed
import com.nice.cxonechat.internal.ChatThreadHandlerLiveChat.Companion.BEGIN_CONVERSATION_MESSAGE
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.server.ServerRequest
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.state.Configuration
import com.nice.cxonechat.thread.ChatThread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class ChatLiveLegacyTest : AbstractChatTest() {

    override fun prepare() {
        isLiveChat = true
        features[Configuration.Feature.RecoverLiveChatDoesNotFail.key] = false
        super.prepare()
    }

    @Test
    fun connect_live_chat_fails_to_recover_thread_legacy() {
        assertSendTexts(
            ServerRequest.ReconnectConsumer(connection),
            ServerRequest.RecoverLiveChatThread(connection, null), // First connect
            ServerRequest.RecoverLiveChatThread(connection, null), // Second connect - prepare
            ServerRequest.SendMessage(
                connection,
                makeChatThread(TestUUIDValue, ""),
                storage = storage,
                message = BEGIN_CONVERSATION_MESSAGE
            )
        ) {
            connect()
            assertEquals(ChatStateConnection.Connected, chatStateListener.connection)
            val actual = testCallback(::get) {
                sendServerMessage(
                    ServerResponse.ErrorResponse(RecoveringLivechatFailed.value)
                )
            }
            assertEquals(Ready, chatStateListener.connection)
            assertNotNull(actual)
        }
    }

    private fun get(listener: (ChatThread?) -> Unit): Cancellable =
        chat.threads().threads(listener = { threadList -> listener(threadList.firstOrNull()) })
}
