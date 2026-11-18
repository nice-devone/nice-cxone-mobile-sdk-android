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

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nice.cxonechat.Chat
import com.nice.cxonechat.ChatInstanceProvider
import com.nice.cxonechat.ChatThreadActionHandler
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.Popup
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerNoop
import com.nice.cxonechat.state.Configuration
import com.nice.cxonechat.thread.Agent
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.ui.UiModule
import com.nice.cxonechat.ui.data.repository.SelectedThreadRepository
import com.nice.cxonechat.ui.data.source.ContentDataSourceList
import com.nice.cxonechat.ui.data.source.SentAttachmentSeeder
import com.nice.cxonechat.ui.storage.ValueStorage
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.UUID

/**
 * Covers the shared isSatisfactionSurveyShowing flag - must never stay stuck true, or every
 * future pre-chat survey wait (ChatViewModel/ChatThreadsViewModel's
 * satisfactionSurveyShowingFlow.first { !it }) blocks forever.
 *
 * Split out of [ChatThreadViewModelSatisfactionSurveyTest] (detekt LargeClass); setup/fakes are
 * duplicated on purpose.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatThreadViewModelSatisfactionSurveyFlagTest {

    private val testDispatcher = StandardTestDispatcher()

    private var currentChat: Chat? = null

    private val testThreadId: UUID = UUID.randomUUID()

    private val scopedViewModels = mutableListOf<ChatThreadViewModel>()

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
        scopedViewModels.forEach { it.viewModelScope.cancel() }
        scopedViewModels.clear()
        Dispatchers.resetMain()
        stopKoin()
    }

    private fun liveChatConfiguration(): Configuration = mockk<Configuration> {
        every { isLiveChat } returns true
        every { hasMultipleThreadsPerEndUser } returns false
        every { liveChatAllowTranscript } returns false
    }

    private fun viewModelWithRepository(valueStorage: ValueStorage): Pair<ChatThreadViewModel, SelectedThreadRepository> {
        val chatProvider = mockk<ChatInstanceProvider>(relaxed = true) {
            every { chat } answers { currentChat }
        }
        val repo = SelectedThreadRepository(chatProvider, valueStorage = valueStorage, logger = LoggerNoop)
        val vm = ChatThreadViewModel(
            contentDataSource = mockk<ContentDataSourceList>(relaxed = true),
            selectedThreadRepository = repo,
            chatProvider = chatProvider,
            logger = LoggerNoop,
            sentAttachmentSeeder = mockk<SentAttachmentSeeder>(relaxed = true),
        )
        // Registered for teardown rather than cancelled at the end of each test: a failing
        // assertion would skip an in-test cancel and leak the viewModelScope into the next test.
        scopedViewModels += vm
        return vm to repo
    }

    /** Starting state for [ValueStorage.EndContactShownForThreadKey]: End Conversation not yet shown for any thread. */
    private fun endContactNotYetShownValueStorage(): ValueStorage = mockk(relaxed = true) {
        every { getString(ValueStorage.StringKey.EndContactShownForThreadKey) } returns flowOf("")
        coEvery { setString(ValueStorage.StringKey.EndContactShownForThreadKey, any()) } returns Unit
    }

    private fun fakeHandlerWithPopups(thread: ChatThread, popupFlow: MutableSharedFlow<Popup>): ChatThreadHandler =
        mockk(relaxed = true) {
            every { get() } returns thread
            every { threadFlow } returns flowOf(thread)
            every { actions() } returns mockk<ChatThreadActionHandler>(relaxed = true) {
                every { this@mockk.popupFlow } returns popupFlow
            }
        }

    private fun stubThread(agent: Agent?, positionInQueue: Int?, canAddMoreMessages: Boolean = true, id: UUID = testThreadId): ChatThread =
        mockk(relaxed = true) {
            every { this@mockk.id } returns id
            every { threadAgent } returns agent
            every { this@mockk.positionInQueue } returns positionInQueue
            every { messages } returns emptyList()
            every { scrollToken } returns ""
            every { this@mockk.canAddMoreMessages } returns canAddMoreMessages
        }

    @Test
    fun `onCleared resets the shared flag so a survey left open does not block future chats`() = runTest(testDispatcher) {
        currentChat = mockk<Chat> { every { configuration } returns liveChatConfiguration() }
        val (viewModel, repo) = viewModelWithRepository(endContactNotYetShownValueStorage())
        viewModel.setDialogHostAttached(true)
        val popupFlow = MutableSharedFlow<Popup>()

        repo.chatThreadHandler = fakeHandlerWithPopups(
            stubThread(agent = null, positionInQueue = null, canAddMoreMessages = true),
            popupFlow,
        )
        advanceUntilIdle()
        popupFlow.emit(mockk<Popup.SatisfactionSurvey>(relaxed = true))
        advanceUntilIdle()
        assertTrue(repo.isSatisfactionSurveyShowing)

        // The hosting Activity finishing without the user ever dismissing the survey - never
        // reaches dismissSatisfactionSurvey(), so only onCleared() can clear the flag.
        storeHolding(viewModel).clear()

        assertFalse(repo.isSatisfactionSurveyShowing)
    }

    @Test
    fun `replacing the survey dialog with another one clears the shared flag`() = runTest(testDispatcher) {
        currentChat = mockk<Chat> { every { configuration } returns liveChatConfiguration() }
        val (viewModel, repo) = viewModelWithRepository(endContactNotYetShownValueStorage())
        viewModel.setDialogHostAttached(true)
        val popupFlow = MutableSharedFlow<Popup>()

        repo.chatThreadHandler = fakeHandlerWithPopups(
            stubThread(agent = null, positionInQueue = null, canAddMoreMessages = true),
            popupFlow,
        )
        advanceUntilIdle()
        popupFlow.emit(mockk<Popup.SatisfactionSurvey>(relaxed = true))
        advanceUntilIdle()
        assertTrue(repo.isSatisfactionSurveyShowing)

        // Any dialog change away from the survey, not just dismissSatisfactionSurvey() itself,
        // must clear the flag - e.g. EndContact overwriting it via the race covered above.
        viewModel.showEndContactDialog()
        advanceUntilIdle()

        assertFalse(repo.isSatisfactionSurveyShowing)
    }

    @Test
    fun `the shared flag is set synchronously when the survey arrives, not after a separate scheduling gap`() =
        runTest(testDispatcher) {
            currentChat = mockk<Chat> { every { configuration } returns liveChatConfiguration() }
            val (viewModel, repo) = viewModelWithRepository(endContactNotYetShownValueStorage())
            viewModel.setDialogHostAttached(true)
            val popupFlow = MutableSharedFlow<Popup>()

            repo.chatThreadHandler = fakeHandlerWithPopups(
                stubThread(agent = null, positionInQueue = null, canAddMoreMessages = true),
                popupFlow,
            )
            advanceUntilIdle()

            // Deliberately no advanceUntilIdle() between the emit and the assertion below: a
            // pre-chat survey check racing in that gap must never see this as still false.
            popupFlow.emit(mockk<Popup.SatisfactionSurvey>(relaxed = true))

            assertTrue(repo.isSatisfactionSurveyShowing)
        }

    @Test
    fun `a survey received while the dialog host is detached does not block the shared flag, and surfaces once attached`() =
        runTest(testDispatcher) {
            currentChat = mockk<Chat> { every { configuration } returns liveChatConfiguration() }
            val (viewModel, repo) = viewModelWithRepository(endContactNotYetShownValueStorage())
            val popupFlow = MutableSharedFlow<Popup>()

            // The dialog host (ThreadDialogView) is not composed - e.g. the user navigated back to
            // ThreadList while this activity-scoped view model is still alive.
            repo.chatThreadHandler = fakeHandlerWithPopups(
                stubThread(agent = null, positionInQueue = null, canAddMoreMessages = true),
                popupFlow,
            )
            advanceUntilIdle()
            val survey = mockk<Popup.SatisfactionSurvey>(relaxed = true)
            popupFlow.emit(survey)
            advanceUntilIdle()

            assertFalse(repo.isSatisfactionSurveyShowing)
            // Still recovered later: the survey itself is not dropped, only the shared flag.
            assertEquals(survey, (viewModel.dialogShown.value as? ConversationDialog.Popup)?.popup)

            viewModel.setDialogHostAttached(true)
            advanceUntilIdle()

            assertTrue(repo.isSatisfactionSurveyShowing)
        }

    @Test
    fun `a stale survey dialog for a previous thread is cleared when a different thread is selected`() = runTest(testDispatcher) {
        currentChat = mockk<Chat> { every { configuration } returns liveChatConfiguration() }
        val (viewModel, repo) = viewModelWithRepository(endContactNotYetShownValueStorage())
        viewModel.setDialogHostAttached(true)
        val popupFlowA = MutableSharedFlow<Popup>()

        repo.chatThreadHandler = fakeHandlerWithPopups(
            stubThread(agent = null, positionInQueue = null, canAddMoreMessages = true, id = testThreadId),
            popupFlowA,
        )
        advanceUntilIdle()
        val survey = mockk<Popup.SatisfactionSurvey>(relaxed = true)
        popupFlowA.emit(survey)
        advanceUntilIdle()
        assertEquals(survey, (viewModel.dialogShown.value as? ConversationDialog.Popup)?.popup)

        // A different thread is selected - the previous thread's survey dialog must not resurface
        // over it.
        val otherThreadId = UUID.randomUUID()
        repo.chatThreadHandler = fakeHandlerWithPopups(
            stubThread(agent = null, positionInQueue = null, canAddMoreMessages = true, id = otherThreadId),
            MutableSharedFlow(),
        )
        advanceUntilIdle()

        assertEquals(ConversationDialog.None, viewModel.dialogShown.value)
        assertFalse(repo.isSatisfactionSurveyShowing)
    }

    @Test
    fun `a survey is restored when its thread is reselected after being cleared by a thread switch`() =
        runTest(testDispatcher) {
            currentChat = mockk<Chat> { every { configuration } returns liveChatConfiguration() }
            val (viewModel, repo) = viewModelWithRepository(endContactNotYetShownValueStorage())
            val popupFlowA = MutableSharedFlow<Popup>()
            val handlerA = fakeHandlerWithPopups(
                stubThread(agent = null, positionInQueue = null, canAddMoreMessages = true, id = testThreadId),
                popupFlowA,
            )

            // Received while the dialog host is detached (e.g. the user is on ThreadList) -
            // core already marks it delivered, so this is the only copy that will ever exist.
            repo.chatThreadHandler = handlerA
            advanceUntilIdle()
            val survey = mockk<Popup.SatisfactionSurvey>(relaxed = true)
            popupFlowA.emit(survey)
            advanceUntilIdle()

            // Switching to another thread must not lose the only copy of the undisplayed survey.
            val otherThreadId = UUID.randomUUID()
            repo.chatThreadHandler = fakeHandlerWithPopups(
                stubThread(agent = null, positionInQueue = null, canAddMoreMessages = true, id = otherThreadId),
                MutableSharedFlow(),
            )
            advanceUntilIdle()
            assertEquals(ConversationDialog.None, viewModel.dialogShown.value)

            // Reselecting thread A must restore the survey instead of leaving it lost forever.
            repo.chatThreadHandler = handlerA
            advanceUntilIdle()

            assertEquals(survey, (viewModel.dialogShown.value as? ConversationDialog.Popup)?.popup)
        }

    /**
     * Puts [viewModel] into a [ViewModelStore] so [ViewModelStore.clear] drives the real
     * `onCleared()` - a test can't call the `protected` override directly.
     */
    private fun storeHolding(viewModel: ChatThreadViewModel): ViewModelStore = ViewModelStore().also { store ->
        ViewModelProvider.create(
            store = store,
            factory = viewModelFactory {
                initializer { viewModel }
            },
        )[ChatThreadViewModel::class]
    }
}
