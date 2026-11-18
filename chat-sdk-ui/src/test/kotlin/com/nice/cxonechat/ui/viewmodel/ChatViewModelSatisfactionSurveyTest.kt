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

import androidx.lifecycle.viewModelScope
import com.nice.cxonechat.Chat
import com.nice.cxonechat.ChatInstanceProvider
import com.nice.cxonechat.ChatMode
import com.nice.cxonechat.ChatState
import com.nice.cxonechat.ChatThreadsHandler
import com.nice.cxonechat.log.LoggerNoop
import com.nice.cxonechat.prechat.PreChatSurvey
import com.nice.cxonechat.ui.data.repository.SelectedThreadRepository
import com.nice.cxonechat.ui.storage.ValueStorage
import com.nice.cxonechat.ui.viewmodel.ChatViewModel.DialogState.Survey
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

/**
 * Covers [ChatViewModel]'s pre-chat survey dialog waiting for a satisfaction survey recovered for
 * a previous thread to be seen first. Split out of [ChatViewModelTest] (detekt LargeClass).
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatViewModelSatisfactionSurveyTest {

    private val testDispatcher = StandardTestDispatcher()
    private val chatProvider = mockk<ChatInstanceProvider>(relaxed = true)
    private val selectedThreadRepository =
        SelectedThreadRepository(chatProvider, valueStorage = mockk<ValueStorage>(relaxed = true), logger = LoggerNoop)
    private val chatStateViewModel = mockk<ChatStateViewModel>(relaxed = true)
    private val threads = mockk<ChatThreadsHandler>(relaxed = true)
    private val chat = mockk<Chat>(relaxed = true)
    private val preChatSurvey = mockk<PreChatSurvey>()

    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { chatProvider.chat } returns chat
        every { chatProvider.chatState } returns ChatState.Ready
        every { chatStateViewModel.state } returns MutableStateFlow(ChatState.Ready)
        every { chat.chatMode } returns ChatMode.LiveChat
        every { chat.threads() } returns threads
        every { threads.preChatSurvey } returns preChatSurvey
        // No existing thread to select, so refreshThreadState falls through to the pre-chat survey.
        every { threads.threadsFlow } returns MutableStateFlow(emptyList())

        viewModel = ChatViewModel(selectedThreadRepository, chatProvider, LoggerNoop, chatStateViewModel)
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun `pre-chat survey dialog waits for a shown satisfaction survey before appearing`() = runTest {
        val surveyOwner = Any()
        selectedThreadRepository.setSatisfactionSurveyShowing(surveyOwner, true)

        viewModel.refreshThreadState()
        testDispatcher.scheduler.advanceUntilIdle()
        // resolveCurrentState() hops to a real Dispatchers.Default thread, which the test
        // dispatcher does not control. Suspend for its actual subscription to
        // satisfactionSurveyShowingFlow (proof it reached and parked on the wait) instead of
        // guessing how long that takes. Run on a real dispatcher so the timeout is real
        // wall-clock time, not virtual time the test scheduler could fast-forward through.
        withContext(Dispatchers.Default) {
            withTimeout(2_000.milliseconds) {
                selectedThreadRepository.surveyShowingSubscriptionCount.first { it >= 1 }
            }
        }

        assertNotEquals(
            "Must not show the pre-chat survey while a satisfaction survey is still showing",
            Survey(preChatSurvey),
            viewModel.dialogShown.value,
        )

        selectedThreadRepository.setSatisfactionSurveyShowing(surveyOwner, false)
        // Join the real job rather than relying on advanceUntilIdle(), which only drives the
        // (virtual) test dispatcher, not the real Dispatchers.Default hop above.
        viewModel.resolveStateJob?.join()

        assertEquals(
            "Must show the pre-chat survey once the satisfaction survey is no longer showing",
            Survey(preChatSurvey),
            viewModel.dialogShown.value,
        )
    }
}
