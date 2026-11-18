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
import com.nice.cxonechat.log.LoggerNoop
import com.nice.cxonechat.ui.api.CustomFieldProviderType
import com.nice.cxonechat.ui.api.NoExtraCustomFields
import com.nice.cxonechat.ui.api.UiCustomFieldsProvider
import com.nice.cxonechat.ui.data.repository.SelectedThreadRepository
import com.nice.cxonechat.ui.storage.ValueStorage
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlin.time.Duration.Companion.seconds

/**
 * [ChatViewModel.reportOnResume] once the chat is Ready: a pause with the chatWindowOpen() request in
 * flight, a chat replaced meanwhile, and a customer field provider that throws. The report runs on
 * Dispatchers.IO, so these tests use runBlocking with real CompletableDeferred gates rather than
 * virtual time.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatViewModelInFlightReportTest {

    private val testDispatcher = StandardTestDispatcher()
    private val chatProvider = mockk<ChatInstanceProvider>(relaxed = true)
    private val chatStateViewModel = mockk<ChatStateViewModel>(relaxed = true)
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
        val selectedThreadRepository =
            SelectedThreadRepository(chatProvider, valueStorage = mockk<ValueStorage>(relaxed = true), logger = LoggerNoop)
        viewModel = ChatViewModel(selectedThreadRepository, chatProvider, LoggerNoop, chatStateViewModel)
    }

    @After
    fun tearDown() {
        try {
            cancelAndAwait(viewModel)
        } finally {
            Dispatchers.resetMain()
            stopKoin()
        }
    }

    /**
     * Cancels [target]'s coroutines and waits until they end, so none resolves a Koin-injected field provider
     * after stopKoin(). A report past the Ready wait reads the provider with no suspension point before it, on
     * a Dispatchers.IO thread, in real time, so cancelling alone does not stop it. runTest waits for that in
     * real time, and it also runs the coroutines queued on Main, the test dispatcher, which cannot finish
     * cancelling otherwise.
     */
    private fun cancelAndAwait(target: ChatViewModel) = runTest(testDispatcher, timeout = DRAIN_TIMEOUT) {
        target.viewModelScope.coroutineContext.job.cancelAndJoin()
    }

    @Test
    fun `cancelReportOnResume while chatWindowOpen is in flight keeps the customer custom fields added`() = runBlocking {
        val customerFields = mapOf("plan" to "gold")
        loadKoinModules(
            module {
                single<UiCustomFieldsProvider>(named(CustomFieldProviderType.Customer)) { UiCustomFieldsProvider { customerFields } }
            }
        )
        val events = mockk<ChatEventHandler>(relaxed = true)
        every { chat.events() } returns events
        val reportStarted = CompletableDeferred<Unit>()
        coEvery { events.trigger(any()) } coAnswers {
            reportStarted.complete(Unit)
            awaitCancellation()
        }
        val fieldHandler = mockk<ChatFieldHandler>(relaxed = true)
        every { chat.customFields() } returns fieldHandler

        viewModel.reportOnResume()
        withTimeout(EVENT_TIMEOUT_MS) { reportStarted.await() }
        viewModel.cancelReportOnResume() // onPause() while the chatWindowOpen() request is in flight

        verify(exactly = 1) { fieldHandler.add(customerFields) }
    }

    @Test
    fun `reportOnResume neither adds the fields nor reports to a chat replaced while the fields are read`() = runBlocking {
        val otherFieldHandler = mockk<ChatFieldHandler>(relaxed = true)
        val otherChat = mockk<Chat>(relaxed = true)
        every { otherChat.customFields() } returns otherFieldHandler
        val fieldHandler = mockk<ChatFieldHandler>(relaxed = true)
        every { chat.customFields() } returns fieldHandler
        val providerRan = CompletableDeferred<Unit>()
        loadKoinModules(
            module {
                single<UiCustomFieldsProvider>(named(CustomFieldProviderType.Customer)) {
                    UiCustomFieldsProvider {
                        every { chatProvider.chat } returns otherChat // e.g. signOut() and a new chat meanwhile
                        providerRan.complete(Unit)
                        mapOf("plan" to "gold")
                    }
                }
            }
        )
        val events = mockk<ChatEventHandler>(relaxed = true)
        every { chat.events() } returns events
        val reported = CompletableDeferred<Unit>()
        coEvery { events.trigger(any()) } coAnswers { reported.complete(Unit) }

        viewModel.reportOnResume()
        withTimeout(EVENT_TIMEOUT_MS) { providerRan.await() }

        assertNull(
            "Must not report chatWindowOpen to the replaced chat",
            withTimeoutOrNull(NO_EVENT_WAIT_MS) { reported.await() },
        )
        verify(exactly = 0) { fieldHandler.add(any()) }
        verify(exactly = 0) { otherFieldHandler.add(any()) }
        verify(exactly = 0) { otherChat.events() }
    }

    @Test
    fun `reportOnResume still reports chatWindowOpen when the customer custom fields provider throws`() = runBlocking {
        loadKoinModules(
            module {
                single<UiCustomFieldsProvider>(named(CustomFieldProviderType.Customer)) {
                    UiCustomFieldsProvider { error("integrator provider failed") }
                }
            }
        )
        val events = mockk<ChatEventHandler>(relaxed = true)
        every { chat.events() } returns events
        val reported = CompletableDeferred<Unit>()
        coEvery { events.trigger(any()) } coAnswers { reported.complete(Unit) }

        viewModel.reportOnResume()

        withTimeout(EVENT_TIMEOUT_MS) { reported.await() }
    }

    @Test
    fun `reportOnResume ends without reporting when the customer custom fields provider throws CancellationException`() =
        runBlocking {
            val providerRan = CompletableDeferred<Unit>()
            loadKoinModules(
                module {
                    single<UiCustomFieldsProvider>(named(CustomFieldProviderType.Customer)) {
                        UiCustomFieldsProvider {
                            providerRan.complete(Unit)
                            throw CancellationException("integrator provider cancelled")
                        }
                    }
                }
            )
            val fieldHandler = mockk<ChatFieldHandler>(relaxed = true)
            every { chat.customFields() } returns fieldHandler
            val events = mockk<ChatEventHandler>(relaxed = true)
            every { chat.events() } returns events
            val reported = CompletableDeferred<Unit>()
            coEvery { events.trigger(any()) } coAnswers { reported.complete(Unit) }

            viewModel.reportOnResume()
            withTimeout(EVENT_TIMEOUT_MS) { providerRan.await() }

            assertNull(
                "Must not report chatWindowOpen once the provider signalled cancellation",
                withTimeoutOrNull(NO_EVENT_WAIT_MS) { reported.await() },
            )
            verify(exactly = 0) { fieldHandler.add(any()) }
        }

    /**
     * The request (and any token refresh it starts) must not outlive the screen: a sign-out after
     * leaving it cancels and drains only the core session's jobs, not this ViewModel's.
     */
    @Test
    fun `cancelReportOnResume cancels an in-flight chatWindowOpen request`() = runBlocking {
        val events = mockk<ChatEventHandler>(relaxed = true)
        every { chat.events() } returns events
        val reportStarted = CompletableDeferred<Unit>()
        val requestCancelled = CompletableDeferred<Unit>()
        coEvery { events.trigger(any()) } coAnswers {
            reportStarted.complete(Unit)
            try {
                awaitCancellation()
            } finally {
                requestCancelled.complete(Unit)
            }
        }

        viewModel.reportOnResume()
        withTimeout(EVENT_TIMEOUT_MS) { reportStarted.await() }
        viewModel.cancelReportOnResume()

        withTimeout(EVENT_TIMEOUT_MS) { requestCancelled.await() }
    }

    private companion object {
        /** How long a test waits for an expected event from work on Dispatchers.IO, which runs in real time. */
        const val EVENT_TIMEOUT_MS = 10_000L

        /** How long a test waits to confirm an event does not happen. */
        const val NO_EVENT_WAIT_MS = 300L

        /** How long tearDown waits for a cancelled ViewModel's coroutines to end. */
        val DRAIN_TIMEOUT = 10.seconds
    }
}
