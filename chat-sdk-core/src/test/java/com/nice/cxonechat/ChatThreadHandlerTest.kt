@file:Suppress("FunctionMaxLength")

package com.nice.cxonechat

import com.nice.cxonechat.internal.copy.AgentCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.MessageModel
import com.nice.cxonechat.model.makeAgent
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.model.makeMessageModel
import com.nice.cxonechat.server.ServerRequest
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.tool.nextString
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class ChatThreadHandlerTest : AbstractChatTest() {

    private lateinit var chatThread: ChatThread
    private lateinit var thread: ChatThreadHandler

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
            threadAgent = agent.toAgent()
        )
        val actual = testCallback(::get) {
            sendServerMessage(ServerResponse.ThreadRecovered(scrollToken, expected, agent, messages = messages))
        }
        assertEquals(expected, actual)
    }

    @Test
    fun get_ignores_otherThanSelfThread_threadRecovered() {
        val actual = testCallback(::get) {
            sendServerMessage(ServerResponse.ThreadRecovered())
        }
        assertNull(actual)
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
        val thread = chatThread
        val actual = testCallback(::get) {
            sendServerMessage(ServerResponse.TypingStarted(thread))
        }
        val expected = thread.asCopyable().copy(threadAgent = thread.threadAgent?.asCopyable()?.copy(isTyping = true))
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

    // ---

    private fun get(listener: (ChatThread) -> Unit): Cancellable =
        thread.get(listener = { listener(it) })

}
