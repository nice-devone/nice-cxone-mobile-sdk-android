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

package com.nice.cxonechat

import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.ChatThreadMutable
import com.nice.cxonechat.internal.model.ChatThreadMutable.Companion.asMutable
import com.nice.cxonechat.internal.model.network.EventCaseStatusChanged.CaseStatus.Closed
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.thread.ChatThread
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

internal class ChatThreadHandlerLiveChatTest: AbstractChatTest() {

    private lateinit var chatThread: ChatThreadMutable
    private lateinit var thread: ChatThreadHandler

    override fun prepare() {
        isLiveChat = true
        super.prepare()
        updateChatThread(makeChatThread())
    }

    @Test
    fun get_observes_case_closed() {
        val expected = chatThread.asCopyable().copy(
            canAddMoreMessages = false
        )
        assertNotEquals<ChatThread>(chatThread, expected)
        val actual = testCallback(::get) {
            sendServerMessage(ServerResponse.CaseStatusChanged(chatThread.snapshot(), Closed))
        }
        assertEquals(expected, actual)
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
