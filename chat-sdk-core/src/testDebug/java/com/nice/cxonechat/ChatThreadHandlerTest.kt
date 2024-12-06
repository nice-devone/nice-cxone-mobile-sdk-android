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

@file:Suppress(
    "FunctionMaxLength",
    "LargeClass"
)

package com.nice.cxonechat

import com.nice.cxonechat.enums.ErrorType
import com.nice.cxonechat.exceptions.RuntimeChatException.ServerCommunicationError
import com.nice.cxonechat.exceptions.InvalidStateException
import com.nice.cxonechat.internal.ChatWithParameters
import com.nice.cxonechat.internal.copy.AgentCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.ChannelConfiguration
import com.nice.cxonechat.internal.model.ChatThreadMutable
import com.nice.cxonechat.internal.model.ChatThreadMutable.Companion.asMutable
import com.nice.cxonechat.internal.model.CustomFieldInternal
import com.nice.cxonechat.internal.model.CustomFieldPolyType.Text
import com.nice.cxonechat.internal.model.MessageModel
import com.nice.cxonechat.message.Message
import com.nice.cxonechat.model.makeAgent
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.model.makeMessage
import com.nice.cxonechat.model.makeMessageModel
import com.nice.cxonechat.model.makeUserStatistics
import com.nice.cxonechat.server.ServerRequest
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState
import com.nice.cxonechat.thread.ChatThreadState.Loaded
import com.nice.cxonechat.thread.ChatThreadState.Pending
import com.nice.cxonechat.thread.CustomField
import com.nice.cxonechat.tool.nextString
import com.nice.cxonechat.util.UUIDProvider
import io.kotest.matchers.shouldBe
import io.mockk.every
import org.junit.Test
import java.util.Date
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

internal class ChatThreadHandlerTest : AbstractChatTest() {

    private lateinit var chatThread: ChatThreadMutable
    private lateinit var thread: ChatThreadHandler

    private val customerCustomFields = listOf<CustomField>(
        CustomFieldInternal("1", nextString(), Date(0)),
        CustomFieldInternal("2", nextString(), Date(0))
    )
    private val contactCustomFields = listOf<CustomField>(
        CustomFieldInternal("1", nextString(), Date(0)),
        CustomFieldInternal("2", nextString(), Date(0))
    )

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

    override fun prepare() {
        super.prepare()
        updateChatThread(makeChatThread())
    }

    // ---

    @Test
    fun setName_sendsExpectedMessage() {
        val id = chatThread.id
        val name = "newName!"
        assertSendText(ServerRequest.UpdateThread(connection, chatThread.asCopyable().copy(threadName = name)), id.toString()) {
            thread.setName(name)
        }
    }

    @Test(expected = InvalidStateException::class)
    fun endContactThrows() {
        assertSendsNothing {
            thread.endContact()
        }
    }

    @Test
    fun endContactSendsExpectedMessage() {
        isLiveChat = true

        val chat = buildChat()

        val threads = chat.threads()
        val currentThread = chatThread.asCopyable().copy(contactId = TestContactId)
        val thread = threads.thread(currentThread)

        assertSendText(ServerRequest.EndContact(connection, currentThread)) {
            thread.endContact()
        }
    }

    @Test
    fun get_observes_moreMessagesLoaded() {
        val id = chatThread.id
        val messages = arrayOf(
            makeMessageModel(threadIdOnExternalPlatform = id),
            makeMessageModel(threadIdOnExternalPlatform = id)
        )
        val scrollToken = nextString()
        val expected = chatThread.asCopyable().copy(
            scrollToken = scrollToken,
            messages = messages.mapNotNull(MessageModel::toMessage).toList() + chatThread.messages
        )
        val actual = testCallback(::get) {
            sendServerMessage(ServerResponse.MoreMessagesLoaded(scrollToken, messages = messages))
        }
        assertEquals(expected, actual)
    }

    @Test
    fun get_ignores_otherThanSelfThread_moreMessagesLoaded() {
        val actual = testCallback(::get) {
            sendServerMessage(ServerResponse.MoreMessagesLoaded(nextString(), makeMessageModel()))
        }
        assertNull(actual)
    }

