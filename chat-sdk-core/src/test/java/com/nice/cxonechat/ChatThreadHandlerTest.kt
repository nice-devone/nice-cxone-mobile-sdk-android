@file:Suppress("FunctionMaxLength")

package com.nice.cxonechat

import com.nice.cxonechat.internal.ChatWithParameters
import com.nice.cxonechat.internal.copy.AgentCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.ChannelConfiguration
import com.nice.cxonechat.internal.model.CustomFieldInternal
import com.nice.cxonechat.internal.model.CustomFieldPolyType.Text
import com.nice.cxonechat.internal.model.MessageModel
import com.nice.cxonechat.model.makeAgent
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.model.makeMessageModel
import com.nice.cxonechat.server.ServerRequest
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.CustomField
import com.nice.cxonechat.tool.nextString
import org.junit.Test
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

internal class ChatThreadHandlerTest : AbstractChatTest() {

    private lateinit var chatThread: ChatThread
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
        chatThread = makeChatThread()
        thread = chat.threads().thread(chatThread)
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
    fun get_observes_threadMetadataLoaded() {
        val id = chatThread.id
        val agent = makeAgent()
        val message = makeMessageModel(threadIdOnExternalPlatform = id)
        val expected = chatThread.asCopyable().copy(
            messages = listOfNotNull(message.toMessage()),
            threadAgent = agent.toAgent()
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
            messages = listOfNotNull(messageModel.toMessage())
        )
        val actual = testCallback(::get) {
            sendServerMessage(ServerResponse.MessageCreated(chatThread, messageModel))
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
        val messages = arrayOf(
            makeMessageModel(threadIdOnExternalPlatform = id),
            makeMessageModel(threadIdOnExternalPlatform = id)
        )
        val agent = makeAgent()
        val scrollToken = "scrollToken"
        val expected = chatThread.asCopyable().copy(
            scrollToken = scrollToken,
            messages = messages.mapNotNull(MessageModel::toMessage) + chatThread.messages,
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
        assertEquals(expected, actual)
        assertEquals(customerCustomFields, chat.fields)
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
        chatThread = makeChatThread(
            fields = contactCustomFieldsInitial
        )

        thread = chat.threads().thread(chatThread)
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
        chatThread = chatThread.asCopyable().copy(threadAgent = threadAgent1)
        thread = chat.threads().thread(chatThread)
        val thread = chatThread
        val actual = testCallback(::get) {
            sendServerMessage(ServerResponse.TypingStarted(thread))
        }
        val threadAgent2 = threadAgent1.asCopyable().copy(isTyping = true)
        val expected = thread.asCopyable().copy(threadAgent = threadAgent2)
        assertEquals(expected, actual)
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
        val expected = chatThread
        val actual = testCallback(::get) {
            sendServerMessage(ServerResponse.TypingEnded(expected))
        }
        assertEquals(expected, actual)
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

    // ---

    private fun get(listener: (ChatThread) -> Unit): Cancellable =
        thread.get(listener = { listener(it) })
}
