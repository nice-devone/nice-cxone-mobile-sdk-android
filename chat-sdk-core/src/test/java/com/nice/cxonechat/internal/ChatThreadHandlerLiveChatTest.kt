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

package com.nice.cxonechat.internal

import com.nice.cxonechat.AbstractChatTest
import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.thread.ChatThread
import org.junit.Test
import java.util.UUID
import kotlin.test.assertEquals

internal class ChatThreadHandlerLiveChatTest : AbstractChatTest() {
    private lateinit var chatThread: ChatThread
    private lateinit var thread: ChatThreadHandler

    override fun prepare() {
        isLiveChat = true

        super.prepare()

        val threadId = UUID.randomUUID()
        chatThread = makeChatThread(messages = listOf(), id = threadId)
        thread = chat.threads().thread(chatThread)
    }

    @Test
    fun testPositionInQueueUpdates() {
        val expected = chatThread.asCopyable().copy(positionInQueue = 10, hasOnlineAgent = true, contactId = TestContactId)

        val actual = testCallback(::get) {
            sendServerMessage(ServerResponse.SetPositionInQueue(position = 10, isAgentAvailable = true))
        }

        assertEquals(expected, actual.asCopyable().copy())
    }

    private fun get(listener: (ChatThread) -> Unit): Cancellable =
        thread.get(listener = { listener(it) })
}
