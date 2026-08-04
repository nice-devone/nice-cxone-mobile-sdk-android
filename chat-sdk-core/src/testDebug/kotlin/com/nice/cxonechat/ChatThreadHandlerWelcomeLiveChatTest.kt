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

package com.nice.cxonechat

import com.nice.cxonechat.enums.EventType.RecoverLivechat
import com.nice.cxonechat.enums.EventType.SendOutbound
import com.nice.cxonechat.internal.ChatThreadHandlerLiveChat.Companion.BEGIN_CONVERSATION_MESSAGE
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.MessageModel
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.model.makeMessage
import com.nice.cxonechat.model.makeMessageContent
import com.nice.cxonechat.model.makeMessageModel
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState.Pending
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Before
import org.junit.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for [com.nice.cxonechat.internal.ChatThreadHandlerWelcomeLiveChat].
 *
 * Key testing constraint: [com.nice.cxonechat.internal.ChatThreadHandlerWelcomeLiveChat] calls
 * `appendMissingWelcomeMessage()` synchronously in its `init {}` block during handler creation,
 * which can consume the one-shot `eventSendEnabled` flag.
 * Tests must set up mock capture BEFORE calling `chat.threads().thread()` to observe the welcome
 * event, and use fresh random UUIDs to avoid the static `eventSendEnabledMap` contamination.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatThreadHandlerWelcomeLiveChatTest : AbstractLiveChatTest() {

    private lateinit var threadId: UUID

    @Before
    fun setUp() {
        threadId = UUID.randomUUID()
    }

    private fun beginConversationMessage() = makeMessage(
        makeMessageModel(messageContent = makeMessageContent(BEGIN_CONVERSATION_MESSAGE))
    )

    private fun conversationStartModel() = makeMessageModel(
        messageContent = makeMessageContent(BEGIN_CONVERSATION_MESSAGE)
    )

    private fun captureSends(): MutableList<String> {
        clearMocks(socket)
        val sends = mutableListOf<String>()
        every { socket.send(text = capture(sends)) } returns true
        return sends
    }

    private fun ignoreSends() {
        clearMocks(socket)
        every { socket.send(text = any()) } returns true
    }

    private fun sendRecovery(thread: ChatThread, vararg messages: MessageModel) {
        socketServer.sendServerMessage(
            ServerResponse.LivechatRecovered(
                thread = thread.asCopyable().copy(id = threadId),
                messages = messages
            )
        )
    }

    // --- appendMissingWelcomeMessage (fires during handler creation) ---

    @Test
    fun `welcome event is sent when handler is created for thread with conversation start`() {
        // Mock capture must be set up BEFORE chat.threads().thread() because
        // ChatThreadHandlerWelcomeLiveChat.init {} calls appendMissingWelcomeMessage()
        // synchronously during construction when a conversation start message is present.
        val sends = captureSends()

        val thread = makeChatThread(messages = listOf(beginConversationMessage()), id = threadId)
        chat.threads().thread(thread)
        testScope.advanceUntilIdle()

        assertTrue(
            sends.any { it.contains(SendOutbound.value) && it.contains(testWelcomeMessage) },
            "Expected SendOutbound welcome event with message '$testWelcomeMessage' during handler creation"
        )
    }

    @Test
    fun `no welcome event when stored message is blank`() {
        every { storage.welcomeMessage } returns ""
        ignoreSends()

        val thread = makeChatThread(messages = listOf(beginConversationMessage()), id = threadId)
        chat.threads().thread(thread)
        testScope.advanceUntilIdle()

        verify(exactly = 0) { socket.send(text = match { it.contains(SendOutbound.value) }) }
    }

    @Test
    fun `no welcome event when thread has two or more messages`() {
        ignoreSends()

        val thread = makeChatThread(messages = listOf(beginConversationMessage(), makeMessage()), id = threadId)
        chat.threads().thread(thread)
        testScope.advanceUntilIdle()

        verify(exactly = 0) { socket.send(text = match { it.contains(SendOutbound.value) }) }
    }

    // --- welcome event interaction with message sending ---

    @Test
    fun `recovery with conversation start triggers welcome event on empty thread`() {
        // Use empty thread so appendMissingWelcomeMessage doesn't consume eventSendEnabled
        val thread = makeChatThread(messages = emptyList(), id = threadId)
        val sends = captureSends()

        var recoveredThread: ChatThread? = null
        val handler = chat.threads().thread(thread)
        val job = testScope.launch { handler.threadFlow.drop(1).collect { recoveredThread = it } }
        testScope.advanceUntilIdle()

        sendRecovery(thread, conversationStartModel())
        testScope.advanceUntilIdle()

        assertNotNull(recoveredThread, "Listener should have received the recovered thread")
        assertTrue(
            sends.any { it.contains(SendOutbound.value) && it.contains(testWelcomeMessage) },
            "Expected SendOutbound welcome event with message '$testWelcomeMessage' via recovery"
        )
        job.cancel()
    }

    @Test
    fun `send second message does not trigger welcome again`() {
        // Start with conversation start — welcome fires during handler creation
        ignoreSends()

        val thread = makeChatThread(messages = listOf(beginConversationMessage()), id = threadId)
        val handler = chat.threads().thread(thread)
        testScope.advanceUntilIdle()

        // Now send a message — welcome should NOT fire again (eventSendEnabled is already false)
        val secondSends = captureSends()
        val job = testScope.launch { handler.messages().send("Second") }
        testScope.advanceUntilIdle()
        assertTrue(job.isCompleted, SEND_SHOULD_HAVE_COMPLETED)

        assertTrue(
            secondSends.none { it.contains(SendOutbound.value) },
            "Welcome event should not be sent again"
        )
    }

    // --- WelcomeThreadMessageHandlerLiveChat.addWelcomeMessageAndSend ---

    @Test
    fun `send user message triggers welcome event before user message`() {
        // Empty thread: appendMissingWelcomeMessage won't consume eventSendEnabled
        // (no conversation start to match), so it's still true when send() is called.
        val thread = makeChatThread(messages = emptyList(), id = threadId)
        val sends = captureSends()

        val handler = chat.threads().thread(thread)
        testScope.advanceUntilIdle()

        val job = testScope.launch { handler.messages().send("Hello from user") }
        testScope.advanceUntilIdle()
        assertTrue(job.isCompleted, SEND_SHOULD_HAVE_COMPLETED)

        val welcomeIndex = sends.indexOfFirst { it.contains(SendOutbound.value) && it.contains(testWelcomeMessage) }
        val userMessageIndex = sends.indexOfFirst { it.contains("Hello from user") }
        assertTrue(welcomeIndex >= 0, "Welcome event should have been sent")
        assertTrue(userMessageIndex >= 0, "User message should have been sent")
        assertTrue(
            welcomeIndex < userMessageIndex,
            "Welcome event (index=$welcomeIndex) should be sent before user message (index=$userMessageIndex)"
        )
    }

    @Test
    fun `send begin conversation triggers message first then welcome event`() {
        // Empty thread so eventSendEnabled stays true until send() uses it.
        val thread = makeChatThread(messages = emptyList(), id = threadId)
        val sends = captureSends()

        val handler = chat.threads().thread(thread)
        testScope.advanceUntilIdle()

        val job = testScope.launch { handler.messages().send(BEGIN_CONVERSATION_MESSAGE) }
        testScope.advanceUntilIdle()
        assertTrue(job.isCompleted, SEND_SHOULD_HAVE_COMPLETED)

        val beginIndex = sends.indexOfFirst { it.contains(BEGIN_CONVERSATION_MESSAGE) }
        val welcomeIndex = sends.indexOfFirst { it.contains(SendOutbound.value) && it.contains(testWelcomeMessage) }
        assertTrue(beginIndex >= 0, "Begin conversation message should have been sent")
        assertTrue(welcomeIndex >= 0, "Welcome event should have been sent after begin conversation")
        assertTrue(
            beginIndex < welcomeIndex,
            "Begin conversation (index=$beginIndex) should be sent before welcome event (index=$welcomeIndex)"
        )
    }

    @Test
    fun `send delegates to origin when stored message is blank`() {
        every { storage.welcomeMessage } returns ""
        val thread = makeChatThread(messages = emptyList(), id = threadId)
        val sends = captureSends()

        val handler = chat.threads().thread(thread)
        testScope.advanceUntilIdle()

        val job = testScope.launch { handler.messages().send("Hello") }
        testScope.advanceUntilIdle()
        assertTrue(job.isCompleted, SEND_SHOULD_HAVE_COMPLETED)

        assertTrue(
            sends.none { it.contains(SendOutbound.value) && it.contains(testWelcomeMessage) },
            "No welcome event should be sent when stored message is blank"
        )
    }

    // --- prepareThreadRecoverAppend ---

    @Test
    fun `recovery with multiple messages does not trigger welcome`() {
        val thread = makeChatThread(messages = emptyList(), id = threadId)
        ignoreSends()

        var recoveredThread: ChatThread? = null
        val handler = chat.threads().thread(thread)
        val job = testScope.launch { handler.threadFlow.drop(1).collect { recoveredThread = it } }
        testScope.advanceUntilIdle()

        sendRecovery(thread, makeMessageModel(), makeMessageModel())
        testScope.advanceUntilIdle()

        assertNotNull(recoveredThread, "Listener should have received the recovered thread")
        verify(exactly = 0) { socket.send(text = match { it.contains(SendOutbound.value) }) }
        job.cancel()
    }

    // --- DE-166650: no duplicate per-thread decorator stack in LiveChat ---

    @Test
    fun `thread does not build a duplicate handler chain in LiveChat`() {
        // In LiveChat the per-thread message-cache watcher (ChatThreadsHandlerMessages) is omitted, so
        // thread() builds exactly one handler chain. If the watcher were built it would construct a
        // second ChatThreadHandlerLiveChat whose init fires a duplicate RecoverLiveChatThreadEvent — so
        // a single recover event proves the duplicate stack is not created (DE-166650). Recovery is not
        // gated by the one-shot welcome latch, so unlike the welcome send this duplication is observable.
        val pendingThread = makeChatThread(messages = emptyList(), id = threadId, threadState = Pending)
        val sends = captureSends()

        chat.threads().thread(pendingThread)
        testScope.advanceUntilIdle()

        val recoverSends = sends.count { it.contains(RecoverLivechat.value) }
        assertEquals(1, recoverSends, "thread() must build a single LiveChat handler chain (no duplicate cache-watcher stack)")
    }

    private companion object {
        private const val SEND_SHOULD_HAVE_COMPLETED = "send() should have completed"
    }
}