    @Test
    fun get_updates_existing_messages_moreMassagesLoaded() {
        val id = chatThread.id
        val existingMessage = makeMessage(makeMessageModel(threadIdOnExternalPlatform = id))
        chatThread = chatThread.asCopyable().copy(messages = listOf(existingMessage)).asMutable()
        val messages = arrayOf(
            makeMessageModel(threadIdOnExternalPlatform = id),
            makeMessageModel(threadIdOnExternalPlatform = id, idOnExternalPlatform = existingMessage.id)
        )
        val scrollToken = nextString()
        val expected = chatThread.asCopyable().copy(
            scrollToken = scrollToken,
            messages = messages.mapNotNull(MessageModel::toMessage).toList()
        )
        val actual = testCallback(::get) {
            sendServerMessage(ServerResponse.MoreMessagesLoaded(scrollToken, messages = messages))
        }
        assertEquals(expected, actual)
    }

    @Test
    fun get_observes_threadMetadataLoaded() {
        val id = chatThread.id
        val agent = makeAgent()
        val message = makeMessageModel(threadIdOnExternalPlatform = id)
        val expected = chatThread.asCopyable().copy(
            messages = listOfNotNull(message.toMessage()),
            threadAgent = agent.toAgent(),
            threadState = Loaded,
        )
        val actual = testCallback(::get) {
            sendServerMessage(ServerResponse.ThreadMetadataLoaded(agent, message))
        }
        assertEquals(expected, actual)
    }

    @Test
    fun get_ignores_otherThanSelfThread_threadMetadataLoaded() {
        val actual = testCallback(::get) {
            sendServerMessage(ServerResponse.ThreadMetadataLoaded())
        }
        assertNull(actual)
    }

    @Test
    fun get_observes_messageCreated() {
        val id = chatThread.id
        val messageModel = makeMessageModel(
            threadIdOnExternalPlatform = id
        )
        val expected = chatThread.asCopyable().copy(
            contactId = TestContactId,
            messages = listOfNotNull(messageModel.toMessage())
        )
        val actual = testCallback(::get) {
            sendServerMessage(ServerResponse.MessageCreated(chatThread, messageModel))
        }
        assertEquals(expected, actual)
    }

    @Test
    fun get_ignores_duplicit_messageCreated() {
        val id = chatThread.id
        val messageModel = makeMessageModel(
            threadIdOnExternalPlatform = id
        )
        val message = messageModel.toMessage()
        assertNotNull(message)
        updateChatThread(chatThread.asCopyable().copy(messages = listOf(message), contactId = TestContactId))
        val actual = testCallback(::get) {
            sendServerMessage(ServerResponse.MessageCreated(chatThread, messageModel))
        }
        assertNull(actual)
    }

    @Test
    fun get_observes_updated_messageCreated() {
        val id = chatThread.id
        val messageModel = makeMessageModel(
            threadIdOnExternalPlatform = id
        )
        val message = messageModel.toMessage()
        assertNotNull(message)
        updateChatThread(chatThread.asCopyable().copy(messages = listOf(message)))
        val updatedMessage = messageModel.copy(
            userStatistics = makeUserStatistics(seenAt = Date(0))
        )
        val expected = chatThread.asCopyable().copy(
            contactId = TestContactId,
            messages = listOfNotNull(updatedMessage.toMessage())
        )
        val actual = testCallback(::get) {
            sendServerMessage(ServerResponse.MessageCreated(chatThread, updatedMessage))
        }
        assertEquals(expected, actual)
    }

    @Test
    fun get_ignores_otherThanSelfThread_messageCreated() {
        val actual = testCallback(::get) {
            sendServerMessage(ServerResponse.MessageCreated(makeChatThread(), makeMessageModel()))
        }
        assertNull(actual)
    }

