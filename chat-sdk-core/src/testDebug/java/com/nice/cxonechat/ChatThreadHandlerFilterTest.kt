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

import android.annotation.SuppressLint
import com.nice.cxonechat.internal.ChatThreadHandlerFilter
import com.nice.cxonechat.internal.ChatWithParameters
import com.nice.cxonechat.internal.model.network.Parameters
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.model.makeMessage
import com.nice.cxonechat.model.makeMessageContent
import com.nice.cxonechat.model.makeMessageModel
import com.nice.cxonechat.thread.ChatThread
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class ChatThreadHandlerFilterTest : AbstractChatTest() {
    private lateinit var origin: ChatThreadHandler
    private lateinit var filter: ChatThreadHandlerFilter
    private lateinit var chatThread: ChatThread
    private lateinit var unsupportedMessage: com.nice.cxonechat.message.Message
    private lateinit var supportedMessage: com.nice.cxonechat.message.Message
    private var listenerCalledWith: ChatThread? = null

    override fun prepare() {
        super.prepare()
        val threadId = UUID.randomUUID()
        unsupportedMessage = makeMessage(
            model = makeMessageModel(
                threadIdOnExternalPlatform = threadId,
                messageContent = makeMessageContent().copy(
                    parameters = Parameters.Object(isUnsupportedMessageTypeAnswer = true)
                ),
            )
        )
        supportedMessage = makeMessage(
            model = makeMessageModel(threadIdOnExternalPlatform = threadId)
        )
        chatThread = makeChatThread(messages = listOf(supportedMessage, unsupportedMessage), id = threadId)
        origin = mockk(relaxed = true)
        every { origin.get() } returns chatThread
        every { origin.get(any()) } answers {
            val l = firstArg<ChatThreadHandler.OnThreadUpdatedListener>()
            l.onUpdated(chatThread)
            mockk<Cancellable>(relaxed = true)
        }
        filter = ChatThreadHandlerFilter(origin, mockk<ChatWithParameters>(relaxed = true))
    }

    @Test
    fun onUpdated_filters_thread_for_listener() {
        val listener = object : ChatThreadHandler.OnThreadUpdatedListener {
            override fun onUpdated(thread: ChatThread) {
                listenerCalledWith = thread
            }
        }
        @SuppressLint("CheckResult") // We don't need to cancel in this test
        filter.get(listener)
        val actual = listenerCalledWith
        assertNotNull(actual)
        assertEquals(1, actual.messages.size)
        assertEquals(supportedMessage, actual.messages[0])
    }

    @Test
    fun filter_removes_unsupported_messages() {
        val filtered = filter.get()
        assertEquals(1, filtered.messages.size)
        assertEquals(supportedMessage, filtered.messages[0])
    }
}
