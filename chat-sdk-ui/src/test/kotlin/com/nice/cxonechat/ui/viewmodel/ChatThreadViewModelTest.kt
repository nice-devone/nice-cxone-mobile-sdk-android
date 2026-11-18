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
import com.nice.cxonechat.thread.ContactHandler
import com.nice.cxonechat.ui.UiModule
import com.nice.cxonechat.ui.data.repository.SelectedThreadRepository
import com.nice.cxonechat.ui.data.source.ContentDataSourceList
import com.nice.cxonechat.ui.data.source.SentAttachmentSeeder
import com.nice.cxonechat.ui.storage.ValueStorage
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatThreadViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private var currentChat: Chat? = null

    /**
     * Fresh per test: JUnit 4 constructs a new instance of the test class for every `@Test`
     * method, so each test gets its own id and no handler state bleeds across tests.
     */
    private val testThreadId: UUID = UUID.randomUUID()

    /** ViewModels whose [viewModelScope] must be cancelled once the test finishes, pass or fail. */
    private val scopedViewModels = mutableListOf<ChatThreadViewModel>()

    private val seeder = mockk<SentAttachmentSeeder>(relaxed = true)

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

    private fun viewModel(): ChatThreadViewModel {
        val chatProvider = mockk<ChatInstanceProvider>(relaxed = true) {
            every { chat } answers { currentChat }
        }
        val repo = SelectedThreadRepository(chatProvider, valueStorage = mockk<ValueStorage>(relaxed = true), logger = LoggerNoop)
        return ChatThreadViewModel(
            contentDataSource = mockk<ContentDataSourceList>(relaxed = true),
            selectedThreadRepository = repo,
            chatProvider = chatProvider,
            logger = LoggerNoop,
            sentAttachmentSeeder = seeder,
        ).also(scopedViewModels::add)
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

    // -----------------------------------------------------------------------
    // positionInQueue display rule
    // -----------------------------------------------------------------------
    //
    // Every test below must collect isAgentTyping before touching positionInQueue (assertPositionInQueue
    // does), because the two read different flows: positionInQueue maps chatThreadCachedFlow, but the
    // only writer of that cache is an onEach inside the private chatThreadFlow, which is
    // stateIn(viewModelScope, SharingStarted.Lazily, NoThread) and so stays cold until something
    // subscribes to it. isAgentTyping is the accessible flow that does subscribe to chatThreadFlow,
    // and collecting it is what starts the chatThreadHandlerFlow -> flatMapLatest { threadFlow } ->
    // chatThreadCachedFlow pipeline. Without it every awaitItem() below hangs on the seed value.

    private fun viewModelWithRepository(): Pair<ChatThreadViewModel, SelectedThreadRepository> {
        val chatProvider = mockk<ChatInstanceProvider>(relaxed = true) {
            every { chat } answers { currentChat }
        }
        val repo = SelectedThreadRepository(chatProvider, valueStorage = mockk<ValueStorage>(relaxed = true), logger = LoggerNoop)
        val vm = ChatThreadViewModel(
            contentDataSource = mockk<ContentDataSourceList>(relaxed = true),
            selectedThreadRepository = repo,
            chatProvider = chatProvider,
            logger = LoggerNoop,
            sentAttachmentSeeder = seeder,
        )
        // Registered for teardown rather than cancelled at the end of each test: a failing
        // assertion would skip an in-test cancel and leak the viewModelScope into the next test.
        scopedViewModels += vm
        return vm to repo
    }

    private fun fakeHandlerFor(
        thread: ChatThread,
        popups: Flow<Popup> = emptyFlow(),
    ): ChatThreadHandler = mockk(relaxed = true) {
        every { get() } returns thread
        every { threadFlow } returns flowOf(thread)
        every { actions() } returns mockk<ChatThreadActionHandler>(relaxed = true) {
            every { popupFlow } returns popups
        }
    }

    private fun stubThread(
        agent: Agent?,
        positionInQueue: Int?,
        contactHandler: ContactHandler? = null,
    ): ChatThread = mockk(relaxed = true) {
        every { id } returns testThreadId
        every { threadAgent } returns agent
        every { this@mockk.positionInQueue } returns positionInQueue
        every { this@mockk.contactHandler } returns contactHandler
        every { messages } returns emptyList()
        every { scrollToken } returns ""
    }

    private fun stubAgent(): Agent = mockk(relaxed = true)

    /** Starts from the seed `null`, then shows each thread in turn and awaits its expected position. */
    private fun assertPositionInQueue(vararg steps: Pair<ChatThread, Int?>) = runTest(testDispatcher) {
        val (viewModel, repo) = viewModelWithRepository()
        backgroundScope.launch { viewModel.isAgentTyping.collect {} }

        viewModel.positionInQueue.test {
            assertEquals(null, awaitItem())
            steps.forEach { (thread, expected) ->
                repo.chatThreadHandler = fakeHandlerFor(thread)
                assertEquals(expected, awaitItem())
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `positionInQueue is null when an agent is assigned`() = assertPositionInQueue(
        stubThread(agent = null, positionInQueue = 5) to 5,
        stubThread(agent = stubAgent(), positionInQueue = 5) to null,
    )

    @Test
    fun `positionInQueue is exposed when no agent is assigned`() = assertPositionInQueue(
        stubThread(agent = null, positionInQueue = 5) to 5,
    )

    @Test
    fun `positionInQueue is null when no agent and no position`() = assertPositionInQueue(
        // Transition through a non-null value so distinctUntilChanged does not mask the null.
        stubThread(agent = null, positionInQueue = 5) to 5,
        stubThread(agent = null, positionInQueue = null) to null,
    )

    @Test
    fun `positionInQueue zero is exposed when no agent is assigned`() = assertPositionInQueue(
        stubThread(agent = null, positionInQueue = 0) to 0,
    )

    @Test
    fun `positionInQueue zero is hidden when an agent is assigned`() = assertPositionInQueue(
        stubThread(agent = null, positionInQueue = 0) to 0,
        // Position 0 is falsy in many languages; the guard must not key off the position value.
        stubThread(agent = stubAgent(), positionInQueue = 0) to null,
    )

    @Test
    fun `positionInQueue is hidden while a Cognigy bot handles the contact and returns when the handler is cleared`() =
        assertPositionInQueue(
            stubThread(null, 5, contactHandler = null) to 5,
            stubThread(null, 4, ContactHandler.Cognigy) to null,
            stubThread(null, 3, contactHandler = null) to 3,
        )

    @Test
    fun `positionInQueue is hidden while an unrecognised handler holds the contact`() = assertPositionInQueue(
        stubThread(null, 5, contactHandler = null) to 5,
        stubThread(null, 4, ContactHandler.Unknown) to null,
    )

    @Test
    fun `positionInQueue is exposed while CXone handles the contact`() = assertPositionInQueue(
        stubThread(null, 5, ContactHandler.CXone) to 5,
    )

    @Test
    fun `positionInQueue ignores position updates while the bot holds the contact`() = runTest(testDispatcher) {
        val (viewModel, repo) = viewModelWithRepository()
        backgroundScope.launch { viewModel.isAgentTyping.collect {} }

        viewModel.positionInQueue.test {
            assertEquals(null, awaitItem())

            repo.chatThreadHandler = fakeHandlerFor(stubThread(null, 5, contactHandler = null))
            assertEquals(5, awaitItem())

            repo.chatThreadHandler = fakeHandlerFor(stubThread(null, 3, ContactHandler.Cognigy))
            assertEquals(null, awaitItem())

            // The 2 may be conflated with the 7; the awaited Cognigy step above pins the rule.
            repo.chatThreadHandler = fakeHandlerFor(stubThread(null, 2, ContactHandler.Cognigy))
            repo.chatThreadHandler = fakeHandlerFor(stubThread(null, 7, contactHandler = null))
            assertEquals(7, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `positionInQueue is hidden when an agent is assigned while the bot handler is set`() = assertPositionInQueue(
        stubThread(null, 5, ContactHandler.CXone) to 5,
        stubThread(stubAgent(), 5, ContactHandler.Cognigy) to null,
    )
}
