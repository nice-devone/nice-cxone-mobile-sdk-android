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
import app.cash.turbine.test
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.UUID

/**
 * Covers the Satisfaction Survey popup display, its interaction with the EndContact dialog, and
 * the shared isSatisfactionSurveyShowing flag.
 *
 * Split out of [ChatThreadViewModelTest] (detekt LargeClass); setup/fakes are duplicated on purpose.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatThreadViewModelSatisfactionSurveyTest {

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

    private fun configuration(liveChat: Boolean, multiThread: Boolean, allowTranscript: Boolean) =
        mockk<Configuration> {
            every { isLiveChat } returns liveChat
            every { hasMultipleThreadsPerEndUser } returns multiThread
            every { liveChatAllowTranscript } returns allowTranscript
        }

    private fun liveChatConfiguration(): Configuration = configuration(liveChat = true, multiThread = false, allowTranscript = false)

    /**
     * A working fake of the encrypted pending-survey update SelectedThreadRepository actually uses,
     * so a stashSurvey()/takeSurvey() round trip behaves like the real DataStore-backed atomic update.
     */
    private fun fakeSurveyStashStorage(): ValueStorage {
        var stored = ""
        return mockk(relaxed = true) {
            coEvery { updateEncryptedPendingSurvey(any()) } coAnswers {
                stored = firstArg<(String) -> String>()(stored)
            }
        }
    }

    private fun viewModelWithRepository(
        valueStorage: ValueStorage = fakeSurveyStashStorage(),
    ): Pair<ChatThreadViewModel, SelectedThreadRepository> {
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

    // -----------------------------------------------------------------------
    // Satisfaction Survey popup
    // -----------------------------------------------------------------------

    @Test
    fun `satisfaction survey from popupFlow is shown as a dialog`() = runTest(testDispatcher) {
        currentChat = mockk<Chat> { every { configuration } returns liveChatConfiguration() }
        val (viewModel, repo) = viewModelWithRepository(endContactNotYetShownValueStorage())
        val popupFlow = MutableSharedFlow<Popup>()

        viewModel.dialogShown.test {
            assertEquals(ConversationDialog.None, awaitItem())

            repo.chatThreadHandler = fakeHandlerWithPopups(
                stubThread(agent = null, positionInQueue = null, canAddMoreMessages = true),
                popupFlow,
            )
            advanceUntilIdle()

            val survey = mockk<Popup.SatisfactionSurvey>(relaxed = true)
            popupFlow.emit(survey)

            val shown = awaitItem()
            assertTrue(shown is ConversationDialog.Popup)
            assertEquals(survey, (shown as ConversationDialog.Popup).popup)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `end contact is suppressed while satisfaction survey is showing and shown after dismissSatisfactionSurvey`() =
        runTest(testDispatcher) {
            currentChat = mockk<Chat> { every { configuration } returns liveChatConfiguration() }
            val (viewModel, repo) = viewModelWithRepository(endContactNotYetShownValueStorage())
            val popupFlow = MutableSharedFlow<Popup>()
            // isArchived reads chatThreadCachedFlow, which only chatThreadFlow (SharingStarted.Lazily)
            // writes to; isAgentTyping is the flow that keeps chatThreadFlow warm.
            backgroundScope.launch { viewModel.isAgentTyping.collect {} }
            backgroundScope.launch { viewModel.isArchived.collect {} }

            viewModel.dialogShown.test {
                assertEquals(ConversationDialog.None, awaitItem())

                // Survey arrives while the thread can still accept messages.
                repo.chatThreadHandler = fakeHandlerWithPopups(
                    stubThread(agent = null, positionInQueue = null, canAddMoreMessages = true),
                    popupFlow,
                )
                advanceUntilIdle()
                val survey = mockk<Popup.SatisfactionSurvey>(relaxed = true)
                popupFlow.emit(survey)
                assertTrue(awaitItem() is ConversationDialog.Popup)

                // The conversation then ends (archived); without a guard this would auto-show
                // EndContact and bury the survey dialog that is still on screen.
                repo.chatThreadHandler = fakeHandlerWithPopups(
                    stubThread(agent = null, positionInQueue = null, canAddMoreMessages = false),
                    popupFlow,
                )
                advanceUntilIdle()
                expectNoEvents()

                // Dismissing the survey while still archived shows the EndContact dialog it was holding back.
                viewModel.dismissSatisfactionSurvey()
                advanceUntilIdle()
                assertEquals(ConversationDialog.None, awaitItem())
                assertEquals(ConversationDialog.EndContact, awaitItem())

                cancelAndIgnoreRemainingEvents()
            }
        }

    // -----------------------------------------------------------------------
    // Satisfaction Survey popup display across chat modes - the ViewModel collects
    // handler.actions().popupFlow unconditionally, with no chat-mode check, so it must show the
    // dialog the same way whether the active session is LiveChat, MultiThread, or SingleThread.
    // -----------------------------------------------------------------------

    @Test
    fun `satisfaction survey popup is shown for a multi thread session`() = runTest(testDispatcher) {
        currentChat = mockk<Chat> {
            every { configuration } returns configuration(liveChat = false, multiThread = true, allowTranscript = false)
        }
        val (viewModel, repo) = viewModelWithRepository()
        val popupFlow = MutableSharedFlow<Popup>()

        viewModel.dialogShown.test {
            assertEquals(ConversationDialog.None, awaitItem())

            repo.chatThreadHandler = fakeHandlerWithPopups(
                stubThread(agent = null, positionInQueue = null),
                popupFlow,
            )
            advanceUntilIdle()

            val survey = mockk<Popup.SatisfactionSurvey>(relaxed = true)
            popupFlow.emit(survey)

            val dialog = awaitItem()
            val popupDialog = dialog as? ConversationDialog.Popup
            assertEquals(survey, popupDialog?.popup)
            assertEquals(testThreadId, popupDialog?.threadId)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `satisfaction survey popup is shown for a single thread session`() = runTest(testDispatcher) {
        currentChat = mockk<Chat> {
            every { configuration } returns configuration(liveChat = false, multiThread = false, allowTranscript = false)
        }
        val (viewModel, repo) = viewModelWithRepository()
        val popupFlow = MutableSharedFlow<Popup>()

        viewModel.dialogShown.test {
            assertEquals(ConversationDialog.None, awaitItem())

            repo.chatThreadHandler = fakeHandlerWithPopups(
                stubThread(agent = null, positionInQueue = null),
                popupFlow,
            )
            advanceUntilIdle()

            val survey = mockk<Popup.SatisfactionSurvey>(relaxed = true)
            popupFlow.emit(survey)

            val dialog = awaitItem()
            val popupDialog = dialog as? ConversationDialog.Popup
            assertEquals(survey, popupDialog?.popup)
            assertEquals(testThreadId, popupDialog?.threadId)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // -----------------------------------------------------------------------
    // Background recovery - ChatThreadsViewModel.backgroundSurveyFlow consumes a survey arriving
    // for a thread while it's in the background (to post a local notification) and stashes it in
    // SelectedThreadRepository, since popupFlow's one-shot delivery means a fresh collection on
    // selection would otherwise never see it again.
    // -----------------------------------------------------------------------

    @Test
    fun `a survey stashed in the background is recovered and shown as a dialog once the thread is selected`() =
        runTest(testDispatcher) {
            currentChat = mockk<Chat> {
                every { configuration } returns configuration(liveChat = false, multiThread = true, allowTranscript = false)
            }
            val (viewModel, repo) = viewModelWithRepository()
            val popupFlow = MutableSharedFlow<Popup>()
            val survey = mockk<Popup.SatisfactionSurvey>(relaxed = true) {
                every { text } returns "How did we do?"
                every { surveyUrl } returns "https://example.test/survey"
            }
            repo.stashSurvey(testThreadId, survey)

            viewModel.dialogShown.test {
                assertEquals(ConversationDialog.None, awaitItem())

                // No live popupFlow emission here - the stashed survey alone must surface.
                repo.chatThreadHandler = fakeHandlerWithPopups(
                    stubThread(agent = null, positionInQueue = null),
                    popupFlow,
                )
                advanceUntilIdle()

                val dialog = awaitItem()
                val popupDialog = dialog as? ConversationDialog.Popup
                // The recovered popup is reconstructed from persisted text/surveyUrl, not the same
                // instance that was stashed, so compare the fields the dialog actually needs.
                val recoveredSurvey = popupDialog?.popup as? Popup.SatisfactionSurvey
                assertEquals(survey.text, recoveredSurvey?.text)
                assertEquals(survey.surveyUrl, recoveredSurvey?.surveyUrl)
                assertEquals(testThreadId, popupDialog?.threadId)

                cancelAndIgnoreRemainingEvents()
            }
        }
}
