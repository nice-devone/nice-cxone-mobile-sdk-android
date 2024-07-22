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

import com.google.gson.JsonObject
import com.google.gson.JsonSerializer
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.MessageModel
import com.nice.cxonechat.internal.model.network.MessagePolyContent.Noop
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.model.makeMessage
import com.nice.cxonechat.model.makeMessageModel
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.thread.ChatThread
import org.junit.Test
import java.util.Date
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class ChatThreadHandlerMessageReadByAgentTest : AbstractChatTest() {

    private lateinit var message: MessageModel
    private lateinit var messages: ChatThreadMessageHandler
    private lateinit var chatThread: ChatThread
    private lateinit var thread: ChatThreadHandler

    override fun prepare() {
        super.prepare()
        val threadId = UUID.randomUUID()
        message = makeMessageModel(threadIdOnExternalPlatform = threadId)
        chatThread = makeChatThread(messages = listOf(makeMessage(message)), id = threadId)
        messages = chat.threads().thread(chatThread).messages()
        thread = chat.threads().thread(chatThread)
    }

    @Test
    fun read_event_updates_message_in_thread() {
        val expected = chatThread.asCopyable().copy(
            messages = listOf(
                makeMessage(
                    message.copy(
                        userStatistics = message.userStatistics.copy(readAt = Date(0))
                    )
                )
            )
        )
        val actual = testCallback(::get) {
            sendServerMessage(ServerResponse.MessageReadChanged(message))
        }
        assertEquals(expected, actual.asCopyable().copy())
    }

    @Test
    fun read_event_from_other_thread_is_ignored() {
        val actual = testCallback(::get) {
            sendServerMessage(ServerResponse.MessageReadChanged(makeMessageModel()))
        }
        assertNull(actual)
    }

    @Test
    fun read_event_without_message_is_ignored() {
        val serializer = JsonSerializer<Noop> { _, _, _ ->
            JsonObject().apply {
                addProperty("type", "noop")
            }
        }
        val pair = Noop::class.java to serializer
        val actual = testCallback(::get) {
            sendServerMessage(
                ServerResponse.MessageReadChanged(
                    message = message.copy(messageContent = Noop),
                    temporaryTypeAdapters = mapOf(pair)
                )
            )
        }
        assertNull(actual)
    }

    private fun get(listener: (ChatThread) -> Unit): Cancellable =
        thread.get(listener = { listener(it) })
}
