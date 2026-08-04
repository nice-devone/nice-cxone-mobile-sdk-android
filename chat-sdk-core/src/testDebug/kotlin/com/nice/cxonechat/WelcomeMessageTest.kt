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

import com.nice.cxonechat.internal.ChatThreadHandlerWelcome
import com.nice.cxonechat.internal.ChatWithParameters
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.ChannelConfiguration
import com.nice.cxonechat.internal.model.ChatThreadMutable.Companion.asMutable
import com.nice.cxonechat.internal.model.CustomFieldInternal
import com.nice.cxonechat.message.Message
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.model.makeMessageModel
import com.nice.cxonechat.server.ServerRequest
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.CustomField
import com.nice.cxonechat.tool.SocketFactoryMock
import com.nice.cxonechat.tool.nextString
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
internal class WelcomeMessageTest : AbstractMultiThreadChatTest() {

    private val contactCustomFields = listOf<CustomField>(
        CustomFieldInternal("1", nextString(), Instant.fromEpochMilliseconds(0)),
        CustomFieldInternal("2", nextString(), Instant.fromEpochMilliseconds(0))
    )

    private val enteredCustomerCustomFields = mapOf("testField" to "testValue")

    override val config: ChannelConfiguration
        get() = requireNotNull(super.config)

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
        val id = UUID.randomUUID()
        var thread: ChatThread = makeChatThread(id)
        assertTrue(thread.messages.isEmpty())
        val job = testScope.launch {
            thread = handler.threadFlow.first()
        }
        job.cancel()
        assertNotEquals(id, thread.id)
        val messages = thread.messages
        assertTrue(messages.isNotEmpty())
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
        assertEquals(1, threadFromHandler1.messages.size)
        assertEquals(threadFromHandler1.messages.size, handler2.get().messages.size)
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

    @Test
    fun welcome_job_is_cancelled_when_real_messages_arrive_via_thread_flow() {
        // Regression test: the welcomeTaskCancellation coroutine must cancel prepareWelcomeMessageJob
        // when real messages arrive via threadFlow. With blank storage the job suspends in
        // awaitEventSuspend — if not cancelled, it becomes a dangling coroutine that
        // ChatThreadMutable.close() can no longer reach via resultCallbacks.
        setupWelcomeMessagePersistence() // blank storage → job suspends in awaitEventSuspend

        val mutableThread = makeChatThread().asMutable()
        val originFlow = MutableSharedFlow<ChatThread>(extraBufferCapacity = 1)
        val origin = mockk<ChatThreadHandler>(relaxed = true) {
            every { threadFlow } returns originFlow
            every { get() } answers { mutableThread.snapshot() }
        }
        val handler = ChatThreadHandlerWelcome(
            origin = origin,
            chat = chat as ChatWithParameters,
            mutableThread = mutableThread,
        )
        assertFalse(
            handler.prepareWelcomeMessageJob.isCompleted,
            "Expected welcome job to be active (suspended) after handler creation with blank storage"
        )

        // Emit a thread with >1 messages to trigger cancellation in welcomeTaskCancellation coroutine
        val realMessages = listOf(makeMessageModel(), makeMessageModel()).mapNotNull { it.toMessage() }
        mutableThread += mutableThread.asCopyable().copy(messages = realMessages)
        originFlow.tryEmit(mutableThread.snapshot())
        testScope.advanceUntilIdle()

        assertTrue(
            handler.prepareWelcomeMessageJob.isCancelled,
            "prepareWelcomeMessageJob must be cancelled when real messages arrive via threadFlow"
        )
    }

    @Test
    fun threadFlow_does_not_emit_welcome_when_task_fails_silently() {
        // Simulate safeLaunch swallowing a storage exception: the job completes
        // (isCancelled = false, isCompleted = true) but no WelcomeMessage placeholder was appended.
        // Without the messages.any { it is WelcomeMessage } guard, welcomeNotification would emit
        // spuriously despite no placeholder being present in the thread.
        every { storage.welcomeMessage } throws RuntimeException("Simulated storage failure")

        val handler = chat.threads().create()
        val emissions = mutableListOf<ChatThread>()

        val job = testScope.launch { handler.threadFlow.collect { emissions.add(it) } }
        testScope.advanceUntilIdle()
        job.cancel()

        assertEquals(1, emissions.size, "Only the initial state emission should occur — no welcome notification")
    }

    private fun setupWelcomeMessage(
        message: String,
        customerCustomFields: Map<String, String> = enteredCustomerCustomFields,
    ) {
        setupWelcomeMessagePersistence()
        this serverResponds ServerResponse.WelcomeMessage(message, customerCustomFields)

        // FetchThreadList can be sent as part of ChatMultiThread.connect() during buildChat()
        verify(exactly = if (config.settings.hasMultipleThreadsPerEndUser) 1 else 0) {
            socket.send(match<String> { it.contains("FetchThreadList") })
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
