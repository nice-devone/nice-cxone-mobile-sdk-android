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

package com.nice.cxonechat.internal

import com.nice.cxonechat.internal.ChatThreadHandlerLiveChat.Companion.BEGIN_CONVERSATION_MESSAGE
import com.nice.cxonechat.internal.ChatThreadHandlerLiveChat.Companion.removeConversationStarter
import com.nice.cxonechat.internal.model.MessageText
import com.nice.cxonechat.internal.model.network.MessagePolyContent
import com.nice.cxonechat.internal.model.network.Parameters
import com.nice.cxonechat.message.Message
import com.nice.cxonechat.model.makeMessageModel
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * Unit tests for [ChatThreadHandlerLiveChat.Companion.removeConversationStarter].
 *
 * The function should filter out the silent "Begin Conversation" message that the SDK
 * sends automatically when a new LiveChat thread is created, so it never appears in the
 * customer-visible message list.
 */
internal class ChatThreadHandlerLiveChatCompanionTest {

    // -------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------

    private fun makeBeginConversationMessage(isInitialMessage: Boolean = true): MessageText {
        val content = MessagePolyContent.Text(
            payload = MessagePolyContent.Text.Payload(BEGIN_CONVERSATION_MESSAGE),
            parameters = Parameters.Object(isInitialMessage = isInitialMessage),
        )
        return MessageText(makeMessageModel(messageContent = content))
    }

    private fun makeRegularMessage(text: String = "Hello"): MessageText {
        val content = MessagePolyContent.Text(
            payload = MessagePolyContent.Text.Payload(text),
        )
        return MessageText(makeMessageModel(messageContent = content))
    }

    // -------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------

    @Test
    fun `empty list stays empty`() {
        val result = emptyList<Message>().removeConversationStarter()
        assertEquals(emptyList(), result)
    }

    @Test
    fun `regular messages are not removed`() {
        val messages = listOf(makeRegularMessage("Hi"), makeRegularMessage("There"))
        val result = messages.removeConversationStarter()
        assertEquals(messages, result)
    }

    @Test
    fun `begin conversation message with isInitialMessage=true is removed`() {
        val begin = makeBeginConversationMessage(isInitialMessage = true)
        val result = listOf(begin).removeConversationStarter()
        assertEquals(emptyList(), result)
    }

    @Test
    fun `begin conversation message with isInitialMessage=false is NOT removed`() {
        val begin = makeBeginConversationMessage(isInitialMessage = false)
        val result = listOf(begin).removeConversationStarter()
        assertEquals(listOf(begin), result)
    }

    @Test
    fun `begin conversation removed while surrounding messages are preserved`() {
        val before = makeRegularMessage("before")
        val begin = makeBeginConversationMessage()
        val after = makeRegularMessage("after")
        val result = listOf(before, begin, after).removeConversationStarter()
        assertEquals(listOf(before, after), result)
    }

    @Test
    fun `same text as begin conversation but no isInitialMessage flag is not removed`() {
        // Text matches but no Parameters.Object flag — should survive filtering.
        val content = MessagePolyContent.Text(
            payload = MessagePolyContent.Text.Payload(BEGIN_CONVERSATION_MESSAGE),
            parameters = null,
        )
        val message = MessageText(makeMessageModel(messageContent = content))
        val result = listOf(message).removeConversationStarter()
        assertEquals(listOf(message), result)
    }

    @Test
    fun `non-Text messages are never removed`() {
        // A plain mock Message that is NOT a MessageText is untouched by the filter.
        val mockMsg = mockk<Message>()
        val result = listOf(mockMsg).removeConversationStarter()
        assertSame(mockMsg, result.single())
    }
}
