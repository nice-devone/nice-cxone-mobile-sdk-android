/*
 * Copyright (c) 2026. NICE Ltd. All rights reserved.
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
import com.nice.cxonechat.internal.model.network.EventCaseStatusChanged.CaseStatus.Open
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ContactHandler
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class ChatThreadHandlerSingleThreadTest : AbstractSingleThreadChatTest() {

    private lateinit var chatThread: ChatThreadMutable
    private lateinit var thread: ChatThreadHandler

    override fun prepare() {
        super.prepare()
        connect()
        updateChatThread(makeChatThread())
    }

    @Test
    fun `caseStatusChanged with only a new handler emits the handler on threadFlow`() {
        val actual = testCallback(::get) {
            sendServerMessage(ServerResponse.CaseStatusChanged(chatThread, Open, handler = "cognigy"))
        }
        assertEquals(ContactHandler.Cognigy, actual.contactHandler)
        assertEquals(ContactHandler.Cognigy, thread.get().contactHandler)
    }

    @Test
    fun `caseStatusChanged without a handler keeps the previous handler`() {
        updateChatThread(chatThread.asCopyable().copy(contactHandler = ContactHandler.Cognigy))
        val actual = testCallback(::get) {
            sendServerMessage(ServerResponse.CaseStatusChanged(chatThread, Open))
        }
        assertNull(actual)
        assertEquals(ContactHandler.Cognigy, thread.get().contactHandler)
    }

    // ---

    private fun get(listener: (ChatThread) -> Unit): Cancellable {
        val job = testScope.launch { thread.threadFlow.drop(1).collect { listener(it) } }
        return Cancellable { job.cancel() }
    }

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
