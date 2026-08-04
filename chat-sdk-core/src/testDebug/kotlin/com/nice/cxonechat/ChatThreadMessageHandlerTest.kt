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

@file:Suppress("FunctionMaxLength")

package com.nice.cxonechat

import com.nice.cxonechat.exceptions.InvalidParameterException
import com.nice.cxonechat.exceptions.RuntimeChatException
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.socket.MESSAGE_WEBSOCKET_SEND_FAILED
import com.nice.cxonechat.message.OutboundMessage
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.model.makeMessage
import com.nice.cxonechat.server.ServerRequest
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.tool.nextString
import io.mockk.every
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.fail

@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatThreadMessageHandlerTest : AbstractMultiThreadChatTest() {

    private lateinit var messages: ChatThreadMessageHandler
    private lateinit var thread: ChatThread

    override fun prepare() {
        super.prepare()
        thread = makeChatThread(messages = listOf(makeMessage()), id = TestUUIDValue)
        setupMessages(thread)
    }

    private fun setupMessages(thread: ChatThread) {
        messages = chat.threads().thread(thread).messages()
    }

    // ---

    @Test
    fun loadMore_sendsExpectedMessage() {
        assertSendText(ServerRequest.LoadMore(connection, thread)) {
            messages.loadMore()
        }
    }

    @Test(expected = AssertionError::class)
    fun loadMore_ignoresLoad() {
        messages = chat.threads().thread(makeChatThread()).messages()
        assertSendText("this is wrong") {
            messages.loadMore()
        }
    }

    @Test
    fun send_text_sendsExpectedMessage() {
        val expected = "hello!"
        assertSendText(ServerRequest.SendMessage(connection, thread, storage, expected)) {
            messages.send(OutboundMessage(expected))
        }
    }

    @Test
    fun send_text_customCustomerFieldsWhenDefined() {
        val fields = mapOf("my-field!" to "my-value?")
        this serverResponds ServerResponse.WelcomeMessage("", fields)
        val expected = "Welcome defined fields!!!"
        assertSendText(
            expected = ServerRequest.SendMessage(connection, thread, storage, expected, fields = fields),
            replaceDate = true,
        ) {
            messages.send(OutboundMessage(expected))
        }
    }

    @Test
    fun send_text_customCustomerFields_merged() {
        val fields1 = mapOf("my-field1" to "my-new-value1")
        val fields2 = mapOf("my-field2" to "my-new-value2")
        this serverResponds ServerResponse.WelcomeMessage("", fields1)
        this serverResponds ServerResponse.WelcomeMessage("", fields2)
        val expected = "Welcome merged fields!!!"
        assertSendText(
            expected = ServerRequest.SendMessage(connection, thread, storage, expected, fields2 + fields1),
            replaceDate = true,
        ) {
            messages.send(OutboundMessage(expected))
        }
    }

    @Test
    fun send_text_customCustomerFields_distinct() {
        val fields1 = mapOf("my-field" to "my-value1")
        val fields2 = mapOf("my-field" to "my-value2")
        this serverResponds ServerResponse.WelcomeMessage("", fields1)
        this serverResponds ServerResponse.WelcomeMessage("", fields2)
        val expected = "Welcome distinct fields!!!"
        assertSendText(
            expected = ServerRequest.SendMessage(connection, thread, storage, expected, fields = fields2),
            replaceDate = true,
        ) {
            messages.send(OutboundMessage(expected))
        }
    }

    @Test
    fun send_text_customCustomerFieldsWhenDefined_once() {
        val fields = mapOf("my-new-field" to "my-value?")
        this serverResponds ServerResponse.WelcomeMessage("", fields)
        val expected = "I seek your presence…"
        assertSendText(
            expected = ServerRequest.SendMessage(connection, thread, storage, expected, fields = fields),
            replaceDate = true,
        ) {
            messages.send(OutboundMessage(expected))
        }
        assertSendText(ServerRequest.SendMessage(connection, thread, storage, expected)) {
            messages.send(OutboundMessage(expected))
        }
    }

    @Test
    fun send_text_with_postback_sendExpectedMessage() {
        val expected = nextString()
        val postback = nextString()
        assertSendText(
            ServerRequest.SendMessage(
                connection = connection,
                thread = thread,
                storage = storage,
                message = expected,
                postback = postback
            )
        ) {
            messages.send(OutboundMessage(expected, postback))
        }
    }

    @Test(expected = InvalidParameterException::class)
    fun send_empty_message_throws() = runTest {
        messages.send(OutboundMessage(""))
    }

    @Test(
        expected = IllegalStateException::class
    )
    fun send_text_to_archived_thread_throws() = runTest {
        super.prepare()
        thread = thread.asCopyable().copy(canAddMoreMessages = false)
        setupMessages(thread)
        assertFalse(thread.canAddMoreMessages)
        testSendTextFeedback()
        messages.send(OutboundMessage("message1"))
        fail("messages.send() should throw ISE")
    }

    /**
     * WebSocket.send()=false propagates as ServerCommunicationError
     * through the full decorator chain instead of silently dropping the message.
     */
    @Test
    fun send_text_when_websocket_send_fails_throws() = runTest {
        every { socket.send(text = any()) } returns false
        val exception = assertFailsWith<RuntimeChatException.ServerCommunicationError> {
            messages.send(OutboundMessage(nextString()))
        }
        assertEquals(MESSAGE_WEBSOCKET_SEND_FAILED, exception.message)
    }

    @Test
    fun `send text returns valid uuid string`() = runTest {
        testSendTextFeedback()
        val id = messages.send(OutboundMessage("hello"))
        assertNotNull(UUID.fromString(id))
    }

    @Test
    fun `send with string and postback sends expected message`() {
        val message = nextString()
        val postback = nextString()
        assertSendText(
            ServerRequest.SendMessage(
                connection = connection,
                thread = thread,
                storage = storage,
                message = message,
                postback = postback
            )
        ) {
            messages.send(message, postback)
        }
    }

    @Test
    fun `send with attachments sends expected message`() {
        testSendTextFeedback()
        val message = nextString()
        assertSendText(ServerRequest.SendMessage(connection, thread, storage, message)) {
            messages.send(message = message)
        }
    }

    @Test
    fun `send with attachments and postback produces expected server request`() {
        val message = nextString()
        val postback = nextString()
        assertSendText(
            ServerRequest.SendMessage(
                connection = connection,
                thread = thread,
                storage = storage,
                message = message,
                postback = postback
            )
        ) {
            messages.send(attachments = emptyList(), message = message, postback = postback)
        }
    }

    @Test
    fun `send with attachments and default postback`() {
        val message = nextString()
        assertSendText(
            ServerRequest.SendMessage(
                connection = connection,
                thread = thread,
                storage = storage,
                message = message,
            )
        ) {
            messages.send(attachments = emptyList(), message = message)
        }
    }

    @Test(expected = InvalidParameterException::class)
    fun `send with empty attachments and defaults throws for empty message`() = runTest {
        messages.send(attachments = emptyList())
    }
}
