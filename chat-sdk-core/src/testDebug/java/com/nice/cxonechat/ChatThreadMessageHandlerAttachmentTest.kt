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

import android.webkit.MimeTypeMap
import com.nice.cxonechat.api.model.AttachmentUploadResponse
import com.nice.cxonechat.exceptions.InvalidStateException
import com.nice.cxonechat.exceptions.RuntimeChatException.AttachmentUploadError
import com.nice.cxonechat.internal.model.AttachmentModel
import com.nice.cxonechat.internal.model.AttachmentUploadModel
import com.nice.cxonechat.internal.model.AttachmentUploadModelTest
import com.nice.cxonechat.internal.model.ChannelConfiguration
import com.nice.cxonechat.internal.model.ChannelConfiguration.AllowedFileType
import com.nice.cxonechat.internal.model.ChannelConfiguration.FileRestrictions
import com.nice.cxonechat.message.ContentDescriptor
import com.nice.cxonechat.message.OutboundMessage
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.model.makeMessage
import com.nice.cxonechat.server.ServerRequest
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
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class ChatThreadMessageHandlerAttachmentTest : AbstractChatTest() {

    private lateinit var messages: ChatThreadMessageHandler
    private lateinit var thread: ChatThread

    override val config: ChannelConfiguration
        get() = ChannelConfiguration(
            settings = ChannelConfiguration.Settings(
                hasMultipleThreadsPerEndUser = true,
                isProactiveChatEnabled = true,
                fileRestrictions = FileRestrictions(
                    FILE_SIZE,
                    listOf(
                        AllowedFileType(MIME_TYPE, "WTF"),
                        AllowedFileType(MIME_TYPE_WITH_WILDCARD, "Wildcard test"),
                    ),
                    false,
                ),
                features = features
            ),
            isAuthorizationEnabled = true,
            preContactForm = null,
            customerCustomFields = listOf(),
            contactCustomFields = listOf(),
            isLiveChat = isLiveChat,
            availability = mockk {
                every { status } answers { chatAvailability }
            }
        )

    override fun prepare() {
        super.prepare()
        thread = makeChatThread(messages = listOf(makeMessage()), id = TestUUIDValue)
        messages = chat.threads().thread(thread).messages()
    }

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun send_attachments_sendsExpectedMessage() {
        val bytes = Random.nextBytes(32)
        val postback = nextString()
        val expected = Base64.encode(bytes)
        val friendlyName2 = "friendlyName2"
        val attachments = listOf(
            AttachmentModel("url", "friendlyName", mimeType = MIME_TYPE),
            AttachmentModel("url", friendlyName2, mimeType = MIME_TYPE_WILDCARD),
        )
        val call: Call<AttachmentUploadResponse?> = mockCall { AttachmentUploadResponse("url") }
        val filename = "filename"
        val filename2 = "filename2"
        val upload = contentDescriptor(bytes, filename)
        val upload2 = ContentDescriptor(bytes, MIME_TYPE_WILDCARD, filename2, friendlyName2)
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
            messages.send(
                OutboundMessage(
                    attachments = listOf(
                        upload,
                        upload2
                    ),
                    message = expected,
                    postback = postback
                )
            )
        }

        val model = AttachmentUploadModel(upload)

        verify {
            android.util.Base64.encodeToString(eq(bytes), eq(0))
            mimeTypeMap.getExtensionFromMimeType(MIME_TYPE)
            service.uploadFile(model, connection.brandId.toString(), connection.channelId)
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun send_attachment_notifies_about_failure_in_response() {
        val expected = nextString(8)
        val filename = nextString()
        val postback = nextString()
        val bytes = Base64.decode(expected)

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
            exception is AttachmentUploadError,
            "Expected exception of type ${AttachmentUploadError::class.simpleName} but was " +
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
        val expected = nextString(length = 8)
        val filename = nextString()
        val postback = nextString()
        val bytes = Base64.UrlSafe.decode(expected)

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
            exception is AttachmentUploadError,
            "Expected exception of type ${AttachmentUploadError::class.simpleName} but was " +
                    "${exception::class.simpleName}"
        )
        assertEquals(filename, exception.attachmentName)
        assertEquals(ioException, exception.cause)
    }

    @Test
    fun send_too_large_attachment() {
        val bytes = Random.nextBytes(FILE_SIZE * 1024 * 1024 + 1)
        val filename = "file_too_large"
        val upload = contentDescriptor(bytes, filename)
        assertSendsNothing {
            messages.send(OutboundMessage(listOf(upload), ""))
        }
        assertTrue(chatStateListener.onChatRuntimeExceptions.last() is AttachmentUploadError)
    }

    @Test
    fun send_attachment_without_allowed_type() {
        val bytes = Random.nextBytes(FILE_SIZE)
        val filename = "file_of_disallowed_type"
        val upload = contentDescriptor(bytes, filename, "not_allowed_type/application")
        assertSendsNothing {
            messages.send(OutboundMessage(listOf(upload), ""))
        }
        assertTrue(chatStateListener.onChatRuntimeExceptions.last() is AttachmentUploadError)
    }

    private fun mockMimeTypeMap() = mockk<MimeTypeMap> {
        every { getExtensionFromMimeType(any()) } answers {
            arg<String>(0).split("/").last()
        }
        every { getExtensionFromMimeType(AttachmentUploadModelTest.mimeType) } returns "wtf"
    }.also {
        mockkStatic(MimeTypeMap::class)
        every { MimeTypeMap.getSingleton() } returns it
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun mockAndroidBase64() {
        // since android.* classes aren't implemented for unit tests, mock out Base64 conversion
        // to just return a fixed string
        mockkStatic(android.util.Base64::class)
        every { android.util.Base64.encodeToString(any(), any()) } answers { Base64.encode(arg<ByteArray>(0)) }
    }

    companion object {
        private const val FILE_SIZE = 1
        private const val MIME_TYPE = "application/wtf"
        private const val MIME_TYPE_WITH_WILDCARD = "wildcard/*"
        private const val MIME_TYPE_WILDCARD = "wildcard/something"

        private fun contentDescriptor(bytes: ByteArray, filename: String, type: String = MIME_TYPE) =
            ContentDescriptor(bytes, type, filename, "friendlyName")
    }
}
