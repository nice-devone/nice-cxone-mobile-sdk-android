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

import android.net.Uri
import androidx.lifecycle.viewModelScope
import app.cash.turbine.test
import com.nice.cxonechat.Chat
import com.nice.cxonechat.ChatInstanceProvider
import com.nice.cxonechat.ChatState
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadMessageHandler
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerNoop
import com.nice.cxonechat.message.ContentDescriptor
import com.nice.cxonechat.message.OutboundMessage
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.ui.UiModule
import com.nice.cxonechat.ui.data.repository.SelectedThreadRepository
import com.nice.cxonechat.ui.data.source.ContentDataSourceList
import com.nice.cxonechat.ui.data.source.ContentDataSourceList.ContentRequestResult
import com.nice.cxonechat.ui.data.source.SentAttachmentSeeder
import com.nice.cxonechat.ui.storage.ValueStorage
import com.nice.cxonechat.ui.util.preview.message.SdkMessage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
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
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Covers attachment sending: echo-seed adoption, cancel-during-upload cleanup, and the race
 * between an echo message arriving and its send() call registering.
 *
 * Split out of [ChatThreadViewModelTest] (detekt LargeClass); setup/fakes are duplicated on purpose.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatThreadViewModelAttachmentTest {

    private val testDispatcher = StandardTestDispatcher()

    private var currentChat: Chat? = null

    private val testThreadId: UUID = UUID.randomUUID()

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

    private fun viewModelWithRepository(
        contentDataSource: ContentDataSourceList = mockk(relaxed = true),
    ): Pair<ChatThreadViewModel, SelectedThreadRepository> {
        val chatProvider = mockk<ChatInstanceProvider>(relaxed = true) {
            every { chat } answers { currentChat }
        }
        val repo = SelectedThreadRepository(chatProvider, valueStorage = mockk<ValueStorage>(relaxed = true), logger = LoggerNoop)
        val vm = ChatThreadViewModel(
            contentDataSource = contentDataSource,
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

    private fun fakeHandlerFor(thread: ChatThread): ChatThreadHandler = mockk(relaxed = true) {
        every { get() } returns thread
        every { threadFlow } returns flowOf(thread)
    }

    private fun fakeHandlerFor(thread: ChatThread, messages: ChatThreadMessageHandler): ChatThreadHandler =
        mockk(relaxed = true) {
            every { get() } returns thread
            every { threadFlow } returns flowOf(thread)
            every { messages() } returns messages
        }

    private fun stubThread(): ChatThread = mockk(relaxed = true) {
        every { id } returns testThreadId
        every { messages } returns emptyList()
        every { scrollToken } returns ""
    }

    private fun pickedPdfSource(): ContentDataSourceList = mockk {
        val descriptor = mockk<ContentDescriptor>(relaxed = true) {
            every { mimeType } returns "application/pdf"
            every { friendlyName } returns "picked.pdf"
        }
        coEvery { descriptorForUri(any()) } answers { ContentRequestResult.Success(firstArg(), descriptor) }
    }

    @Test
    fun `an echoed message is shown only after its seeds are adopted`() = runTest(testDispatcher) {
        val adopted = AtomicBoolean(false)
        coEvery { seeder.adoptEchoes(any()) } coAnswers {
            if (firstArg<Map<UUID, SdkMessage>>().isNotEmpty()) {
                delay(ADOPT_DELAY_MS)
                adopted.set(true)
            }
        }
        val echo = mockk<SdkMessage>(relaxed = true) {
            every { id } returns UUID.randomUUID()
            every { createdAt } returns Instant.fromEpochMilliseconds(0)
        }
        val thread = mockk<ChatThread>(relaxed = true) {
            every { id } returns testThreadId
            every { messages } returns listOf(echo)
            every { scrollToken } returns ""
        }
        val (viewModel, repo) = viewModelWithRepository()
        backgroundScope.launch { viewModel.isAgentTyping.collect {} }

        viewModel.messages.test {
            assertEquals(emptyList<SdkMessage>(), awaitItem())

            repo.chatThreadHandler = fakeHandlerFor(thread)

            assertEquals(listOf(echo), awaitItem())
            assertTrue(adopted.get())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `copies of a send cancelled during upload are deleted`() = runTest(testDispatcher, timeout = TEST_TIMEOUT) {
        coEvery { seeder.seed(any()) } returns SEEDS
        val uploading = CompletableDeferred<Unit>()
        val messages = mockk<ChatThreadMessageHandler> {
            coEvery { send(any<OutboundMessage>()) } coAnswers {
                uploading.complete(Unit)
                awaitCancellation()
            }
        }
        val (viewModel, repo) = viewModelWithRepository(pickedPdfSource())
        repo.chatThreadHandler = fakeHandlerFor(stubThread(), messages)

        viewModel.sendAttachment(PICKED_URI)
        uploading.await()
        viewModel.viewModelScope.cancel()

        coVerify(timeout = VERIFY_TIMEOUT_MS) { seeder.discard(SEEDS) }
    }

    @Test
    fun `an echo shown before its send registers is still adopted`() = runTest(testDispatcher, timeout = TEST_TIMEOUT) {
        val echoId = UUID.randomUUID()
        val echo = mockk<SdkMessage>(relaxed = true) {
            every { id } returns echoId
            every { createdAt } returns Instant.fromEpochMilliseconds(0)
        }
        val thread = mockk<ChatThread>(relaxed = true) {
            every { id } returns testThreadId
            every { messages } returns listOf(echo)
            every { scrollToken } returns ""
        }
        val registered = AtomicBoolean(false)
        val echoSeen = CompletableDeferred<Unit>()
        val adoptedAfterRegister = CompletableDeferred<Unit>()
        every { seeder.register(echoId, any()) } answers { registered.set(true) }
        coEvery { seeder.adoptEchoes(any()) } coAnswers {
            if (echoId in firstArg<Map<UUID, SdkMessage>>()) {
                if (registered.get()) adoptedAfterRegister.complete(Unit) else echoSeen.complete(Unit)
            }
        }
        val messages = mockk<ChatThreadMessageHandler> {
            coEvery { send(any<OutboundMessage>()) } returns echoId.toString()
        }
        val (viewModel, repo) = viewModelWithRepository(pickedPdfSource())
        backgroundScope.launch { viewModel.isAgentTyping.collect {} }
        backgroundScope.launch { viewModel.messages.collect {} }
        repo.chatThreadHandler = fakeHandlerFor(thread, messages)
        echoSeen.await()

        viewModel.sendAttachment(PICKED_URI)

        adoptedAfterRegister.await()
    }

    @Test
    fun `attachments picked before any thread is known reach the composer once prepared`() =
        runTest(testDispatcher, timeout = TEST_TIMEOUT) {
            val (viewModel, _) = viewModelWithRepository(pickedPdfSource())
            val target = viewModel.currentAttachmentTarget()

            viewModel.addPendingAttachments(listOf(PICKED_URI), target).join()

            assertEquals(1, viewModel.pendingAttachments.value.size)
        }

    @Test
    fun `attachments picked for the selected thread reach the composer once prepared`() = runTest(testDispatcher, timeout = TEST_TIMEOUT) {
        val (viewModel, repo) = viewModelWithRepository(pickedPdfSource())
        repo.chatThreadHandler = fakeHandlerFor(stubThread())
        val target = viewModel.currentAttachmentTarget()

        viewModel.addPendingAttachments(listOf(PICKED_URI), target).join()

        assertEquals(1, viewModel.pendingAttachments.value.size)
    }

    @Test
    fun `attachments picked for a thread whose handler was recreated reach the composer once prepared`() =
        runTest(testDispatcher, timeout = TEST_TIMEOUT) {
            val (viewModel, repo) = viewModelWithRepository(pickedPdfSource())
            repo.chatThreadHandler = fakeHandlerFor(stubThread())
            val target = viewModel.currentAttachmentTarget()
            repo.chatThreadHandler = fakeHandlerFor(stubThread()) // e.g. a reconnect selecting the same thread again

            viewModel.addPendingAttachments(listOf(PICKED_URI), target).join()

            assertEquals(1, viewModel.pendingAttachments.value.size)
        }

    @Test
    fun `attachments picked for a thread whose id was re-assigned reach the composer once prepared`() =
        runTest(testDispatcher, timeout = TEST_TIMEOUT) {
            val (viewModel, repo) = viewModelWithRepository(pickedPdfSource())
            var thread = stubThread()
            repo.chatThreadHandler = mockk(relaxed = true) {
                every { get() } answers { thread }
            }
            val target = viewModel.currentAttachmentTarget()
            val reassignedId = UUID.randomUUID()
            thread = mockk(relaxed = true) { every { id } returns reassignedId } // a pending thread re-id'd by the backend

            viewModel.addPendingAttachments(listOf(PICKED_URI), target).join()

            assertEquals(1, viewModel.pendingAttachments.value.size)
        }

    @Test
    fun `attachments picked for a thread are dropped once prepared when the same thread is selected after a sign-out`() =
        runTest(testDispatcher, timeout = TEST_TIMEOUT) {
            val (viewModel, repo) = viewModelWithRepository(pickedPdfSource())
            repo.chatThreadHandler = fakeHandlerFor(stubThread())
            val target = viewModel.currentAttachmentTarget()
            repo.onChatChanged(null) // sign-out, then the same user signs in again
            repo.chatThreadHandler = fakeHandlerFor(stubThread())

            viewModel.addPendingAttachments(listOf(PICKED_URI), target).join()

            assertEquals(emptyList<PreparedAttachment>(), viewModel.pendingAttachments.value)
        }

    @Test
    fun `attachments picked before a sign-out are dropped once prepared`() = runTest(testDispatcher, timeout = TEST_TIMEOUT) {
        val (viewModel, repo) = viewModelWithRepository(pickedPdfSource())
        val target = viewModel.currentAttachmentTarget()
        repo.onChatChanged(null) // sign-out while the pick waited for the chat to be prepared

        viewModel.addPendingAttachments(listOf(PICKED_URI), target).join()

        assertEquals(emptyList<PreparedAttachment>(), viewModel.pendingAttachments.value)
    }

    @Test
    fun `attachments picked for another thread are dropped once prepared`() = runTest(testDispatcher, timeout = TEST_TIMEOUT) {
        val (viewModel, repo) = viewModelWithRepository(pickedPdfSource())
        repo.chatThreadHandler = fakeHandlerFor(stubThread())
        val target = viewModel.currentAttachmentTarget()
        repo.chatThreadHandler = fakeHandlerFor(
            mockk(relaxed = true) {
                every { id } returns UUID.randomUUID()
                every { messages } returns emptyList()
                every { scrollToken } returns ""
            }
        )

        viewModel.addPendingAttachments(listOf(PICKED_URI), target).join()

        assertEquals(emptyList<PreparedAttachment>(), viewModel.pendingAttachments.value)
    }

    @Test
    fun `attachments picked before a sign-out while no chat existed yet are dropped once prepared`() =
        runTest(testDispatcher, timeout = TEST_TIMEOUT) {
            val (viewModel, repo) = viewModelWithRepository(pickedPdfSource())
            val target = viewModel.currentAttachmentTarget()
            // signOut() during Preparing: no chat to null, so only the return to Initial is reported.
            repo.onChatStateChanged(ChatState.Initial)

            viewModel.addPendingAttachments(listOf(PICKED_URI), target).join()

            assertEquals(emptyList<PreparedAttachment>(), viewModel.pendingAttachments.value)
        }

    @Test
    fun `attachments picked in Initial before an unnotified sign-out are dropped once the next session is prepared`() =
        runTest(testDispatcher, timeout = TEST_TIMEOUT) {
            val (viewModel, repo) = viewModelWithRepository(pickedPdfSource())
            repo.onChatStateChanged(ChatState.Initial) // a failed prepare
            val target = viewModel.currentAttachmentTarget()
            repo.onChatStateChanged(ChatState.Preparing) // signOut() in Initial notified nothing
            repo.onChatStateChanged(ChatState.Prepared)

            viewModel.addPendingAttachments(listOf(PICKED_URI), target).join()

            assertEquals(emptyList<PreparedAttachment>(), viewModel.pendingAttachments.value)
        }

    @Test
    fun `attachments picked before the first prepare of a fresh process reach the composer once prepared`() =
        runTest(testDispatcher, timeout = TEST_TIMEOUT) {
            val (viewModel, repo) = viewModelWithRepository(pickedPdfSource())
            val target = viewModel.currentAttachmentTarget() // a pick delivered after process death
            repo.onChatStateChanged(ChatState.Preparing)
            repo.onChatStateChanged(ChatState.Prepared)

            viewModel.addPendingAttachments(listOf(PICKED_URI), target).join()

            assertEquals(1, viewModel.pendingAttachments.value.size)
        }

    @Test
    fun `a sign-out landing while the target is read is never paired with the handler read after it`() =
        runTest(testDispatcher, timeout = TEST_TIMEOUT) {
            val (viewModel, repo) = viewModelWithRepository(pickedPdfSource())
            val armed = AtomicBoolean(false)
            val signedOut = AtomicBoolean(false)
            val thread = stubThread()
            repo.chatThreadHandler = mockk(relaxed = true) {
                every { get() } answers {
                    // signOut() from another thread, between the generation read and the handler read
                    if (armed.get() && signedOut.compareAndSet(false, true)) repo.onChatStateChanged(ChatState.Initial)
                    thread
                }
            }
            // Collectors call get() only at join() below, so the sign-out fires inside the target read.
            armed.set(true)
            val target = viewModel.currentAttachmentTarget()
            repo.chatThreadHandler = fakeHandlerFor(stubThread()) // the same thread selected after signing in again

            viewModel.addPendingAttachments(listOf(PICKED_URI), target).join()

            assertTrue(signedOut.get())
            assertEquals(emptyList<PreparedAttachment>(), viewModel.pendingAttachments.value)
        }

    @Test
    fun `preparation errors of attachments picked before a sign-out are not shown`() =
        runTest(testDispatcher, timeout = TEST_TIMEOUT) {
            val failingSource = mockk<ContentDataSourceList> {
                coEvery { descriptorForUri(any()) } answers { ContentRequestResult.Error(firstArg(), "unreadable") }
            }
            val (viewModel, repo) = viewModelWithRepository(failingSource)
            // Let the initial handler collection reset the dialog first, or it can overwrite the result.
            testScheduler.runCurrent()
            val target = viewModel.currentAttachmentTarget()
            repo.onChatChanged(null)

            viewModel.addPendingAttachments(listOf(PICKED_URI), target).join()

            assertEquals(ConversationDialog.None, viewModel.dialogShown.value)
        }

    @Test
    fun `preparation errors of attachments picked for the shown session are shown`() =
        runTest(testDispatcher, timeout = TEST_TIMEOUT) {
            val failingSource = mockk<ContentDataSourceList> {
                coEvery { descriptorForUri(any()) } answers { ContentRequestResult.Error(firstArg(), "unreadable") }
            }
            val (viewModel, _) = viewModelWithRepository(failingSource)
            // Let the initial handler collection reset the dialog first, or it can overwrite the result.
            testScheduler.runCurrent()
            val target = viewModel.currentAttachmentTarget()

            viewModel.addPendingAttachments(listOf(PICKED_URI), target).join()

            assertTrue(viewModel.dialogShown.value is ConversationDialog.InvalidAttachments)
        }
}

private const val ADOPT_DELAY_MS = 100L
private const val VERIFY_TIMEOUT_MS = 5_000L
private val TEST_TIMEOUT = 20.seconds
private val PICKED_URI: Uri = mockk(relaxed = true)
private val SEEDS = listOf(SentAttachmentSeeder.Seed("picked.pdf", "application/pdf", File("seed-test")))
