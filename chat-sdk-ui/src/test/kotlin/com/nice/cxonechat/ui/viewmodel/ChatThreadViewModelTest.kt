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

package com.nice.cxonechat.ui.viewmodel

import com.nice.cxonechat.Chat
import com.nice.cxonechat.ChatInstanceProvider
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerNoop
import com.nice.cxonechat.state.Configuration
import com.nice.cxonechat.ui.UiModule
import com.nice.cxonechat.ui.data.repository.SelectedThreadRepository
import com.nice.cxonechat.ui.data.source.ContentDataSourceList
import com.nice.cxonechat.ui.storage.ValueStorage
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatThreadViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private var currentChat: Chat? = null

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        startKoin {
            modules(
                module {
                    single<Logger>(named(UiModule.LOGGER_NAME)) { LoggerNoop }
                }
            )
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    private fun viewModel(): ChatThreadViewModel {
        val chatProvider = mockk<ChatInstanceProvider>(relaxed = true) {
            every { chat } answers { currentChat }
        }
        return ChatThreadViewModel(
            contentDataSource = mockk<ContentDataSourceList>(relaxed = true),
            selectedThreadRepository = SelectedThreadRepository(chatProvider),
            chatProvider = chatProvider,
            logger = LoggerNoop,
            valueStorage = mockk<ValueStorage>(relaxed = true),
        )
    }

    private fun configuration(liveChat: Boolean, multiThread: Boolean, allowTranscript: Boolean) =
        mockk<Configuration> {
            every { isLiveChat } returns liveChat
            every { hasMultipleThreadsPerEndUser } returns multiThread
            every { liveChatAllowTranscript } returns allowTranscript
        }

    @Test
    fun `config flags do not throw and default to false while the chat is still being built`() {
        currentChat = null

        val viewModel = viewModel()

        assertFalse(viewModel.isLiveChat)
        assertFalse(viewModel.isMultiThreadEnabled)
        assertFalse(viewModel.liveChatAllowTranscript)
    }

    @Test
    fun `config flags reflect the live configuration once the chat is available`() {
        currentChat = null
        val viewModel = viewModel()

        currentChat = mockk<Chat> {
            every { configuration } returns configuration(liveChat = true, multiThread = true, allowTranscript = true)
        }

        assertTrue(viewModel.isLiveChat)
        assertTrue(viewModel.isMultiThreadEnabled)
        assertTrue(viewModel.liveChatAllowTranscript)
    }

    @Test
    fun `config flags fall back to the last known configuration when the chat becomes null during teardown`() {
        currentChat = mockk<Chat> {
            every { configuration } returns configuration(liveChat = true, multiThread = true, allowTranscript = true)
        }
        val viewModel = viewModel()

        // Prime the cache with the live configuration.
        assertTrue(viewModel.isLiveChat)

        // signOut() nulls the session-scoped chat while long-lived flows may still read the flags.
        currentChat = null

        assertTrue(viewModel.isLiveChat)
        assertTrue(viewModel.isMultiThreadEnabled)
        assertTrue(viewModel.liveChatAllowTranscript)
    }

    @Test
    fun `preChatSurvey and hasQuestions do not throw while the chat is still being built`() {
        currentChat = null

        val viewModel = viewModel()

        assertNull(viewModel.preChatSurvey)
        assertFalse(viewModel.hasQuestions)
    }
}
