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
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadsHandler
import com.nice.cxonechat.log.LoggerNoop
import com.nice.cxonechat.prechat.PreChatSurvey
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState
import com.nice.cxonechat.ui.data.repository.SelectedThreadRepository
import com.nice.cxonechat.ui.storage.ValueStorage
import com.nice.cxonechat.ui.storage.ValueStorage.StringKey.EndContactShownForThreadKey
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds

/**
 * [ChatViewModel.selectFirstThread] tests: selecting a closed LiveChat thread on cold start,
 * leaving an already-selected thread untouched until its EndContact has been shown, and
 * resolving a fresh thread afterward - regardless of how refreshThreadState() got called.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatViewModelSelectFirstThreadTest {

    private val testDispatcher = StandardTestDispatcher()
    private val chatProvider = mockk<ChatInstanceProvider>(relaxed = true)
    private val valueStorage = mockk<ValueStorage>(relaxed = true)
    private val selectedThreadRepository =
        SelectedThreadRepository(chatProvider, valueStorage = valueStorage, logger = LoggerNoop)
    private val chatStateViewModel = mockk<ChatStateViewModel>(relaxed = true)
    private val threads = mockk<ChatThreadsHandler>(relaxed = true)
    private val chat = mockk<Chat>(relaxed = true)

    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { chatProvider.chat } returns chat
        every { chatProvider.chatState } returns ChatState.Ready
        every { chatStateViewModel.state } returns MutableStateFlow(ChatState.Ready)
        every { chat.chatMode } returns ChatMode.LiveChat
        every { chat.threads() } returns threads
        every { threads.preChatSurvey } returns null
        // "" decodes to null via getEndContactShownForThread()'s isNotEmpty() check - not shown yet.
        every { valueStorage.getString(EndContactShownForThreadKey) } returns flowOf("")

        viewModel = ChatViewModel(selectedThreadRepository, chatProvider, LoggerNoop, chatStateViewModel)
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
    }

    private fun closedLiveChatThread(id: UUID = UUID.randomUUID()): ChatThread = mockk(relaxed = true) {
        every { this@mockk.id } returns id
        every { canAddMoreMessages } returns false
        every { threadState } returns ChatThreadState.Closed
    }

    private fun selectThread(thread: ChatThread) {
        selectedThreadRepository.chatThreadHandler = mockk<ChatThreadHandler>(relaxed = true) {
            every { get() } returns thread
        }
    }

    @Test
    fun `cold start selects a closed LiveChat thread when nothing was selected yet`() = runTest(testDispatcher) {
        val closedThread = closedLiveChatThread()
        every { threads.threadsFlow } returns MutableStateFlow(listOf(closedThread))
        every { threads.thread(closedThread) } returns mockk(relaxed = true) {
            every { get() } returns closedThread
        }

        viewModel.refreshThreadState()
        // resolveCurrentState() hops to a real Dispatchers.Default thread, which testDispatcher
        // does not control - advanceUntilIdle() alone would not wait for it. Join the real job.
        viewModel.resolveStateJob?.join()

        assertEquals(
            "A closed thread found on the very first resolution must be selected - after the " +
                "core fix, ChatThreadsHandlerLive only ever hands one back when a missed survey is pending",
            closedThread.id,
            selectedThreadRepository.chatThreadHandler.get().id,
        )
        verify { threads.thread(closedThread) }
    }

    @Test
    fun `resume while the selected thread's EndContact was not shown yet leaves it untouched`() = runTest(testDispatcher) {
        val selectedThread = closedLiveChatThread()
        selectThread(selectedThread)
        val handlerBefore = selectedThreadRepository.chatThreadHandler
        // threadsFlow is deliberately left unconfigured: this must never even look at the list.

        viewModel.refreshThreadState()
        viewModel.resolveStateJob?.join()

        verify(exactly = 0) { threads.thread(any()) }
        assertEquals(
            "A thread whose EndContact was not shown yet must stay selected exactly as is",
            handlerBefore,
            selectedThreadRepository.chatThreadHandler,
        )
    }

    @Test
    fun `resume for SingleThread leaves the selected thread untouched without refreshing`() = runTest(testDispatcher) {
        // EndContact is a LiveChat-only concept and is never marked for SingleThread, so the
        // "not shown yet" branch always applies - this locks in that it never refreshes either.
        every { chat.chatMode } returns ChatMode.SingleThread
        val selectedThread = closedLiveChatThread()
        selectThread(selectedThread)
        val handlerBefore = selectedThreadRepository.chatThreadHandler
        // threadsFlow is deliberately left unconfigured: this must never even look at the list.

        viewModel.refreshThreadState()
        viewModel.resolveStateJob?.join()

        verify(exactly = 0) { threads.thread(any()) }
        assertEquals(
            "SingleThread has no EndContact lifecycle - the selected thread must stay exactly as is",
            handlerBefore,
            selectedThreadRepository.chatThreadHandler,
        )
    }

    @Test
    fun `resume after EndContact was shown for the selected thread resolves a fresh one`() = runTest(testDispatcher) {
        val oldClosedThread = closedLiveChatThread()
        selectThread(oldClosedThread)
        every { valueStorage.getString(EndContactShownForThreadKey) } returns flowOf(oldClosedThread.id.toString())

        val freshThread = closedLiveChatThread().also { every { it.canAddMoreMessages } returns true }
        every { threads.threadsFlow } returns MutableStateFlow(listOf(freshThread))
        every { threads.thread(freshThread) } returns mockk(relaxed = true) {
            every { get() } returns freshThread
        }

        viewModel.refreshThreadState()
        viewModel.resolveStateJob?.join()

        assertEquals(
            "EndContact already shown means the old thread's lifecycle is done - a fresh one must be selected",
            freshThread.id,
            selectedThreadRepository.chatThreadHandler.get().id,
        )
    }

    @Test
    fun `relaunching after Close chat creates a new thread instead of re-showing the archived one`() =
        runTest(testDispatcher) {
            // "Close chat" only finishes the Activity (ChatActivity.kt: closeChat = { finish() }) -
            // it never clears chatThreadHandler. Simulates relaunching with a fresh ChatViewModel
            // sharing the same process-wide SelectedThreadRepository.
            val oldClosedThread = closedLiveChatThread()
            selectThread(oldClosedThread)
            every { valueStorage.getString(EndContactShownForThreadKey) } returns flowOf(oldClosedThread.id.toString())

            val freshThread = closedLiveChatThread().also { every { it.canAddMoreMessages } returns true }
            every { threads.threadsFlow } returns MutableStateFlow(listOf(freshThread))
            every { threads.thread(freshThread) } returns mockk(relaxed = true) {
                every { get() } returns freshThread
            }
            val relaunched = ChatViewModel(selectedThreadRepository, chatProvider, LoggerNoop, chatStateViewModel)

            relaunched.refreshThreadState()
            relaunched.resolveStateJob?.join()

            assertEquals(
                "Relaunching must select the freshly recovered thread, not re-show the old archived one",
                freshThread.id,
                selectedThreadRepository.chatThreadHandler.get().id,
            )
            relaunched.viewModelScope.cancel()
        }

    @Test
    fun `New Conversation waits for a fresh thread instead of the stale replayed one`() = runTest(testDispatcher) {
        // threadsFlow is a real SDK SharedFlow with replay(1): tapping "New Conversation" without
        // the Chat instance being closed and recreated means this flow is still warm, and a new
        // collector (flow.first()) would otherwise immediately see the already-replayed stale
        // closed thread instead of waiting for the fresh result threads.refresh() triggers.
        val oldClosedThread = closedLiveChatThread()
        selectThread(oldClosedThread)
        every { valueStorage.getString(EndContactShownForThreadKey) } returns flowOf(oldClosedThread.id.toString())

        val sharedThreadsFlow = MutableSharedFlow<List<ChatThread>>(replay = 1)
        sharedThreadsFlow.tryEmit(listOf(oldClosedThread))
        every { threads.threadsFlow } returns sharedThreadsFlow
        val freshThread = closedLiveChatThread().also { every { it.canAddMoreMessages } returns true }
        every { threads.thread(freshThread) } returns mockk(relaxed = true) {
            every { get() } returns freshThread
        }
        // refresh() is fire-and-forget in production; the fresh result is driven explicitly below
        // instead, once flow.first() has genuinely subscribed and already seen the stale replay.

        viewModel.refreshThreadState()

        // resolveCurrentState() hops to a real Dispatchers.Default thread, which testDispatcher
        // does not control - wait on real time for flow.first() to actually subscribe (meaning it
        // already observed the stale replayed value and, with the fix, is waiting past it) before
        // emitting the fresh one. Without the fix, flow.first() would already have returned the
        // stale value the instant it subscribed, long before this wait is satisfied.
        withContext(Dispatchers.Default) {
            withTimeout(2_000.milliseconds) {
                sharedThreadsFlow.subscriptionCount.first { it >= 1 }
            }
        }
        sharedThreadsFlow.emit(listOf(freshThread))
        viewModel.resolveStateJob?.join()

        assertEquals(
            "Must select the freshly recovered thread, not the stale one threadsFlow had already replayed",
            freshThread.id,
            selectedThreadRepository.chatThreadHandler.get().id,
        )
    }

    @Test
    fun `cold start with a pre-chat survey pending resolves immediately instead of hanging`() = runTest(testDispatcher) {
        // LiveChat channel with a pre-chat survey configured: recovery intentionally returns no
        // thread until the survey is answered, so threadsFlow's first emission is an empty list.
        // There is no stale thread here (nothing was selected yet) - must not be mistaken for one.
        val survey = mockk<PreChatSurvey>(relaxed = true)
        every { threads.preChatSurvey } returns survey
        every { threads.threadsFlow } returns MutableStateFlow(emptyList())

        viewModel.refreshThreadState()

        // resolveCurrentState() hops to a real Dispatchers.Default thread - wait on real time so a
        // regression (selectFirstThread() hanging instead of resolving) fails this test instead of
        // the test itself hanging forever.
        withContext(Dispatchers.Default) {
            withTimeout(2_000.milliseconds) {
                viewModel.resolveStateJob?.join()
            }
        }

        assertEquals(
            "An empty recovery result on a genuine cold start must resolve to showing the pre-chat survey",
            ChatViewModel.DialogState.Survey(survey),
            viewModel.dialogShown.value,
        )
    }
}
