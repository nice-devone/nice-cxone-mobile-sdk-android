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

package com.nice.cxonechat.ui.data.repository

import com.nice.cxonechat.Chat
import com.nice.cxonechat.ChatInstanceProvider
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.ui.domain.model.NoThreadHandler
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import kotlin.test.assertSame

internal class SelectedThreadRepositoryTest {

    private val chatProvider = mockk<ChatInstanceProvider>(relaxed = true)
    private val repository = SelectedThreadRepository(chatProvider)

    @Test
    fun `repository registers itself as a provider listener`() {
        verify { chatProvider.addListener(repository) }
    }

    @Test
    fun `session swap re-derives the selected thread handler from the new chat`() {
        val selectedThread = mockk<ChatThread>(relaxed = true)
        val previousSessionHandler = mockk<ChatThreadHandler>(relaxed = true) {
            every { get() } returns selectedThread
        }
        repository.chatThreadHandler = previousSessionHandler

        val newSessionHandler = mockk<ChatThreadHandler>(relaxed = true)
        val newSessionChat = mockk<Chat>(relaxed = true) {
            every { threads().thread(selectedThread) } returns newSessionHandler
        }
        repository.onChatChanged(newSessionChat)

        assertSame(
            newSessionHandler,
            repository.chatThreadHandler,
            "a new session's Chat must yield a freshly derived handler for the selected thread"
        )
        assertSame(newSessionHandler, repository.chatThreadHandlerFlow.value)
    }

    @Test
    fun `session swap without a selection keeps NoThreadHandler and derives nothing`() {
        val newSessionChat = mockk<Chat>(relaxed = true)

        repository.onChatChanged(newSessionChat)

        assertSame(NoThreadHandler, repository.chatThreadHandler)
        verify(exactly = 0) { newSessionChat.threads() }
    }

    @Test
    fun `cleared chat resets the selection to NoThreadHandler`() {
        val previousSessionHandler = mockk<ChatThreadHandler>(relaxed = true) {
            every { get() } returns mockk(relaxed = true)
        }
        repository.chatThreadHandler = previousSessionHandler

        repository.onChatChanged(null)

        assertSame(NoThreadHandler, repository.chatThreadHandler)
    }
}
