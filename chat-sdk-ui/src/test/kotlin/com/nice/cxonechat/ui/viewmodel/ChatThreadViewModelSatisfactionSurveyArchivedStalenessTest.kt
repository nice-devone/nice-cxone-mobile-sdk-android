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
import kotlinx.coroutines.flow.MutableStateFlow
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
 * Covers isArchived's staleness while uncollected, and restoring a cached survey undoing a
 * premature EndContact mark. Split out of [ChatThreadViewModelSatisfactionSurveyEndContactTest].
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatThreadViewModelSatisfactionSurveyArchivedStalenessTest {

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

    private fun liveChatConfiguration(): Configuration = mockk {
        every { isLiveChat } returns true
        every { hasMultipleThreadsPerEndUser } returns false
        every { liveChatAllowTranscript } returns false
    }

    private fun viewModelWithRepository(
        valueStorage: ValueStorage = mockk(relaxed = true),
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
        // Registered for teardown so a failing assertion can't leak the viewModelScope.
        scopedViewModels += vm
        return vm to repo
    }

    // Stateful - getString must reflect a prior setString, otherwise a test using this fake would
    // pass even if the production code it is meant to guard were removed.
    private fun endContactNotYetShownValueStorage(): ValueStorage {
        val endContactShownForThread = MutableStateFlow("")
        return mockk(relaxed = true) {
            every { getString(ValueStorage.StringKey.EndContactShownForThreadKey) } returns endContactShownForThread
            coEvery {
                setString(ValueStorage.StringKey.EndContactShownForThreadKey, any())
            } coAnswers { call ->
                endContactShownForThread.value = call.invocation.args[1] as String
            }
        }
    }

    private fun fakeHandlerWithPopups(thread: ChatThread, popupFlow: MutableSharedFlow<Popup>): ChatThreadHandler =
        mockk(relaxed = true) {
            every { get() } returns thread
            every { threadFlow } returns flowOf(thread)
            every { actions() } returns mockk<ChatThreadActionHandler>(relaxed = true) {
                every { this@mockk.popupFlow } returns popupFlow
            }
        }

    private fun stubThread(agent: Agent?, positionInQueue: Int?, canAddMoreMessages: Boolean = true): ChatThread =
        mockk(relaxed = true) {
            every { id } returns testThreadId
            every { threadAgent } returns agent
            every { this@mockk.positionInQueue } returns positionInQueue
            every { messages } returns emptyList()
            every { scrollToken } returns ""
            every { this@mockk.canAddMoreMessages } returns canAddMoreMessages
        }

    @Test
    fun `dismissSatisfactionSurvey shows EndContact even if isArchived was never recomputed`() =
        runTest(testDispatcher) {
            // isArchived is WhileSubscribed - it only updates while actively collected (e.g. by
            // ChatConversation's lifecycle-aware collector, paused while backgrounded). Deliberately
            // not collecting it here simulates it staying stale at its initial `false`, even though
            // the thread (read directly from the handler) is already archived.
            currentChat = mockk<Chat> { every { configuration } returns liveChatConfiguration() }
            val (viewModel, repo) = viewModelWithRepository(endContactNotYetShownValueStorage())
            val popupFlow = MutableSharedFlow<Popup>()
            backgroundScope.launch { viewModel.isAgentTyping.collect {} }

            viewModel.dialogShown.test {
                assertEquals(ConversationDialog.None, awaitItem())

                repo.chatThreadHandler = fakeHandlerWithPopups(
                    stubThread(agent = null, positionInQueue = null, canAddMoreMessages = false),
                    popupFlow,
                )
                advanceUntilIdle()

                val survey = mockk<Popup.SatisfactionSurvey>(relaxed = true)
                popupFlow.emit(survey)
                assertTrue(awaitItem() is ConversationDialog.Popup)

                viewModel.dismissSatisfactionSurvey()
                advanceUntilIdle()
                assertEquals(ConversationDialog.None, awaitItem())
                assertEquals(ConversationDialog.EndContact, awaitItem())

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `restoring a cached survey on a handler replacement also undoes a premature EndContact mark`() =
        runTest(testDispatcher) {
            currentChat = mockk<Chat> { every { configuration } returns liveChatConfiguration() }
            val (viewModel, repo) = viewModelWithRepository(endContactNotYetShownValueStorage())
            val popupFlow = MutableSharedFlow<Popup>()
            backgroundScope.launch { viewModel.isAgentTyping.collect {} }
            backgroundScope.launch { viewModel.isArchived.collect {} }

            viewModel.dialogShown.test {
                assertEquals(ConversationDialog.None, awaitItem())

                repo.chatThreadHandler = fakeHandlerWithPopups(
                    stubThread(agent = null, positionInQueue = null, canAddMoreMessages = false),
                    popupFlow,
                )
                advanceUntilIdle()
                assertEquals(ConversationDialog.EndContact, awaitItem())

                val survey = mockk<Popup.SatisfactionSurvey>(relaxed = true)
                popupFlow.emit(survey)
                assertTrue(awaitItem() is ConversationDialog.Popup)

                // Simulates EndContact being (wrongly) re-marked for this thread after the survey
                // was already cached - e.g. a later, independently-timed isArchived re-evaluation.
                // Without the fix, restoring the cached survey below would leave this mark in place.
                repo.withEndContactLock { repo.setEndContactShownForThread(testThreadId.toString()) }

                // A handler replacement for the same thread restores the cached survey via
                // collectLatest - no new dialogShown emission, since the restored Popup(survey,
                // threadId) is structurally equal to what is already showing.
                repo.chatThreadHandler = fakeHandlerWithPopups(
                    stubThread(agent = null, positionInQueue = null, canAddMoreMessages = false),
                    MutableSharedFlow(),
                )
                advanceUntilIdle()
                expectNoEvents()

                // The restore must have undone the mark, so dismissing still shows the real EndContact.
                viewModel.dismissSatisfactionSurvey()
                advanceUntilIdle()
                assertEquals(ConversationDialog.None, awaitItem())
                assertEquals(ConversationDialog.EndContact, awaitItem())

                cancelAndIgnoreRemainingEvents()
            }
        }
}
