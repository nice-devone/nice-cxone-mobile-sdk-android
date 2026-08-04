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
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadsHandler
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerNoop
import com.nice.cxonechat.state.Configuration
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.ui.UiModule
import com.nice.cxonechat.ui.data.repository.SelectedThreadRepository
import com.nice.cxonechat.ui.domain.model.Thread
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
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

@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatThreadsViewModelTest {

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
    fun `threadFlow resubscribes to fresh threadsHandler after Ready to Connecting to Ready transition`() {
        val stateFlow = MutableStateFlow<ChatState>(ChatState.Connecting)
        val chatStateViewModel = mockk<ChatStateViewModel> {
            every { state } returns stateFlow
        }

        // chat1: provider for the first SDK session
        val threadsFlow1 = MutableSharedFlow<List<ChatThread>>(replay = 1)
        val handler1 = mockk<ChatThreadsHandler>(relaxed = true) {
            every { threadsFlow } returns threadsFlow1
        }
        val chat1 = mockk<Chat>(relaxed = true) {
            every { threads() } returns handler1
        }

        // chat2: provider for the reconnected SDK session
        val threadsFlow2 = MutableSharedFlow<List<ChatThread>>(replay = 1)
        val handler2 = mockk<ChatThreadsHandler>(relaxed = true) {
            every { threadsFlow } returns threadsFlow2
        }
        val chat2 = mockk<Chat>(relaxed = true) {
            every { threads() } returns handler2
        }

        val chatProvider = mockk<ChatInstanceProvider>(relaxed = true) {
            every { chat } returns chat1
        }

        val viewModel = ChatThreadsViewModel(
            chatProvider,
            SelectedThreadRepository(chatProvider),
            chatStateViewModel,
        )

        val scope = TestScope(testDispatcher)
        val results = mutableListOf<List<Thread>>()
        val collectJob = scope.launch {
            viewModel.threads.collect { results.add(it) }
        }

        // Start collecting — subscribers trigger Lazily-started StateFlows
        testDispatcher.scheduler.advanceUntilIdle()

        // Phase 1: transition to Ready; handler1 emits 1 thread
        val chatThread1 = mockk<ChatThread>(relaxed = true)
        stateFlow.value = ChatState.Ready
        threadsFlow1.tryEmit(listOf(chatThread1))
        testDispatcher.scheduler.advanceUntilIdle()

        val phase1Results = results.filter { it.isNotEmpty() }
        assertTrue("Phase 1: at least one non-empty emission from chat1 handler", phase1Results.isNotEmpty())
        assertEquals("Phase 1: chat1 handler emits 1 thread", 1, phase1Results.last().size)

        // Phase 2: disconnect — simulate close() + reconnect replacing the Chat instance
        stateFlow.value = ChatState.Connecting
        every { chatProvider.chat } returns chat2
        testDispatcher.scheduler.advanceUntilIdle()

        // Phase 3: Ready again; handler2 emits 2 threads — verifies flatMapLatest resubscribed
        val chatThread2 = mockk<ChatThread>(relaxed = true)
        stateFlow.value = ChatState.Ready
        threadsFlow2.tryEmit(listOf(chatThread1, chatThread2))
        testDispatcher.scheduler.advanceUntilIdle()

        val phase3Results = results.filter { it.size == 2 }
        assertTrue(
            "Phase 3: threadFlow must resubscribe to chat2's handler after reconnect — " +
                    "if flatMapLatest did not resubscribe, handler2 emissions would never reach the collector",
            phase3Results.isNotEmpty()
        )
        assertEquals("Phase 3: chat2 handler emits 2 threads", 2, results.last().size)

        // Cancel the ViewModel scope so the Eagerly-started shareIn coroutine does not outlive the test
        viewModel.viewModelScope.cancel()
        testDispatcher.scheduler.advanceUntilIdle()
        collectJob.cancel()
        scope.cancel()
    }

    private fun configuration(multiThread: Boolean) = mockk<Configuration> {
        every { hasMultipleThreadsPerEndUser } returns multiThread
    }

    private fun chatThreadMock(threadId: UUID, messageCount: Int) = mockk<ChatThread>(relaxed = true) {
        every { id } returns threadId
        every { messages } returns List(messageCount) { mockk(relaxed = true) }
    }

    /** A mocked multi-thread chat session with its top-level and per-thread update flows exposed. */
    private data class ThreadSession(
        val chat: Chat,
        val threadsFlow: MutableSharedFlow<List<ChatThread>>,
        val perThreadFlow: MutableSharedFlow<ChatThread>,
    )

    private fun threadSession(): ThreadSession {
        val perThreadFlow = MutableSharedFlow<ChatThread>(replay = 1)
        val perThreadHandler = mockk<ChatThreadHandler>(relaxed = true) {
            every { threadFlow } returns perThreadFlow
        }
        val threadsFlow = MutableSharedFlow<List<ChatThread>>(replay = 1)
        val handler = mockk<ChatThreadsHandler>(relaxed = true) {
            every { this@mockk.threadsFlow } returns threadsFlow
            every { thread(any()) } returns perThreadHandler
        }
        val chat = mockk<Chat>(relaxed = true) {
            every { threads() } returns handler
            every { this@mockk.configuration } returns configuration(multiThread = true)
        }
        return ThreadSession(chat, threadsFlow, perThreadFlow)
    }

    @Test
    fun `threads list reflects an incoming message even when backgroundThreadsFlow is never collected`() {
        val stateFlow = MutableStateFlow<ChatState>(ChatState.Ready)
        val chatStateViewModel = mockk<ChatStateViewModel> {
            every { state } returns stateFlow
        }

        val threadId = UUID.randomUUID()
        val chatThreadBefore = chatThreadMock(threadId, messageCount = 1)
        val chatThreadAfter = chatThreadMock(threadId, messageCount = 2)

        val session = threadSession()
        val chatProvider = mockk<ChatInstanceProvider>(relaxed = true) {
            every { this@mockk.chat } returns session.chat
        }

        val viewModel = ChatThreadsViewModel(
            chatProvider,
            SelectedThreadRepository(chatProvider),
            chatStateViewModel,
        )

        val scope = TestScope(testDispatcher)
        val results = mutableListOf<List<Thread>>()
        // viewModel.backgroundThreadsFlow is intentionally never collected here — this reproduces
        // browsing the thread list without ever opening a thread (BackgroundThreadUpdates() in
        // ChatActivity is only composed inside ThreadScreen, never ThreadListScreen).
        val collectJob = scope.launch {
            viewModel.threads.collect { results.add(it) }
        }

        testDispatcher.scheduler.advanceUntilIdle()
        session.threadsFlow.tryEmit(listOf(chatThreadBefore))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Baseline: listed thread shows 1 message", 1, results.last().first().messages.toList().size)

        session.perThreadFlow.tryEmit(chatThreadAfter)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            "An incoming message on a listed thread must refresh the visible list even though " +
                    "no collector ever subscribed to backgroundThreadsFlow",
            2,
            results.last().first().messages.toList().size
        )

        viewModel.viewModelScope.cancel()
        testDispatcher.scheduler.advanceUntilIdle()
        collectJob.cancel()
        scope.cancel()
    }

    @Test
    fun `threads picks up messages from the reconnected session's thread handler after background and foreground`() {
        val stateFlow = MutableStateFlow<ChatState>(ChatState.Ready)
        val chatStateViewModel = mockk<ChatStateViewModel> {
            every { state } returns stateFlow
        }

        val threadId = UUID.randomUUID()
        // Same visible content (same id, same message count) before and after reconnect —
        // must not trivially force threadList (a StateFlow) to re-emit.
        val chatThreadStable = chatThreadMock(threadId, messageCount = 1)
        val chatThreadUpdated = chatThreadMock(threadId, messageCount = 2)

        val session1 = threadSession() // pre-background session — must NOT deliver the update
        val session2 = threadSession() // post-reconnect session

        val chatProvider = mockk<ChatInstanceProvider>(relaxed = true) {
            every { this@mockk.chat } returns session1.chat
        }

        val viewModel = ChatThreadsViewModel(
            chatProvider,
            SelectedThreadRepository(chatProvider),
            chatStateViewModel,
        )

        val scope = TestScope(testDispatcher)
        val results = mutableListOf<List<Thread>>()
        val collectJob = scope.launch {
            viewModel.threads.collect { results.add(it) }
        }

        testDispatcher.scheduler.advanceUntilIdle()
        session1.threadsFlow.tryEmit(listOf(chatThreadStable))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Baseline: listed thread shows 1 message", 1, results.last().first().messages.toList().size)

        // Background/foreground cycle: ChatActivity.onStop() closes chat, onResume() waits for a
        // fresh Ready state — a full session rebuild, same visible list content.
        stateFlow.value = ChatState.Connecting
        every { chatProvider.chat } returns session2.chat
        testDispatcher.scheduler.advanceUntilIdle()
        stateFlow.value = ChatState.Ready
        session2.threadsFlow.tryEmit(listOf(chatThreadStable))
        testDispatcher.scheduler.advanceUntilIdle()

        // Emit the incoming message only on the reconnected session's per-thread flow.
        session2.perThreadFlow.tryEmit(chatThreadUpdated)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(
            "After background/foreground, the list must follow the reconnected session's " +
                    "per-thread handler, not stay stuck on the pre-reconnect (closed) one",
            2,
            results.last().first().messages.toList().size
        )

        viewModel.viewModelScope.cancel()
        testDispatcher.scheduler.advanceUntilIdle()
        collectJob.cancel()
        scope.cancel()
    }
}
