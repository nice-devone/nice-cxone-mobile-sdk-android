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
import com.nice.cxonechat.ChatFieldHandler
import com.nice.cxonechat.ChatInstanceProvider
import com.nice.cxonechat.ChatMode
import com.nice.cxonechat.ChatState
import com.nice.cxonechat.ChatThreadsHandler
import com.nice.cxonechat.log.LoggerNoop
import com.nice.cxonechat.prechat.PreChatSurvey
import com.nice.cxonechat.ui.api.CustomFieldProviderType
import com.nice.cxonechat.ui.api.NoExtraCustomFields
import com.nice.cxonechat.ui.api.UiCustomFieldsProvider
import com.nice.cxonechat.ui.data.repository.SelectedThreadRepository
import com.nice.cxonechat.ui.storage.ValueStorage
import com.nice.cxonechat.ui.viewmodel.ChatViewModel.DialogState.None
import com.nice.cxonechat.ui.viewmodel.ChatViewModel.DialogState.Preparing
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.job
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.Collections
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val chatProvider = mockk<ChatInstanceProvider>(relaxed = true)
    private val selectedThreadRepository =
        SelectedThreadRepository(chatProvider, valueStorage = mockk<ValueStorage>(relaxed = true), logger = LoggerNoop)
    private val chatStateViewModel = mockk<ChatStateViewModel>(relaxed = true)
    private val threads = mockk<ChatThreadsHandler>(relaxed = true)
    private val chat = mockk<Chat>(relaxed = true)

    // Held as a field so individual tests can emit new states to test reactive behaviour.
    private val chatStateFlow = MutableStateFlow<ChatState>(ChatState.Ready)

    private lateinit var viewModel: ChatViewModel

    /** Every ViewModel a test builds, so tearDown drains each one even when the test fails first. */
    private val viewModels = mutableListOf<ChatViewModel>()

    private fun newViewModel() =
        ChatViewModel(selectedThreadRepository, chatProvider, LoggerNoop, chatStateViewModel).also { viewModels += it }

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
        every { chatStateViewModel.state } returns chatStateFlow
        every { chat.chatMode } returns ChatMode.LiveChat
        every { chat.threads() } returns threads
        every { threads.preChatSurvey } returns null

        viewModel = newViewModel()
    }

    @After
    fun tearDown() {
        try {
            viewModels.forEach(::cancelAndAwait)
        } finally {
            Dispatchers.resetMain()
            stopKoin()
        }
    }

    /**
     * Cancels [target]'s coroutines and waits until they end, so none resolves a Koin-injected field provider
     * after stopKoin(). Cancelling alone is not enough: a report past chatWindowOpen() has no suspension point
     * left and ends on a Dispatchers.IO thread, in real time. runTest waits for that in real time, and it also
     * runs the coroutines queued on Main, the test dispatcher, which cannot finish cancelling otherwise.
     */
    private fun cancelAndAwait(target: ChatViewModel) = runTest(testDispatcher, timeout = DRAIN_TIMEOUT) {
        target.viewModelScope.coroutineContext.job.cancelAndJoin()
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
    fun `close lets closeSuspending finish even if its own coroutine is cancelled mid-drain`() = runTest(testDispatcher) {
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

    // ── connect() state-guard tests ────────────────────────────────────────────────────────────

    /**
     * When the SDK is autonomously reconnecting (Connecting), connect() must show the loading
     * overlay so the user sees feedback — but must NOT trigger a second connect() call, which
     * would double-start the reconnect and corrupt the state machine.
     */
    @Test
    fun `connect when already Connecting shows Preparing dialog without calling provider connect`() {
        every { chatProvider.chatState } returns ChatState.Connecting

        viewModel.connect()

        assertEquals(Preparing, viewModel.dialogShown.value)
        verify(exactly = 0) { chatProvider.connect() }
    }

    /**
     * connect() is the normal path after prepare() succeeds — Prepared state must trigger both
     * the loading overlay and the actual connection attempt.
     */
    @Test
    fun `connect when Prepared shows Preparing and calls provider connect`() {
        every { chatProvider.chatState } returns ChatState.Prepared

        viewModel.connect()

        assertEquals(Preparing, viewModel.dialogShown.value)
        verify(exactly = 1) { chatProvider.connect() }
    }

    /**
     * Reconnecting after a dropped connection is the recovery path — ConnectionLost must behave
     * identically to Prepared: show the overlay and start the connection.
     */
    @Test
    fun `connect when ConnectionLost shows Preparing and calls provider connect`() {
        every { chatProvider.chatState } returns ChatState.ConnectionLost

        viewModel.connect()

        assertEquals(Preparing, viewModel.dialogShown.value)
        verify(exactly = 1) { chatProvider.connect() }
    }

    /**
     * The third and last state `connect()` lets through. Worth pinning separately because `Offline`
     * is special-cased elsewhere in this ViewModel in the opposite direction — `dialogShown`
     * auto-dismisses the Preparing overlay when the state becomes `Offline` — so an edit that
     * conflated "offline means do not connect" with that dismissal would silently break retry.
     */
    @Test
    fun `connect when Offline shows Preparing and calls provider connect`() {
        // Only chatProvider.chatState is set to Offline, deliberately. connect() reads that
        // property alone, while chatStateFlow feeds the dialogShown dismissal collector — and
        // Offline is precisely the state that collector dismisses Preparing on. Setting both would
        // arm the dismissal this test asserts against, leaving it green only for as long as nothing
        // advances the scheduler. The transition case is covered by the dialogShown test below.
        every { chatProvider.chatState } returns ChatState.Offline

        viewModel.connect()

        assertEquals(Preparing, viewModel.dialogShown.value)
        verify(exactly = 1) { chatProvider.connect() }
    }

    /**
     * Calling connect() while already Ready is a programming error. The method must log it and
     * take no action — it must not show a spurious Preparing dialog or start a redundant connect.
     * The dialog starts as None in setUp (Ready + LiveChat + no survey → None), and must remain
     * None after the invalid call.
     */
    @Test
    fun `connect when in non-connectable state does not show dialog or call provider connect`() {
        // chatProvider.chatState is ChatState.Ready from setUp — connect() guards against this
        val initialDialog = viewModel.dialogShown.value // initialize lazy as None
        assertEquals("Precondition: the dialog starts as None", None, initialDialog)

        viewModel.connect()

        assertEquals(
            "Dialog state must not change after a no-op connect()",
            None,
            viewModel.dialogShown.value,
        )
        verify(exactly = 0) { chatProvider.connect() }
    }

    // ── onReady() tests ────────────────────────────────────────────────────────────────────────

    /**
     * onReady() is the hook called when the SDK emits the Ready state event. If the loading
     * overlay is still showing (because we were waiting for the SDK to connect), it must dismiss
     * it so the user can interact with the chat.
     */
    @Test
    fun `onReady dismisses the Preparing dialog when the loading overlay is showing`() {
        every { chatProvider.chatState } returns ChatState.Prepared
        viewModel.connect() // puts the dialog into Preparing
        assertEquals(Preparing, viewModel.dialogShown.value)

        viewModel.onReady()

        assertEquals(None, viewModel.dialogShown.value)
    }

    /**
     * onReady() must be a no-op when there is no loading overlay — calling it from an unexpected
     * state (e.g., the ViewModel was never in Preparing) must not cause side-effects.
     */
    @Test
    fun `onReady does nothing when dialog is already None`() {
        assertEquals(None, viewModel.dialogShown.value)

        viewModel.onReady()

        assertEquals(None, viewModel.dialogShown.value)
    }

    // ── refreshDialogState() initial-state tests ───────────────────────────────────────────────

    /**
     * For LiveChat/SingleThread with an active preChatSurvey, the initial dialog must be Preparing
     * (the survey dialog drives thread creation, so we must block interaction until survey
     * completes). Regression guard: a null-check regression would skip the survey and let the
     * user into an uninitialized thread.
     */
    @Test
    fun `initial dialog state is Preparing when preChatSurvey is present in non-MultiThread mode`() {
        every { threads.preChatSurvey } returns mockk<PreChatSurvey>() // non-null survey
        val vmWithSurvey = newViewModel()

        assertEquals(
            "A LiveChat ViewModel with an active preChatSurvey must start in Preparing so the " +
                "survey can be shown before the user can send messages",
            Preparing,
            vmWithSurvey.dialogShown.value,
        )
    }

    /**
     * When the chat is not yet in Ready state (e.g., still Prepared/Preparing), the initial
     * dialog must be Preparing regardless of chat mode. This prevents premature thread resolution
     * attempts before the SDK is connected.
     */
    @Test
    fun `initial dialog state is Preparing when chat is not yet Ready`() {
        // Both sources must move together: ChatStateViewModel.state is fed from
        // ChatInstanceProvider.onChatStateChanged and resyncs from chatInstanceProvider.chatState in
        // its init, so a fixture where they disagree describes a state production cannot reach.
        chatStateFlow.value = ChatState.Prepared // override: not Ready yet
        every { chatProvider.chatState } returns ChatState.Prepared
        val vmNotReady = newViewModel()

        assertEquals(
            "A ViewModel created before the SDK reaches Ready must start in Preparing",
            Preparing,
            vmNotReady.dialogShown.value,
        )
    }

    // ── dialogShown reactive behaviour ─────────────────────────────────────────────────────────

    /**
     * Regression guard: when the backend reports the channel is offline (ChatState.Offline) while
     * the loading overlay is already showing, the overlay must auto-dismiss — leaving it stuck
     * in Preparing would block the user forever with no way to escape. The dismiss is driven by
     * the state collector launched inside the dialogShown lazy initializer.
     */
    @Test
    fun `dialogShown auto-dismisses Preparing dialog when chat state becomes Offline`() = runTest(testDispatcher) {
        every { chatProvider.chatState } returns ChatState.Prepared
        viewModel.connect() // puts dialog into Preparing
        val dialog = viewModel.dialogShown // initialise lazy + launch state collector
        advanceUntilIdle() // let collector subscribe to chatStateFlow

        chatStateFlow.value = ChatState.Offline
        advanceUntilIdle()

        assertEquals(
            "Preparing overlay must be dismissed automatically when the channel goes Offline",
            None,
            dialog.value,
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
        withTimeout(EVENT_TIMEOUT_MS) { triggered.await() }
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
        withTimeout(EVENT_TIMEOUT_MS) { triggered.await() }
        delay(100) // give a stray second report, if the cancellation bug were present, time to surface

        coVerify(exactly = 1) { events.trigger(any()) }
    }

    /**
     * The user can send as soon as the chat is Ready, so the customer custom fields must not wait for
     * the chatWindowOpen() round-trip, or a message sent meanwhile goes out without them.
     */
    @Test
    fun `reportOnResume adds the customer custom fields before reporting chatWindowOpen`() = runBlocking {
        loadKoinModules(
            module {
                single<UiCustomFieldsProvider>(named(CustomFieldProviderType.Customer)) {
                    UiCustomFieldsProvider { mapOf("plan" to "gold") }
                }
            }
        )
        val calls = Collections.synchronizedList(mutableListOf<String>())
        val fieldHandler = mockk<ChatFieldHandler>()
        every { fieldHandler.add(any()) } answers { calls += "add" }
        every { chat.customFields() } returns fieldHandler
        val events = mockk<ChatEventHandler>()
        every { chat.events() } returns events
        val triggered = CompletableDeferred<Unit>()
        coEvery { events.trigger(any()) } coAnswers {
            calls += "chatWindowOpen"
            triggered.complete(Unit)
        }

        viewModel.reportOnResume()
        withTimeout(EVENT_TIMEOUT_MS) { triggered.await() }

        assertEquals(listOf("add", "chatWindowOpen"), calls.toList())
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

    private companion object {
        /** How long a test waits for an expected event from work on Dispatchers.IO, which runs in real time. */
        const val EVENT_TIMEOUT_MS = 2000L

        /** How long tearDown waits for a cancelled ViewModel's coroutines to end. */
        val DRAIN_TIMEOUT = 2.seconds
    }
}
