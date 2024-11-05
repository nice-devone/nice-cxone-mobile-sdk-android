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
import com.nice.cxonechat.enums.ContactStatus.Closed
import com.nice.cxonechat.enums.ErrorType.RecoveringLivechatFailed
import com.nice.cxonechat.internal.ChatThreadHandlerLiveChat.Companion.BEGIN_CONVERSATION_MESSAGE
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.AvailabilityStatus.Offline
import com.nice.cxonechat.internal.model.MessageModel
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.model.makeMessageModel
import com.nice.cxonechat.server.ServerRequest
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.thread.ChatThread
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class ChatLiveTest : AbstractChatTest() {

    override fun prepare() {
        isLiveChat = true
        features.clear()
        super.prepare()
    }

    @Test
    fun connect_offline_is_ready() {
        chatAvailability = Offline

        chat = buildChat()

        chat.isChatAvailable shouldBe false
        chatStateListener.connection shouldBe Ready
    }

    @Test
    fun connect_live_chat_recovers_thread() {
        val messages = arrayOf(makeMessageModel())
        val expected = makeChatThread().asCopyable().copy(
            messages = messages.mapNotNull(MessageModel::toMessage),
            contactId = TestContactId
        )
        assertSendTexts(
            ServerRequest.ReconnectConsumer(connection),
            ServerRequest.RecoverLiveChatThread(connection, null), // Connect
            ServerRequest.RecoverLiveChatThread(connection, null), // Refresh thread state on first call
            ServerRequest.RecoverLiveChatThread(connection, expected), // Refresh thread state - forced
        ) {
            connect()
            assertEquals(ChatStateConnection.Connected, chatStateListener.connection)
            val actual = testCallback(::get) {
                sendServerMessage(
                    ServerResponse.LivechatRecovered(thread = expected, messages = messages)
                )
            }
            assertNotNull(actual)
            assertEquals(Ready, chatStateListener.connection)
            assertEquals(expected.copy(), actual.asCopyable().copy())
            chat.threads().thread(actual).refresh()
        }
    }

    @Test
    fun connect_live_chat_fails_to_recover_thread() {
        assertSendTexts(
            ServerRequest.ReconnectConsumer(connection),
            ServerRequest.RecoverLiveChatThread(connection, null), // Connect
            ServerRequest.RecoverLiveChatThread(connection, null), // Refresh thread state on first call
        ) {
            connect()
            assertEquals(ChatStateConnection.Connected, chatStateListener.connection)
            val actual = testCallback(::get) {
                sendServerMessage(
                    ServerResponse.ErrorResponse(RecoveringLivechatFailed.value)
                )
            }
            assertEquals(ChatStateConnection.Connected, chatStateListener.connection)
            assertNull(actual)
        }
    }

    @Test
    fun end_contact() {
        val messages = arrayOf(makeMessageModel())
        val expected = makeChatThread().asCopyable().copy(
            messages = messages.mapNotNull(MessageModel::toMessage),
            contactId = TestContactId
        )
        connect()
        assertEquals(ChatStateConnection.Connected, chatStateListener.connection)
        val actual = testCallback(::get) {
            sendServerMessage(
                ServerResponse.LivechatRecovered(thread = expected, messages = messages)
            )
        }
        assertNotNull(actual)
        assertSendText(ServerRequest.EndContact(connection, expected)) {
            chat.threads().thread(actual).endContact()
        }
    }

    @Test
    fun connect_live_chat_ignores_closed_recovered_thread() {
        val messages = arrayOf(makeMessageModel())
        val recovered = makeChatThread().asCopyable().copy(
            messages = messages.mapNotNull(MessageModel::toMessage),
            contactId = TestContactId
        )
        assertSendTexts(
            ServerRequest.ReconnectConsumer(connection),
            ServerRequest.RecoverLiveChatThread(connection, null), // Connect
            ServerRequest.RecoverLiveChatThread(connection, null), // Refresh thread state on first call
            ServerRequest.SendMessage(connection,
                makeChatThread(TestUUIDValue, ""),
                storage = storage,
                message = BEGIN_CONVERSATION_MESSAGE
            )
        ) {
            connect()
            assertEquals(ChatStateConnection.Connected, chatStateListener.connection)
            val actual = testCallback(::get) {
                sendServerMessage(
                    ServerResponse.LivechatRecovered(thread = recovered, messages = messages, status = Closed)
                )
            }
            assertNotNull(actual)
            assertEquals(Ready, chatStateListener.connection)
            assertNotEquals(recovered, actual)
        }
    }

    private fun get(listener: (ChatThread?) -> Unit): Cancellable =
        chat.threads().threads(listener = { threadList -> listener(threadList.firstOrNull()) })
}
