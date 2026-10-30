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

@file:Suppress("FunctionMaxLength")

package com.nice.cxonechat.internal

import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadsHandler
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatThreadsHandlerMessagesTest {

    /**
     * Thread handlers share their chain from the moment they are built, so the handler behind the cache watcher can
     * apply a MessageCreated before the watcher subscribes. Its first emission then already carries the new contact
     * and state, which the thread list (no contact, Received) does not know.
     */
    @Test
    fun `threadsFlow keeps a MessageCreated the handler applied before the cache watcher subscribed`() =
        runTest(UnconfinedTestDispatcher()) {
            val listedThread = makeChatThread(contactId = null, threadState = ChatThreadState.Received)
            val updatedThread = listedThread.asCopyable().copy(contactId = CONTACT_ID, threadState = ChatThreadState.Ready)
            val mockHandler: ChatThreadHandler = mockk(relaxed = true) {
                every { threadFlow } returns MutableStateFlow(updatedThread)
            }
            val mockOrigin: ChatThreadsHandler = mockk(relaxed = true) {
                every { threadsFlow } returns MutableStateFlow(listOf(listedThread))
                every { thread(any()) } returns mockHandler
            }
            val handler = ChatThreadsHandlerMessages(mockOrigin, backgroundScope)
            val listed = mutableListOf<List<ChatThread>>()
            backgroundScope.launch { handler.threadsFlow.collect { listed += it } }

            handler.thread(listedThread)

            val shown = assertNotNull(listed.lastOrNull()?.singleOrNull(), "threadsFlow must list the thread")
            assertEquals(CONTACT_ID, shown.contactId)
            assertEquals(ChatThreadState.Ready, shown.threadState)
        }

    private companion object {
        private const val CONTACT_ID = "contact-a"
    }
}
