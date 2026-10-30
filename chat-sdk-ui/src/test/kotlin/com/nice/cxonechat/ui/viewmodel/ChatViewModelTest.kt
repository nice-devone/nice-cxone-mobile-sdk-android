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
import com.nice.cxonechat.ChatEventHandler
import com.nice.cxonechat.ChatInstanceProvider
import com.nice.cxonechat.ChatMode
import com.nice.cxonechat.ChatState
import com.nice.cxonechat.ChatThreadsHandler
import com.nice.cxonechat.log.LoggerNoop
import com.nice.cxonechat.ui.api.CustomFieldProviderType
import com.nice.cxonechat.ui.api.NoExtraCustomFields
import com.nice.cxonechat.ui.api.UiCustomFieldsProvider
import com.nice.cxonechat.ui.data.repository.SelectedThreadRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val chatProvider = mockk<ChatInstanceProvider>(relaxed = true)
    private val selectedThreadRepository = SelectedThreadRepository(chatProvider)
    private val chatStateViewModel = mockk<ChatStateViewModel>(relaxed = true)
    private val threads = mockk<ChatThreadsHandler>(relaxed = true)
    private val chat = mockk<Chat>(relaxed = true)

    private lateinit var viewModel: ChatViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        startKoin {
            modules(
                module {
                    single<UiCustomFieldsProvider>(named(CustomFieldProviderType.Customer)) { NoExtraCustomFields }
                    single<UiCustomFieldsProvider>(named(CustomFieldProviderType.Contact)) { NoExtraCustomFields }
                }
            )
        }
        every { chatProvider.chat } returns chat
        every { chatProvider.chatState } returns ChatState.Ready
        every { chatStateViewModel.state } returns MutableStateFlow(ChatState.Ready)
        every { chat.chatMode } returns ChatMode.LiveChat
        every { chat.threads() } returns threads
        every { threads.preChatSurvey } returns null

        viewModel = ChatViewModel(selectedThreadRepository, chatProvider, LoggerNoop, chatStateViewModel)
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
        Dispatchers.resetMain()
        stopKoin()
    }

    @Test
    fun `refreshThreadState cancels previous in-flight resolveStateJob when called a second time`() {
        viewModel.refreshThreadState()
        val firstJob = viewModel.resolveStateJob
        assertNotNull("resolveStateJob should be set after first call", firstJob)

        viewModel.refreshThreadState()

        // cancel() is called synchronously in refreshThreadState() before launching the new coroutine,
        // so isCancelled is true immediately — no need to advance the scheduler.
        assertTrue("First resolveStateJob must be cancelled when refreshThreadState is called again", firstJob!!.isCancelled)
        assertNotSame("Each refreshThreadState call must produce a distinct Job instance", firstJob, viewModel.resolveStateJob)
    }

    /**
     * Regression test: close() is launched from Activity.lifecycleScope, which is cancelled at
     * ON_DESTROY. If the drain is cancelled mid-flight, storage/cookie cleanup may not finish --
     * close() must let closeSuspending() run to completion regardless of the caller's cancellation.
     */
    @Test
    fun `close lets closeSuspending finish even if its own coroutine is cancelled mid-drain`() = runTest {
        val drainGate = CompletableDeferred<Unit>()
        val drainCompleted = AtomicBoolean(false)
        coEvery { chatProvider.closeSuspending() } coAnswers {
            drainGate.await()
            drainCompleted.set(true)
        }

        val closeJob = launch { viewModel.close() }
        advanceUntilIdle()
        closeJob.cancel()

        drainGate.complete(Unit)
        advanceUntilIdle()

        assertTrue(
            "close() must let closeSuspending() finish even when its own coroutine is cancelled",
            drainCompleted.get()
        )
    }

    /**
     * reportOnResume() launches on Dispatchers.IO (not the test's Main dispatcher), so these tests
     * use runBlocking with a real CompletableDeferred hook instead of runTest's virtual time, which
     * cannot observe that coroutine.
     */
    private fun mockEventsWithTriggerHook(): Pair<ChatEventHandler, CompletableDeferred<Unit>> {
        val events = mockk<ChatEventHandler>(relaxed = true)
        every { chat.events() } returns events
        val triggered = CompletableDeferred<Unit>()
        coEvery { events.trigger(any()) } coAnswers { triggered.complete(Unit) }
        return events to triggered
    }

    @Test
    fun `reportOnResume reports chatWindowOpen only after state becomes Ready`() = runBlocking {
        val stateFlow = MutableStateFlow(ChatState.Prepared)
        every { chatStateViewModel.state } returns stateFlow
        val (events, triggered) = mockEventsWithTriggerHook()

        viewModel.reportOnResume()
        assertNull(
            "Must not report chatWindowOpen before Ready",
            withTimeoutOrNull(200) { triggered.await() }
        )

        stateFlow.value = ChatState.Ready
        withTimeout(2000) { triggered.await() }
        coVerify(exactly = 1) { events.trigger(any()) }
    }

    @Test
    fun `reportOnResume called twice before Ready reports chatWindowOpen exactly once`() = runBlocking {
        val stateFlow = MutableStateFlow(ChatState.Prepared)
        every { chatStateViewModel.state } returns stateFlow
        val (events, triggered) = mockEventsWithTriggerHook()

        viewModel.reportOnResume() // rotation / Home-and-back before Ready: a second call follows
        viewModel.reportOnResume()
        stateFlow.value = ChatState.Ready
        withTimeout(2000) { triggered.await() }
        delay(100) // give a stray second report, if the cancellation bug were present, time to surface

        coVerify(exactly = 1) { events.trigger(any()) }
    }

    @Test
    fun `cancelReportOnResume prevents chatWindowOpen from being reported once Ready arrives`() = runBlocking {
        val stateFlow = MutableStateFlow(ChatState.Prepared)
        every { chatStateViewModel.state } returns stateFlow
        val (events, _) = mockEventsWithTriggerHook()

        viewModel.reportOnResume()
        viewModel.cancelReportOnResume()
        stateFlow.value = ChatState.Ready
        delay(300) // real wait: the (cancelled) job runs on Dispatchers.IO, not this test's dispatcher

        coVerify(exactly = 0) { events.trigger(any()) }
    }
}
