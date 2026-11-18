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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
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
import java.io.IOException
import java.util.UUID

/**
 * Covers the race between the EndContact auto-show and a Satisfaction Survey recovered for the
 * same thread. Split out of [ChatThreadViewModelSatisfactionSurveyTest] (detekt LargeClass).
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatThreadViewModelSatisfactionSurveyEndContactTest {

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
    fun `end contact auto-shown by a race before the survey arrives does not block the later real end contact`() =
        runTest(testDispatcher) {
            currentChat = mockk<Chat> { every { configuration } returns liveChatConfiguration() }
            val (viewModel, repo) = viewModelWithRepository(endContactNotYetShownValueStorage())
            val popupFlow = MutableSharedFlow<Popup>()
            backgroundScope.launch { viewModel.isAgentTyping.collect {} }
            backgroundScope.launch { viewModel.isArchived.collect {} }

            viewModel.dialogShown.test {
                assertEquals(ConversationDialog.None, awaitItem())

                // The thread reports archived before the survey for it arrives, so EndContact wins.
                repo.chatThreadHandler = fakeHandlerWithPopups(
                    stubThread(agent = null, positionInQueue = null, canAddMoreMessages = false),
                    popupFlow,
                )
                advanceUntilIdle()
                assertEquals(ConversationDialog.EndContact, awaitItem())

                // The survey then overwrites EndContact - the premature mark must be undone.
                val survey = mockk<Popup.SatisfactionSurvey>(relaxed = true)
                popupFlow.emit(survey)
                assertTrue(awaitItem() is ConversationDialog.Popup)

                // Dismissing the survey must still show the real EndContact.
                viewModel.dismissSatisfactionSurvey()
                advanceUntilIdle()
                assertEquals(ConversationDialog.None, awaitItem())
                assertEquals(ConversationDialog.EndContact, awaitItem())

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `the survey is still shown even if undoing the EndContact mark fails`() = runTest(testDispatcher) {
        currentChat = mockk<Chat> { every { configuration } returns liveChatConfiguration() }
        // Stateful - a static flowOf would make dismissSatisfactionSurvey()'s read trivially
        // pass regardless of whether the undo write below actually failed to update it.
        val endContactShownForThread = MutableStateFlow("")
        val valueStorage: ValueStorage = mockk(relaxed = true) {
            every { getString(ValueStorage.StringKey.EndContactShownForThreadKey) } returns endContactShownForThread
            coEvery {
                setString(ValueStorage.StringKey.EndContactShownForThreadKey, any())
            } coAnswers { call ->
                val value = call.invocation.args[1] as String
                // The undo write (clearing the mark) fails - must not crash the collector, lose
                // the survey, or leave the backing value cleared.
                if (value.isEmpty()) throw IOException("disk full")
                endContactShownForThread.value = value
            }
        }
        val (viewModel, repo) = viewModelWithRepository(valueStorage)
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

            // Must still be shown even though undoing the EndContact mark throws.
            val survey = mockk<Popup.SatisfactionSurvey>(relaxed = true)
            popupFlow.emit(survey)
            assertTrue(awaitItem() is ConversationDialog.Popup)

            // The persisted undo failed above, but the in-memory cache was updated regardless -
            // dismissing must still show EndContact, not wrongly skip it on a stale disk marker.
            viewModel.dismissSatisfactionSurvey()
            advanceUntilIdle()
            assertEquals(ConversationDialog.None, awaitItem())
            assertEquals(ConversationDialog.EndContact, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a handler replacement during the EndContact undo write does not prevent the survey from being shown`() =
        runTest(testDispatcher) {
            currentChat = mockk<Chat> { every { configuration } returns liveChatConfiguration() }
            val undoWriteGate = CompletableDeferred<Unit>()
            val endContactShownForThread = MutableStateFlow("")
            val valueStorage: ValueStorage = mockk(relaxed = true) {
                every { getString(ValueStorage.StringKey.EndContactShownForThreadKey) } returns endContactShownForThread
                coEvery {
                    setString(ValueStorage.StringKey.EndContactShownForThreadKey, any())
                } coAnswers { call ->
                    val value = call.invocation.args[1] as String
                    // The undo write (clearing the mark) suspends - a handler replacement arriving
                    // before it completes must not cancel the survey publish that happens before it.
                    if (value.isEmpty()) undoWriteGate.await()
                    endContactShownForThread.value = value
                }
            }
            val (viewModel, repo) = viewModelWithRepository(valueStorage)
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

                // The survey arrives and must be published before the undo write below suspends.
                val survey = mockk<Popup.SatisfactionSurvey>(relaxed = true)
                popupFlow.emit(survey)
                assertTrue(awaitItem() is ConversationDialog.Popup)

                // A new handler arrives while the undo write is still gated - this cancels the old
                // popupFlow collection (and its pending undo), but must not revert the survey.
                repo.chatThreadHandler = fakeHandlerWithPopups(
                    stubThread(agent = null, positionInQueue = null, canAddMoreMessages = false),
                    MutableSharedFlow(),
                )
                advanceUntilIdle()
                expectNoEvents()

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `EndContact is still shown even if persisting the shown-for-thread mark fails`() = runTest(testDispatcher) {
        currentChat = mockk<Chat> { every { configuration } returns liveChatConfiguration() }
        // Stateful - a static flowOf would make a later getEndContactShownForThread() read trivially
        // consistent with this write, regardless of whether the write below actually failed.
        val endContactShownForThread = MutableStateFlow("")
        val valueStorage: ValueStorage = mockk(relaxed = true) {
            every { getString(ValueStorage.StringKey.EndContactShownForThreadKey) } returns endContactShownForThread
            coEvery {
                setString(ValueStorage.StringKey.EndContactShownForThreadKey, any())
            } coAnswers { call ->
                val value = call.invocation.args[1] as String
                // The "mark shown" write (triggered by archival below) fails - EndContact must
                // still be shown to the user even though the mark never reached disk.
                if (value == testThreadId.toString()) throw IOException("disk full")
                endContactShownForThread.value = value
            }
        }
        val (viewModel, repo) = viewModelWithRepository(valueStorage)
        val popupFlow = MutableSharedFlow<Popup>()
        backgroundScope.launch { viewModel.isAgentTyping.collect {} }
        backgroundScope.launch { viewModel.isArchived.collect {} }

        viewModel.dialogShown.test {
            assertEquals(ConversationDialog.None, awaitItem())

            // Archival triggers the auto-show, whose persisted mark write throws above.
            repo.chatThreadHandler = fakeHandlerWithPopups(
                stubThread(agent = null, positionInQueue = null, canAddMoreMessages = false),
                popupFlow,
            )
            advanceUntilIdle()
            assertEquals(ConversationDialog.EndContact, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `EndContact is still shown even if reading the shown-for-thread mark fails`() = runTest(testDispatcher) {
        currentChat = mockk<Chat> { every { configuration } returns liveChatConfiguration() }
        val valueStorage: ValueStorage = mockk(relaxed = true) {
            // The read (triggered by archival below) fails - EndContact must still be shown even
            // though the marker could not be checked.
            every { getString(ValueStorage.StringKey.EndContactShownForThreadKey) } returns flow { throw IOException("disk full") }
        }
        val (viewModel, repo) = viewModelWithRepository(valueStorage)
        val popupFlow = MutableSharedFlow<Popup>()
        backgroundScope.launch { viewModel.isAgentTyping.collect {} }
        backgroundScope.launch { viewModel.isArchived.collect {} }

        viewModel.dialogShown.test {
            assertEquals(ConversationDialog.None, awaitItem())

            // Archival triggers the auto-show, whose marker read throws above.
            repo.chatThreadHandler = fakeHandlerWithPopups(
                stubThread(agent = null, positionInQueue = null, canAddMoreMessages = false),
                popupFlow,
            )
            advanceUntilIdle()
            assertEquals(ConversationDialog.EndContact, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a survey arriving while the EndContact marker write is in flight is not overwritten by EndContact`() =
        runTest(testDispatcher) {
            currentChat = mockk<Chat> { every { configuration } returns liveChatConfiguration() }
            val markerWriteGate = CompletableDeferred<Unit>()
            // Real StateFlow (not a static flowOf) so the undo write is actually observable.
            val endContactShownForThread = MutableStateFlow("")
            val valueStorage: ValueStorage = mockk(relaxed = true) {
                every { getString(ValueStorage.StringKey.EndContactShownForThreadKey) } returns endContactShownForThread
                coEvery {
                    setString(ValueStorage.StringKey.EndContactShownForThreadKey, any())
                } coAnswers { call ->
                    val value = call.invocation.args[1] as String
                    // Suspends only on the marker write, mirroring the real suspension point.
                    if (value == testThreadId.toString()) markerWriteGate.await()
                    endContactShownForThread.value = value
                }
            }
            val (viewModel, repo) = viewModelWithRepository(valueStorage)
            val popupFlow = MutableSharedFlow<Popup>()
            backgroundScope.launch { viewModel.isAgentTyping.collect {} }
            backgroundScope.launch { viewModel.isArchived.collect {} }

            viewModel.dialogShown.test {
                assertEquals(ConversationDialog.None, awaitItem())

                // Establish the thread first so threadId resolution isn't itself part of the race.
                repo.chatThreadHandler = fakeHandlerWithPopups(
                    stubThread(agent = null, positionInQueue = null, canAddMoreMessages = true),
                    popupFlow,
                )
                advanceUntilIdle()

                // Archival starts the marker write and suspends on it.
                repo.chatThreadHandler = fakeHandlerWithPopups(
                    stubThread(agent = null, positionInQueue = null, canAddMoreMessages = false),
                    popupFlow,
                )
                advanceUntilIdle()

                // The survey arrives while that write is still in flight and is shown normally.
                val survey = mockk<Popup.SatisfactionSurvey>(relaxed = true)
                popupFlow.emit(survey)
                assertTrue(awaitItem() is ConversationDialog.Popup)

                // Letting the write finish must not overwrite the survey now on screen.
                markerWriteGate.complete(Unit)
                advanceUntilIdle()
                expectNoEvents()

                // The marker must have been undone, so dismissing the survey still shows EndContact.
                viewModel.dismissSatisfactionSurvey()
                advanceUntilIdle()
                assertEquals(ConversationDialog.None, awaitItem())
                assertEquals(ConversationDialog.EndContact, awaitItem())

                cancelAndIgnoreRemainingEvents()
            }
        }
}
