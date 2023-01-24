@file:Suppress("FunctionMaxLength")

package com.nice.cxonechat

import com.nice.cxonechat.internal.model.CustomFieldInternal
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.server.ServerRequest
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.tool.nextString
import com.nice.cxonechat.tool.nextStringMap
import org.junit.Test
import kotlin.test.assertEquals

internal class ChatFieldHandlerTest : AbstractChatTest() {

    private lateinit var thread: ChatThread

    override fun prepare() {
        super.prepare()
        thread = makeChatThread()
    }

    // ---

    @Test
    fun setCustomer_sendsExpectedMessage() {
        val fields = nextStringMap()
        assertSendText(ServerRequest.SetConsumerCustomFields(connection, fields)) {
            chat.customFields().add(fields)
        }
    }

    @Test
    fun setContact_sendsExpectedMessage() {
        val fields = nextStringMap()
        assertSendText(ServerRequest.SetConsumerContactCustomFields(connection, thread, fields), thread.id.toString()) {
            chat.threads().thread(thread).customFields().add(fields)
        }
    }

    @Test
    fun addFields_appendsToThread() {
        val handler = chat.threads().thread(thread)
        val fields = handler.customFields()
        val newFields = nextStringMap()
        testSendTextFeedback()
        fields.add(newFields)
        assertEquals(newFields.map(::CustomFieldInternal), handler.get().fields)
    }

    @Test
    fun addFields_appendsToChat() {
        val fields = chat.customFields()
        val newFields = nextStringMap()
        fields.add(newFields)
        assertEquals(newFields.map(::CustomFieldInternal), chat.fields)
    }

    @Test
    fun addFields_toChat_replacesCurrentValue() {
        val fields = chat.customFields()
        val firstField = nextStringMap()
        fields.add(firstField)
        val newFields = firstField.map {
            it.key to nextString()
        }.toMap()
        fields.add(newFields)
        assertEquals(newFields.map(::CustomFieldInternal), chat.fields)
    }

}