    @Test
    fun get_observes_threadRecovered() {
        val id = chatThread.id
        val initialMessage = makeMessageModel(threadIdOnExternalPlatform = id)
        updateChatThread(
            chatThread.asCopyable().copy(messages = chatThread.messages.plus(makeMessage(initialMessage)))
        )
        val messages = arrayOf(
            makeMessageModel(threadIdOnExternalPlatform = id),
            makeMessageModel(threadIdOnExternalPlatform = id)
        )
        val agent = makeAgent()
        val scrollToken = "scrollToken"
        val expected = chatThread.asCopyable().copy(
            scrollToken = scrollToken,
            messages = messages.mapNotNull(MessageModel::toMessage)
                .plus(chatThread.messages)
                .sortedBy(Message::createdAt),
            threadAgent = agent.toAgent(),
            fields = contactCustomFields,
        )
        val actual = testCallback(::get) {
            sendServerMessage(
                ServerResponse.ThreadRecovered(
                    scrollToken = scrollToken,
                    thread = expected,
                    agent = agent,
                    customerCustomFields = customerCustomFields,
                    messages = messages
                )
            )
        }
        assertEquals<ChatThread>(expected, actual)
        assertEquals(customerCustomFields, chat.fields)
    }

    @Test
    fun recoverThreadBlockedInPending() {
        for (state in ChatThreadState.entries) {
            updateChatThread(chatThread.asCopyable().copy(threadState = state))
            if (state !== Pending) {
                assertSendText(ServerRequest.RecoverThread(connection, chatThread)) {
                    thread.refresh()
                }
            } else {
                assertSendsNothing { thread.refresh() }
            }
        }
    }

    @Test
    fun get_ignores_otherThanSelfThread_threadRecovered() {
        val actual = testCallback(::get) {
            sendServerMessage(ServerResponse.ThreadRecovered())
        }
        assertNull(actual)
    }

    @Test
    fun get_updatesOlderCustomFieldsOnly_threadRecovered() {
        val agent = makeAgent()
        val scrollToken = "scrollToken"

        val customerCustomFieldsInitial = listOf<CustomField>(
            CustomFieldInternal(customerCustomFields[0].id, nextString(), Date(0)),
            CustomFieldInternal(customerCustomFields[1].id, nextString(), Date(2)),
        )
        val customerCustomFieldsUpdate = listOf<CustomField>(
            CustomFieldInternal(customerCustomFields[0].id, nextString(), Date(1)),
            CustomFieldInternal(customerCustomFields[1].id, nextString(), Date(1)),
        )
        val expectedCustomerCustomFields = listOf(
            customerCustomFieldsUpdate[0],
            customerCustomFieldsInitial[1],
        )

        val contactCustomFieldsInitial = listOf<CustomField>(
            CustomFieldInternal(contactCustomFields[0].id, nextString(), Date(0)),
            CustomFieldInternal(contactCustomFields[1].id, nextString(), Date(2)),
        )
        val contactCustomFieldsUpdate = listOf<CustomField>(
            CustomFieldInternal(contactCustomFields[0].id, nextString(), Date(1)),
            CustomFieldInternal(contactCustomFields[1].id, nextString(), Date(1)),
        )
        val expectedContactCustomFields = listOf(
            contactCustomFieldsUpdate[0],
            contactCustomFieldsInitial[1],
        )

        // set up initial custom fields for customer and contact
        (chat as ChatWithParameters).fields = customerCustomFieldsInitial
        updateChatThread(
            makeChatThread(
                fields = contactCustomFieldsInitial
            )
        )
        val id = chatThread.id
        val messages = arrayOf(
            makeMessageModel(threadIdOnExternalPlatform = id),
            makeMessageModel(threadIdOnExternalPlatform = id)
        )
        val expected = chatThread.asCopyable().copy(
            scrollToken = scrollToken,
            messages = messages.mapNotNull(MessageModel::toMessage) + chatThread.messages,
            threadAgent = agent.toAgent(),
            fields = expectedContactCustomFields,
        )
        val actual = testCallback(::get) {
            sendServerMessage(
                ServerResponse.ThreadRecovered(
                    scrollToken,
                    expected,
                    agent = agent,
                    messages = messages,
                    customerCustomFields = customerCustomFieldsUpdate
                )
            )
        }
        assertEquals(expected, actual)
        assertEquals(expectedCustomerCustomFields, chat.fields)
    }

