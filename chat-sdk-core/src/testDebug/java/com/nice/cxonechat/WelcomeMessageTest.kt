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

import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.ChannelConfiguration
import com.nice.cxonechat.internal.model.CustomFieldInternal
import com.nice.cxonechat.internal.model.CustomFieldPolyType.Text
import com.nice.cxonechat.message.Message
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.server.ServerRequest
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.CustomField
import com.nice.cxonechat.tool.SocketFactoryMock
import com.nice.cxonechat.tool.nextString
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Test
import java.util.Date
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

internal class WelcomeMessageTest : AbstractChatTest() {

    private val customerCustomFields = listOf<CustomField>(
        CustomFieldInternal("1", nextString(), Date(0)),
        CustomFieldInternal("2", nextString(), Date(0))
    )
    private val contactCustomFields = listOf<CustomField>(
        CustomFieldInternal("1", nextString(), Date(0)),
        CustomFieldInternal("2", nextString(), Date(0))
    )

    private val enteredCustomerCustomFields = mapOf("testField" to "testValue")

    override val config: ChannelConfiguration
        get() {
            return requireNotNull(super.config).copy(
                contactCustomFields = contactCustomFields.map {
                    Text(it.id, it.value)
                },
                customerCustomFields = customerCustomFields.map {
                    Text(it.id, it.value)
                }
            )
        }

    @Test
    fun get_contains_welcome_message() {
        val welcomeMessage = "This is a simple welcome message"
        setupWelcomeMessage(welcomeMessage)
        assertThreadContainsOnlyThisMessage(welcomeMessage)
    }

    @Test
    fun get_is_notified_about_welcome_message() {
        val welcomeMessage = "This is a different welcome message"
        setupWelcomeMessage(welcomeMessage)
        val handler = chat.threads().create(contactCustomFields.asMap())
        val latch = CountDownLatch(1)
        val id = UUID.randomUUID()
        var thread: ChatThread = makeChatThread(id)
        assert(thread.messages.isEmpty())
        val cancellable = handler.get {
            thread = it
            latch.countDown()
        }
        latch.await(100, TimeUnit.MILLISECONDS)
        cancellable.cancel()
        assertNotEquals(id, thread.id)
        val messages = thread.messages
        assert(messages.isNotEmpty())
        assertEquals(welcomeMessage, (messages[0] as Message.Text).text)
        confirmVerified(socket)
    }

    @Test
    fun send_message_sends_welcome_message_first() {
        val welcomeMessage = nextString()
        setupWelcomeMessage(welcomeMessage)
        val handler = chat.threads().create(contactCustomFields.asMap())
        val firstUserMessage = "Message from user"
        val thread = handler.get().asCopyable().copy(id = TestUUIDValue)
        assertSendTexts(
            expected = arrayOf(
                ServerRequest.SendOutbound(connection, thread, storage, welcomeMessage),
                ServerRequest.SendMessage(connection, thread, storage, firstUserMessage, enteredCustomerCustomFields),
            ),
            replaceDate = true,
        ) {
            handler.messages().send(firstUserMessage)
        }
        verifyOrder {
            socket.send(text = match<String> { it.contains(welcomeMessage) })
            socket.send(text = match<String> { it.contains(firstUserMessage) })
        }
        confirmVerified(socket)
    }

    @Test
    fun welcome_message_is_not_added_to_existing_thread() {
        val welcomeMessage = "This is a welcome message"
        setupWelcomeMessage(welcomeMessage)
        val handler1 = chat.threads().create()
        val threadFromHandler1 = handler1.get()
        val handler2 = chat.threads().thread(threadFromHandler1)
        assert(threadFromHandler1.messages.size == 1)
        assert(threadFromHandler1.messages.size == handler2.get().messages.size)
        confirmVerified(socket)
    }

    @Test
    fun complex_message_is_added_to_thread() {
        val message = "Welcome {{customer.firstName|stranger}}, how was your {{customer.customFields.testField|day}}?"
        val expected = "Welcome ${SocketFactoryMock.firstName}, how was your testValue?"
        setupWelcomeMessage(message)
        assertThreadContainsOnlyThisMessage(expected)
    }

    @Test
    fun create_withCustomParameters_sendsComplexWelcomeMessage_toThread() {
        val message = "Welcome {{customer.firstName|stranger}}, " +
                "how was your {{customer.customFields.testField|day}} " +
                "{{contact.customFields.testField2|'failed test'}}?" +
                "{{fallbackMessage|This unit test has failed.}}"
        val expected = "Welcome ${SocketFactoryMock.firstName}, how was your testValue testValue2?"
        val contactCustomFields = mapOf("testField2" to "testValue2")
        setupWelcomeMessage(message)
        assertThreadContainsOnlyThisMessage(expected, contactCustomFields)
    }

    private fun setupWelcomeMessage(
        message: String,
        customerCustomFields: Map<String, String> = enteredCustomerCustomFields,
    ) {
        setupWelcomeMessagePersistence()
        this serverResponds ServerResponse.WelcomeMessage(message, customerCustomFields)

        verify(exactly = 1) {
            socket.send(match<String> { it.contains("ReconnectConsumer") })
        }
    }

    private fun setupWelcomeMessagePersistence() {
        var backing = ""

        every { storage.welcomeMessage = any() } answers { backing = arg(0) }
        every { storage.welcomeMessage } answers { backing }
    }

    private fun assertThreadContainsOnlyThisMessage(expected: String, customerCustomFields: Map<String, String> = emptyMap()) {
        val handler = chat.threads().create(contactCustomFields.asMap() + customerCustomFields)
        val messages = handler.get().messages
        assertEquals(1, messages.size)
        val message = messages[0]
        assertEquals(expected, (message as Message.Text).text)
        confirmVerified(socket)
    }
}

private fun List<CustomField>.asMap() = associate { it.id to it.value }
