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
import com.nice.cxonechat.internal.model.network.EventCaseStatusChanged.CaseStatus.Closed
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState.Ready
import com.nice.cxonechat.thread.ChatThreadState.Received
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

internal class ChatThreadsHandlerLiveChatTest : AbstractChatTest() {

    private lateinit var threads: ChatThreadsHandler

    override fun prepare() {
        isLiveChat = true
        super.prepare()
        threads = chat.threads()
    }

    @Test
    fun threads_notifies_caseClosed() {
        connect()
        val initial = makeChatThread(threadState = Received)
        val expected = initial.copy(canAddMoreMessages = false, threadState = Ready, contactId = TestContactId)
        val actual = testCallback(::threads) {
            sendServerMessage(ServerResponse.LivechatRecovered(thread = initial))
            sendServerMessage(ServerResponse.CaseStatusChanged(expected, Closed))
        }
        assertNotEquals(expected, initial)
        assertEquals(1, actual.size)
        assertEquals(expected, actual.first().asCopyable().copy())
    }

    // --

    fun threads(listener: (List<ChatThread>) -> Unit): Cancellable =
        threads.threads(listener = { listener(it) })
}
