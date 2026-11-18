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
import com.nice.cxonechat.ChatState
import com.nice.cxonechat.ChatThreadActionHandler
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadsHandler
import com.nice.cxonechat.Popup
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerNoop
import com.nice.cxonechat.prechat.PreChatSurvey
import com.nice.cxonechat.state.Configuration
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.ui.UiModule
import com.nice.cxonechat.ui.data.repository.SelectedThreadRepository
import com.nice.cxonechat.ui.domain.model.NoThreadHandler
import com.nice.cxonechat.ui.domain.model.Thread
import com.nice.cxonechat.ui.domain.usecase.NotifyUpdateUseCase
import com.nice.cxonechat.ui.storage.ValueStorage
import com.nice.cxonechat.ui.viewmodel.ChatThreadsViewModel.State.ThreadPreChatSurveyRequired
import io.mockk.clearMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.UUID

/**
 * Covers [ChatThreadsViewModel.createThread] waiting for a satisfaction survey recovered for a
 * previous thread to be seen first. Split out of [ChatThreadsViewModelTest] (detekt LargeClass).
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatThreadsViewModelSatisfactionSurveyTest {

    private val testDispatcher = StandardTestDispatcher()

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

    @Test
    fun `createThread waits for a shown satisfaction survey before requiring the new pre-chat survey`() = runTest(testDispatcher) {
        val preChatSurvey = mockk<PreChatSurvey>(relaxed = true)
        val threadsHandler = mockk<ChatThreadsHandler>(relaxed = true) {
            every { this@mockk.preChatSurvey } returns preChatSurvey
            every { threadsFlow } returns MutableSharedFlow(replay = 1)
        }
        val chat = mockk<Chat>(relaxed = true) {
            every { threads() } returns threadsHandler
            every { configuration } returns mockk<Configuration> {
                every { hasMultipleThreadsPerEndUser } returns true
            }
        }
        val chatProvider = mockk<ChatInstanceProvider>(relaxed = true) {
            every { this@mockk.chat } returns chat
        }
        val repo = SelectedThreadRepository(chatProvider, valueStorage = mockk<ValueStorage>(relaxed = true), logger = LoggerNoop)
        val surveyOwner = Any()
        repo.setSatisfactionSurveyShowing(surveyOwner, true)
        val chatStateViewModel = mockk<ChatStateViewModel> {
            every { state } returns MutableStateFlow(ChatState.Ready)
        }
        val viewModel = ChatThreadsViewModel(chatProvider, repo, chatStateViewModel, mockk(relaxed = true))

        viewModel.createThread()
        advanceUntilIdle()

        assertNotEquals(
            "Must not require the new pre-chat survey while a satisfaction survey is still showing",
            ThreadPreChatSurveyRequired(preChatSurvey),
            viewModel.state.value,
        )

        repo.setSatisfactionSurveyShowing(surveyOwner, false)
        advanceUntilIdle()

        assertEquals(
            "Must require the pre-chat survey once the satisfaction survey is no longer showing",
            ThreadPreChatSurveyRequired(preChatSurvey),
            viewModel.state.value,
        )

        viewModel.viewModelScope.cancel()
        advanceUntilIdle()
    }

    // -----------------------------------------------------------------------
    // backgroundSurveyFlow - split from the same test above purely for detekt LargeClass; these
    // cover ChatThreadsViewModel.reconcileBackgroundSurveys, not createThread().
    // -----------------------------------------------------------------------

    private fun configuration(multiThread: Boolean) = mockk<Configuration> {
        every { hasMultipleThreadsPerEndUser } returns multiThread
    }

    private fun chatThreadMock(threadId: UUID, messageCount: Int) = mockk<ChatThread>(relaxed = true) {
        every { id } returns threadId
        every { messages } returns List(messageCount) { mockk(relaxed = true) }
    }

    /** A per-thread handler whose [ChatThreadHandler.threadFlow] and popup flow can be driven independently. */
    private fun chatThreadHandlerMock(
        chatThread: ChatThread,
        threadFlow: MutableSharedFlow<ChatThread> = MutableSharedFlow(replay = 1),
        popupFlow: MutableSharedFlow<Popup> = MutableSharedFlow(replay = 1),
    ): ChatThreadHandler {
        val actionHandler = mockk<ChatThreadActionHandler>(relaxed = true) {
            every { this@mockk.popupFlow } returns popupFlow
        }
        return mockk(relaxed = true) {
            every { get() } returns chatThread
            every { this@mockk.threadFlow } returns threadFlow
            every { actions() } returns actionHandler
        }
    }

    @Test
    fun `satisfaction survey notifies for a thread other than the selected one`() {
        val stateFlow = MutableStateFlow<ChatState>(ChatState.Ready)
        val chatStateViewModel = mockk<ChatStateViewModel> {
            every { state } returns stateFlow
        }

        val chatThreadA = chatThreadMock(UUID.randomUUID(), messageCount = 0)
        val chatThreadB = chatThreadMock(UUID.randomUUID(), messageCount = 0)
        val popupFlowA = MutableSharedFlow<Popup>(replay = 1)
        val popupFlowB = MutableSharedFlow<Popup>(replay = 1)
        val handlerA = chatThreadHandlerMock(chatThreadA, popupFlow = popupFlowA)
        val handlerB = chatThreadHandlerMock(chatThreadB, popupFlow = popupFlowB)

        val threadsFlow = MutableSharedFlow<List<ChatThread>>(replay = 1)
        val threadsHandler = mockk<ChatThreadsHandler>(relaxed = true) {
            every { this@mockk.threadsFlow } returns threadsFlow
            every { thread(match { it.id == chatThreadA.id }) } returns handlerA
            every { thread(match { it.id == chatThreadB.id }) } returns handlerB
        }
        val chat = mockk<Chat>(relaxed = true) {
            every { threads() } returns threadsHandler
            every { this@mockk.configuration } returns configuration(multiThread = true)
        }
        val chatProvider = mockk<ChatInstanceProvider>(relaxed = true) {
            every { this@mockk.chat } returns chat
        }

        val selectedThreadRepository =
            SelectedThreadRepository(chatProvider, valueStorage = mockk<ValueStorage>(relaxed = true), logger = LoggerNoop)
        // Thread A is the one currently open on screen; its survey belongs to ChatThreadViewModel's dialog.
        selectedThreadRepository.chatThreadHandler = handlerA

        val notifyUpdateUseCase = mockk<NotifyUpdateUseCase>(relaxed = true)
        val viewModel = ChatThreadsViewModel(chatProvider, selectedThreadRepository, chatStateViewModel, notifyUpdateUseCase)

        testDispatcher.scheduler.advanceUntilIdle()
        threadsFlow.tryEmit(listOf(chatThreadA, chatThreadB))
        testDispatcher.scheduler.advanceUntilIdle()

        // A survey on the currently selected thread A belongs to ChatThreadViewModel's dialog, not a notification.
        popupFlowA.tryEmit(mockk<Popup.SatisfactionSurvey>(relaxed = true))
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify(exactly = 0) { notifyUpdateUseCase.notifySatisfactionSurvey(any(), any()) }

        // A survey on background thread B must notify directly - no collector is needed.
        val survey = mockk<Popup.SatisfactionSurvey>(relaxed = true)
        popupFlowB.tryEmit(survey)
        testDispatcher.scheduler.advanceUntilIdle()

        val threadSlot = slot<Thread>()
        coVerify(exactly = 1) { notifyUpdateUseCase.notifySatisfactionSurvey(capture(threadSlot), survey) }
        assertEquals(chatThreadB.id, threadSlot.captured.id)

        viewModel.viewModelScope.cancel()
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `background survey subscriptions do not call threadsHandler once chat state leaves Ready`() {
        val stateFlow = MutableStateFlow<ChatState>(ChatState.Ready)
        val chatStateViewModel = mockk<ChatStateViewModel> {
            every { state } returns stateFlow
        }

        val chatThreadA = chatThreadMock(UUID.randomUUID(), messageCount = 0)
        val chatThreadB = chatThreadMock(UUID.randomUUID(), messageCount = 0)
        val handlerA = chatThreadHandlerMock(chatThreadA)
        val handlerB = chatThreadHandlerMock(chatThreadB)

        val threadsFlow = MutableSharedFlow<List<ChatThread>>(replay = 1)
        val threadsHandler = mockk<ChatThreadsHandler>(relaxed = true) {
            every { this@mockk.threadsFlow } returns threadsFlow
            every { thread(match { it.id == chatThreadA.id }) } returns handlerA
            every { thread(match { it.id == chatThreadB.id }) } returns handlerB
        }
        val chat = mockk<Chat>(relaxed = true) {
            every { threads() } returns threadsHandler
            every { this@mockk.configuration } returns configuration(multiThread = true)
        }
        // Stateful, not a fixed `returns chat` - signOut() nulls this before advancing chatState
        // away from Ready, and the test below reproduces exactly that order.
        var currentChat: Chat? = chat
        val chatProvider = mockk<ChatInstanceProvider>(relaxed = true) {
            every { this@mockk.chat } answers { currentChat }
        }

        val selectedThreadRepository =
            SelectedThreadRepository(chatProvider, valueStorage = mockk<ValueStorage>(relaxed = true), logger = LoggerNoop)
        selectedThreadRepository.chatThreadHandler = handlerA // A selected, B in background

        val viewModel = ChatThreadsViewModel(chatProvider, selectedThreadRepository, chatStateViewModel, mockk(relaxed = true))

        testDispatcher.scheduler.advanceUntilIdle()
        threadsFlow.tryEmit(listOf(chatThreadA, chatThreadB))
        testDispatcher.scheduler.advanceUntilIdle()
        // thread.chatThread stays reference-identical to chatThreadB through the pipeline, so an
        // exact-instance verify avoids a mockk limitation where match {} in verify probes with a
        // dummy that only supports equals/hashCode/toString.
        verify(atLeast = 1) { threadsHandler.thread(chatThreadB) }
        clearMocks(threadsHandler, answers = false, recordedCalls = true)

        // Reproduces the real sign-out order: ChatInstanceProvider.signOut() nulls chat (which
        // SelectedThreadRepository.onChatChanged turns into NoThreadHandler synchronously) before
        // advancing chatState away from Ready - so this must run with chat already null and state
        // still Ready, not the other way around.
        currentChat = null
        selectedThreadRepository.chatThreadHandler = NoThreadHandler
        testDispatcher.scheduler.advanceUntilIdle()

        verify(exactly = 0) { threadsHandler.thread(any()) }

        viewModel.viewModelScope.cancel()
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @Test
    fun `background survey subscription is not rebuilt when an unrelated thread's messages change`() {
        val stateFlow = MutableStateFlow<ChatState>(ChatState.Ready)
        val chatStateViewModel = mockk<ChatStateViewModel> {
            every { state } returns stateFlow
        }

        val chatThreadA1 = chatThreadMock(UUID.randomUUID(), messageCount = 0)
        val chatThreadB = chatThreadMock(UUID.randomUUID(), messageCount = 0)
        val handlerA = chatThreadHandlerMock(chatThreadA1)
        val handlerB = chatThreadHandlerMock(chatThreadB)

        val threadsFlow = MutableSharedFlow<List<ChatThread>>(replay = 1)
        val threadsHandler = mockk<ChatThreadsHandler>(relaxed = true) {
            every { this@mockk.threadsFlow } returns threadsFlow
            every { thread(match<ChatThread> { it.id == chatThreadA1.id }) } returns handlerA
            every { thread(chatThreadB) } returns handlerB
        }
        val chat = mockk<Chat>(relaxed = true) {
            every { threads() } returns threadsHandler
            every { this@mockk.configuration } returns configuration(multiThread = true)
        }
        val chatProvider = mockk<ChatInstanceProvider>(relaxed = true) {
            every { this@mockk.chat } returns chat
        }

        val selectedThreadRepository =
            SelectedThreadRepository(chatProvider, valueStorage = mockk<ValueStorage>(relaxed = true), logger = LoggerNoop)
        selectedThreadRepository.chatThreadHandler = handlerA // A selected, B in background

        val viewModel = ChatThreadsViewModel(chatProvider, selectedThreadRepository, chatStateViewModel, mockk(relaxed = true))

        testDispatcher.scheduler.advanceUntilIdle()
        threadsFlow.tryEmit(listOf(chatThreadA1, chatThreadB))
        testDispatcher.scheduler.advanceUntilIdle()
        // Only the survey-subscription code calls .actions() on a background handler - the
        // (pre-existing, unrelated) message-flow subscription in backgroundThreadsFlow only ever
        // touches .threadFlow - so this isolates it from that mechanism's own resubscriptions.
        verify(exactly = 1) { handlerB.actions() }

        // Several new messages arriving in the (unrelated, selected) thread A - each a different
        // ChatThread instance with the same id - must not tear down and resubscribe thread B's
        // background popupFlow. In production, each resubscription sends a redundant
        // RecoverThreadEvent to the backend (see ChatThreadsViewModel.reconcileBackgroundSurveys KDoc).
        repeat(3) { index ->
            val chatThreadAUpdated = chatThreadMock(chatThreadA1.id, messageCount = index + 1)
            every { threadsHandler.thread(match<ChatThread> { it.id == chatThreadAUpdated.id }) } returns handlerA
            threadsFlow.tryEmit(listOf(chatThreadAUpdated, chatThreadB))
            testDispatcher.scheduler.advanceUntilIdle()
        }

        verify(exactly = 1) { handlerB.actions() }

        viewModel.viewModelScope.cancel()
        testDispatcher.scheduler.advanceUntilIdle()
    }
}
