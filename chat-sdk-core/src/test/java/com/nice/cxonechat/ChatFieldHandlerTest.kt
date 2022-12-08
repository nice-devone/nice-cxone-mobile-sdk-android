@file:Suppress("FunctionMaxLength")

package com.nice.cxonechat

import com.nice.cxonechat.internal.model.CustomFieldInternal
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.server.ServerRequest
import com.nice.cxonechat.thread.ChatThread
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
        val fields = mapOf("my-field" to "my-value")
        assertSendText(ServerRequest.SetConsumerCustomFields(connection, fields)) {
            chat.customFields().add(fields)
        }
    }

    @Test
    fun setContact_sendsExpectedMessage() {
        val fields = mapOf("my-field!" to "my-value?")
        assertSendText(ServerRequest.SetConsumerContactCustomFields(connection, thread, fields), thread.id.toString()) {
            chat.threads().thread(thread).customFields().add(fields)
        }
    }

    @Test
    fun addFields_appendsToThread() {
        val handler = chat.threads().thread(thread)
        val fields = handler.customFields()
        val newFields = mapOf("my-field?" to "my-value!")
        testSendTextFeedback()
        fields.add(newFields)
        assertEquals(newFields.map { CustomFieldInternal(it.key, it.value) }, handler.get().fields)
    }

}