    @Test
    fun refresh_sendsExpectedMessage() {
        val id = chatThread.id
        assertSendText(ServerRequest.RecoverThread(connection, chatThread), id.toString()) {
            thread.refresh()
        }
    }

    @Test
    fun get_observes_agentTypingStarted() {
        val agent = makeAgent()
        val threadAgent1 = agent.toAgent()
        assertFalse(threadAgent1.isTyping, "Agent isTyping should be false")
        updateChatThread(chatThread.asCopyable().copy(threadAgent = threadAgent1))
        val thread = chatThread
        val actual = testCallback(::get) {
            sendServerMessage(ServerResponse.TypingStarted(thread))
        }
        assertNull(actual)
    }

    @Test
    fun get_observes_agentTypingStarted_withAgentInEvent() {
        val agent = makeAgent()
        val thread = chatThread
        val actual = testCallback(::get) {
            sendServerMessage(ServerResponse.TypingStarted(thread, agent))
        }
        val expected = thread.asCopyable().copy(threadAgent = agent.toAgent().asCopyable().copy(isTyping = true))
        assertEquals(expected, actual)
    }

    @Test
    fun get_observes_agentTypingEnded() {
        // prime the returned thread and ensure the test doesn't return false positive
        get_observes_agentTypingStarted()
        val actual = testCallback(::get) {
            sendServerMessage(ServerResponse.TypingEnded(chatThread))
        }
        assertNull(actual)
    }

    @Test
    fun get_observes_agentTypingEnded_withAgentInEvent() {
        // prime the returned thread and ensure the test doesn't return false positive
        get_observes_agentTypingStarted_withAgentInEvent()
        val agent = makeAgent()
        val threadAgent = agent.toAgent()
        assertFalse(threadAgent.isTyping, "Agent isTyping should be false")
        val expected = chatThread.asCopyable().copy(threadAgent = threadAgent)
        val actual = testCallback(::get) {
            sendServerMessage(ServerResponse.TypingEnded(expected, agent))
        }
        assertEquals(expected, actual)
    }

    @Test
    fun archiveHappyPath() {
        var result: Boolean? = null
        val uuid = UUID.randomUUID()

        UUIDProvider.next = { uuid }

        chatThread.canAddMoreMessages shouldBe true

        assertSendText(ServerRequest.ArchiveThread(connection, chatThread)) {
            thread.archive { result = it }
        }

        result shouldBe null
        chatThread.canAddMoreMessages shouldBe false

        socketServer.sendServerMessage(ServerResponse.ThreadArchived(uuid.toString()))

        result shouldBe true
        chatThread.canAddMoreMessages shouldBe false
    }

    @Test
    fun archiveFailure() {
        var result: Boolean? = null
        val uuid = UUID.randomUUID()

        UUIDProvider.next = { uuid }

        chatThread.canAddMoreMessages shouldBe true

        assertSendText(ServerRequest.ArchiveThread(connection, chatThread)) {
            thread.archive { result = it }
        }

        result shouldBe null
        chatThread.canAddMoreMessages shouldBe false

        socketServer.sendServerMessage(ServerResponse.ErrorResponse(ErrorType.ArchivingThreadFailed.value))

        result shouldBe false
        chatThread.canAddMoreMessages shouldBe true
    }

    @Test
    fun archiveException() {
        var result: Boolean? = null
        val uuid = UUID.randomUUID()

        UUIDProvider.next = { uuid }

        chatThread.canAddMoreMessages shouldBe true

        every { socket.send(any<String>()) } throws ServerCommunicationError("Archive failed")

        thread.archive { result = it }

        result shouldBe false
        chatThread.canAddMoreMessages shouldBe true
    }

    // ---

    private fun get(listener: (ChatThread) -> Unit): Cancellable =
        thread.get(listener = { listener(it) })

    private fun updateChatThread(updatedThread: ChatThread) {
        val threadMutable = updatedThread.asMutable() // Handlers are memoized, therefore mutating the thread is required
        if (::chatThread.isInitialized) {
            chatThread.update(threadMutable)
        } else {
            chatThread = threadMutable
        }
        thread = chat.threads().thread(threadMutable)
    }
}
