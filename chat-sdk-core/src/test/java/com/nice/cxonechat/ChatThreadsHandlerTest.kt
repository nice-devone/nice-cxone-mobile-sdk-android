@file:Suppress("FunctionMaxLength")

package com.nice.cxonechat

import com.nice.cxonechat.internal.model.CustomFieldInternal
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.server.ServerRequest
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.CustomField
import com.nice.cxonechat.tool.SocketFactoryMock
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class ChatThreadsHandlerTest : AbstractChatTest() {

    private lateinit var threads: ChatThreadsHandler

    override fun prepare() {
        super.prepare()
        threads = chat.threads()
    }

    // ---

    @Test
    fun refresh_sendsExpectedMessage() {
        assertSendText(ServerRequest.FetchThreadList(connection)) {
            threads.refresh()
        }
    }

    @Test
    fun threads_notifies_withInitialList() {
        val expected = List(2) { makeChatThread() }
        val actual = testCallback(::threads) {
            sendServerMessage(ServerResponse.ThreadListFetched(expected))
        }
        assertEquals(expected, actual)
    }

    @Test
    fun create_passesNewThread() {
        val handler = chat.threads().create()
        val thread = handler.get()
        assertNotNull(thread)
    }

    @Test
    fun create_withCustomParameters_passesNewThread() {
        val handler = chat.threads().create(emptyMap())
        val thread = handler.get()
        assertNotNull(thread)
    }

    @Test
    fun create_sends_simpleWelcomeMessage_toThread() {
        val expected = "Welcome, how was your day?"
        assertSendsWelcomeMessageToThread(expected, expected)
    }

    @Test
    fun create_sendsComplexWelcomeMessage_toThread() {
        val message = "Welcome {{customer.firstName|stranger}}, how was your {{customer.customFields.testField|day}}?"
        val expected = "Welcome ${SocketFactoryMock.firstName}, how was your testValue?"
        assertSendsWelcomeMessageToThread(message, expected)
    }

    @Test
    fun create_withCustomParameters_sendsComplexWelcomeMessage_toThread() {
        val message = "Welcome {{customer.firstName|stranger}}, " +
            "how was your {{customer.customFields.testField|day}} " +
            "{{contact.customFields.testField2|dear customer}}?" +
            "{{fallbackMessage|This unit test has failed.}}"
        val expected = "Welcome ${SocketFactoryMock.firstName}, how was your testValue testValue2?"
        val contactCustomFields = mapOf("testField2" to "testValue2")
        assertSendsWelcomeMessageToThread(message, expected, contactCustomFields.map(::CustomFieldInternal)) {
            chat.threads().create(contactCustomFields)
        }
    }

    fun threads(listener: (List<ChatThread>) -> Unit): Cancellable =
        threads.threads(listener = { listener(it) })

    private fun assertSendsWelcomeMessageToThread(
        message: String,
        expected: String,
        contactCustomFields: List<CustomField> = emptyList(),
        create: () -> ChatThreadHandler = { chat.threads().create() }
    ) {
        var welcomeMessage = ""
        doAnswer { welcomeMessage = it.getArgument(0) }.whenever(storage).welcomeMessage = any()
        whenever(storage.welcomeMessage).thenAnswer { welcomeMessage }
        val customerCustomFields = mapOf("testField" to "testValue")
        this serverResponds ServerResponse.WelcomeMessage(message, customerCustomFields)
        val thread = makeChatThread(id = TestUUIDValue, fields = contactCustomFields)
        assertSendText(ServerRequest.SendOutbound(connection, thread, storage, expected)) {
            create()
        }
    }
}
