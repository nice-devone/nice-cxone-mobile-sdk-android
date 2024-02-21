/*
 * Copyright (c) 2021-2023. NICE Ltd. All rights reserved.
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

import android.util.Base64
import android.webkit.MimeTypeMap
import com.nice.cxonechat.ChatThreadMessageHandler.OnMessageTransferListener
import com.nice.cxonechat.api.model.AttachmentUploadResponse
import com.nice.cxonechat.exceptions.InvalidParameterException
import com.nice.cxonechat.exceptions.InvalidStateException
import com.nice.cxonechat.exceptions.RuntimeChatException
import com.nice.cxonechat.internal.model.AttachmentModel
import com.nice.cxonechat.internal.model.AttachmentUploadModel
import com.nice.cxonechat.message.ContentDescriptor
import com.nice.cxonechat.message.OutboundMessage
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.model.makeMessage
import com.nice.cxonechat.server.ServerRequest
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.tool.nextString
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.Call
import retrofit2.Response
import java.io.IOException
import java.util.UUID
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.io.encoding.Base64 as KotlinBase64

internal class ChatThreadMessageHandlerTest : AbstractChatTest() {

    private lateinit var messages: ChatThreadMessageHandler
    private lateinit var thread: ChatThread

    override fun prepare() {
        super.prepare()
        thread = makeChatThread(messages = listOf(makeMessage()), id = TestUUIDValue)
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

    private fun mockMimeTypeMap() = mockk<MimeTypeMap> {
        every { getExtensionFromMimeType(mimeType) } returns "wtf"
    }.also {
        mockkStatic(MimeTypeMap::class)
        every { MimeTypeMap.getSingleton() } returns it
    }

    @Test(expected = InvalidParameterException::class)
    fun send_empty_message_throws() {
        messages.send(OutboundMessage(""))
    }

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun send_attachments_sendsExpectedMessage() {
        val bytes = Random.nextBytes(32)
        val postback = nextString()
        val expected = KotlinBase64.encode(bytes)
        val attachments = listOf(AttachmentModel("url", "friendlyName", mimeType = mimeType))
        val call: Call<AttachmentUploadResponse?> = mockCall { AttachmentUploadResponse("url") }
        val filename = "filename"
        val upload = contentDescriptor(bytes, filename)
        val mimeTypeMap = mockMimeTypeMap()

        mockAndroidBase64()

        every { service.uploadFile(any(), any(), any()) } returns call

        assertSendText(
            ServerRequest.SendMessage(
                connection = connection,
                thread = thread,
                storage = storage,
                message = expected,
                attachments = attachments,
                postback = postback
            )
        ) {
            messages.send(OutboundMessage(listOf(upload), expected, postback))
        }

        val model = AttachmentUploadModel(upload)

        verify {
            Base64.encodeToString(eq(bytes), eq(0))
            mimeTypeMap.getExtensionFromMimeType(mimeType)
            service.uploadFile(model, connection.brandId.toString(), connection.channelId)
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun send_attachment_notifies_about_failure_in_response() {
        val expected = nextString()
        val filename = nextString()
        val postback = nextString()
        val bytes = KotlinBase64.decode(expected)

        mockAndroidBase64()

        assertSendText(
            ServerRequest.SendMessage(
                connection = connection,
                thread = thread,
                storage = storage,
                message = expected,
                attachments = emptyList(),
                postback = postback
            )
        ) {
            val upload = contentDescriptor(bytes, filename)
            every { service.uploadFile(any(), any(), any()) } returns mockk {
                every { execute() } returns Response.error(418, "I am a teapot!".toResponseBody())
            }
            messages.send(OutboundMessage(listOf(upload), expected, postback))
        }
        val exception = chatStateListener.onChatRuntimeExceptions.last()
        assertTrue(
            exception is RuntimeChatException.AttachmentUploadError,
            "Expected exception of type ${RuntimeChatException.AttachmentUploadError::class.simpleName} but was " +
                    "${exception::class.simpleName}"
        )
        assertEquals(filename, exception.attachmentName)
        assertTrue(
            exception.cause is InvalidStateException,
            "Expected exception cause of type ${InvalidStateException::class.simpleName} but was " +
                    "${exception.cause?.javaClass?.kotlin?.simpleName}"
        )
    }

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun send_attachment_notifies_about_failure_in_network_call() {
        val expected = nextString()
        val filename = nextString()
        val postback = nextString()
        val bytes = KotlinBase64.decode(expected)

        mockAndroidBase64()

        val ioException = IOException("This is a test")
        assertSendText(
            ServerRequest.SendMessage(
                connection = connection,
                thread = thread,
                storage = storage,
                message = expected,
                attachments = emptyList(),
                postback = postback
            )
        ) {
            val upload = contentDescriptor(bytes, filename)
            every { service.uploadFile(any(), any(), any()) } returns mockk {
                every { execute() } throws ioException
            }
            messages.send(OutboundMessage(listOf(upload), expected, postback))
        }
        val exception = chatStateListener.onChatRuntimeExceptions.last()
        assertTrue(
            exception is RuntimeChatException.AttachmentUploadError,
            "Expected exception of type ${RuntimeChatException.AttachmentUploadError::class.simpleName} but was " +
                    "${exception::class.simpleName}"
        )
        assertEquals(filename, exception.attachmentName)
        assertEquals(ioException, exception.cause)
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun mockAndroidBase64() {
        // since android.* classes aren't implemented for unit tests, mock out Base64 conversion
        // to just return a fixed string
        mockkStatic(Base64::class)
        every { Base64.encodeToString(any(), any()) } answers { KotlinBase64.encode(arg<ByteArray>(0)) }
    }

    @Test
    fun send_text_respondsWithCallback() {
        val result = testCallback<UUID> { trigger ->
            testSendTextFeedback()
            messages.send(OutboundMessage("message1"), OnMessageTransferListener(onSent = trigger))
        }
        assertNotNull(result)
    }

    @Test
    fun send_text_respondsProcessed_withoutServerInterference() {
        val result = testCallback<UUID> { trigger ->
            messages.send(OutboundMessage("message2"), OnMessageTransferListener(onProcessed = trigger))
        }
        assertNotNull(result)
    }

    @Test
    fun send_text_callbacksRespond_withIdenticalId() {
        var processedId: UUID? = null
        val result = testCallback<UUID> { trigger ->
            testSendTextFeedback()
            val listener = OnMessageTransferListener(
                onProcessed = { processedId = it },
                onSent = trigger
            )
            messages.send(message = OutboundMessage("message3"), listener = listener)
        }
        assertSame(processedId, result)
    }

    companion object {
        private const val mimeType = "application/wtf"

        private fun contentDescriptor(bytes: ByteArray, filename: String) =
            ContentDescriptor(bytes, "application/wtf", filename, "friendlyName")
    }
}
