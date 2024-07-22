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

@file:Suppress("FunctionMaxLength")

package com.nice.cxonechat

import com.nice.cxonechat.internal.model.ChannelConfiguration
import com.nice.cxonechat.internal.model.CustomFieldPolyType.Text
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.server.ServerRequest
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.tool.nextString
import com.nice.cxonechat.tool.nextStringMap
import org.junit.Test
import kotlin.test.assertEquals

internal class ChatFieldHandlerTest : AbstractChatTest() {

    private lateinit var thread: ChatThread

    private val questionId = nextString()

    private val fields = nextStringMap()

    override val config: ChannelConfiguration
        get() {
            val config = super.config.let(::requireNotNull)
            return config.copy(
                contactCustomFields = listOf(
                    Text(questionId, "first field")
                ) + fields.entries.map {
                    Text(it.key, it.value)
                }
            )
        }

    override fun prepare() {
        super.prepare()
        thread = makeChatThread()
    }

    // ---

    @Test
    fun setCustomer_sendsExpectedMessage() {
        assertSendText(ServerRequest.SetCustomerCustomFields(connection, fields), replaceDate = true) {
            chat.customFields().add(fields)
        }
    }

    @Test
    fun setContact_sendsExpectedMessage() {
        assertSendText(ServerRequest.SetContactCustomFields(connection, thread, fields), thread.id.toString(), replaceDate = true) {
            chat.threads().thread(thread).customFields().add(fields)
        }
    }

    @Test
    fun addFields_appendsToThread() {
        val newFields = mapOf(
            questionId to "oldValue"
        )
        val handler = chat.threads().thread(thread)
        val fields = handler.customFields()
        testSendTextFeedback()
        fields.add(newFields)
        assertEquals(newFields, handler.get().fields.associate { it.id to it.value })
    }

    @Test
    fun addFields_appendsToChat() {
        val fields = chat.customFields()
        val newFields = mapOf(questionId to nextString())
        fields.add(newFields)
        assertEquals(newFields, chat.fields.associate { it.id to it.value })
    }

    @Test
    fun addFields_toChat_replacesCurrentValue() {
        val fields = chat.customFields()
        val firstField = mapOf(questionId to "answer")
        fields.add(firstField)
        val newFields = firstField.map {
            it.key to nextString()
        }.toMap()
        fields.add(newFields)
        assertEquals(newFields, chat.fields.associate { it.id to it.value })
    }
}
