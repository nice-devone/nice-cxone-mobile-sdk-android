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

import com.nice.cxonechat.message.Action
import com.nice.cxonechat.message.Message
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.tool.MockServer
import com.nice.cxonechat.tool.nextString
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@Suppress(
    "FunctionMaxLength",
)
internal class ChatThreadMessageHandlerPolyTest : AbstractChatTest() {

    private lateinit var handler: ChatThreadHandler
    private lateinit var thread: ChatThread

    override fun prepare() {
        super.prepare()
        thread = makeChatThread()
        handler = chat.threads().thread(thread)
    }

    @Test
    fun parses_typeText() {
        val text = nextString()
        val message = awaitMessage(ServerResponse.Message.Text(thread.id, text))
        assertIs<Message.Text>(message)
        assertEquals(text, message.text)
    }

    @Test
    fun parses_typeQuickReplies() {
        val message = awaitMessage(ServerResponse.Message.QuickReplies(thread.id))
        assertIs<Message.QuickReplies>(message)
        assertNotNull(message.title)
        assertNotNull(message.fallbackText)
        assertEquals(3, message.actions.count())
    }

    @Test
    fun parses_typeRichLink() {
        val message = awaitMessage(ServerResponse.Message.RichLink(thread.id))
        assertIs<Message.RichLink>(message)
        assertNotNull(message.fallbackText)
        assertNotNull(message.title)
        assertNotNull(message.url)
        assertNotNull(message.media)
    }

    @Test
    fun parses_typeListPicker() {
        val message = awaitMessage(ServerResponse.Message.ListPicker(thread.id))
        assertIs<Message.ListPicker>(message)
        assertNotNull(message.title)
        assertNotNull(message.text)
        assertNotNull(message.fallbackText)
        assertNotNull(message.actions)

        with(message.actions.toList()) {
            assertEquals(2, size)

            with(this[0]) {
                assertIs<Action.ReplyButton>(this)
                assertNotNull(text)
                assertNotNull(description)
                assertNotNull(media)
                assertNotNull(postback)
            }

            with(this[1]) {
                assertIs<Action.ReplyButton>(this)
                assertNotNull(text)
                assertNull(description)
                assertNull(media)
                assertNull(postback)
            }
        }
    }

    @Test
    fun ignores_unknownContentType() {
        val result = testCallback(::thread) {
            val message = ServerResponse.Message.InvalidContent(thread.id)
            sendServerMessage(ServerResponse.ThreadMetadataLoaded(message))
        }
        assertNull(result)
    }

    // ---

    private fun awaitMessage(message: Any, sender: MockServer.() -> Unit = {}): Message {
        return testCallback(::thread) {
            sendServerMessage(ServerResponse.ThreadMetadataLoaded(message))
            sender()
        }.messages.first().also(::println)
    }

    private fun thread(function: (ChatThread) -> Unit): Cancellable {
        return handler.get { function(it) }
    }
}
