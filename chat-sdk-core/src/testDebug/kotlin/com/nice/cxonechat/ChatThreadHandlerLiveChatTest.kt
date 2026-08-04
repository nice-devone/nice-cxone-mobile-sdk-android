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

import com.nice.cxonechat.enums.ContactStatus
import com.nice.cxonechat.exceptions.InvalidStateException
import com.nice.cxonechat.internal.ChatThreadHandlerLiveChat.Companion.BEGIN_CONVERSATION_MESSAGE
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.ChatThreadMutable
import com.nice.cxonechat.internal.model.ChatThreadMutable.Companion.asMutable
import com.nice.cxonechat.internal.model.MessageModel
import com.nice.cxonechat.internal.model.network.EventCaseStatusChanged.CaseStatus.Closed
import com.nice.cxonechat.model.makeAgent
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.model.makeCustomField
import com.nice.cxonechat.model.makeMessageModel
import com.nice.cxonechat.server.ServerRequest
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState
import com.nice.cxonechat.thread.CustomField
import com.nice.cxonechat.tool.nextString
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatThreadHandlerLiveChatTest : AbstractLiveChatTest() {

    private lateinit var chatThread: ChatThreadMutable
    private lateinit var thread: ChatThreadHandler

    override fun prepare() {
        super.prepare()
        updateChatThread(makeChatThread())
    }

    @Test
    fun endContactSendsExpectedMessage() {
        val currentThread = chatThread.asCopyable().copy(contactId = TestContactId)
        val thread = chat.threads().thread(currentThread)
        assertSendText(ServerRequest.EndContact(connection, currentThread)) {
            thread.endContact()
        }
    }

    @Test
    fun get_observes_case_closed() {
        val expected = chatThread.asCopyable().copy(
            canAddMoreMessages = false,
            threadState = ChatThreadState.Closed,
        )
        assertNotEquals<ChatThread>(chatThread, expected)
        val actual = testCallback(::get) {
            sendServerMessage(ServerResponse.CaseStatusChanged(chatThread.snapshot(), Closed))
        }
        assertEquals(expected, actual)
    }

    @Test
    fun livechat_recovered_updates_thread() {
        val messages = listOf(
            makeMessageModel(),
            makeMessageModel()
        )
        val scrollToken = nextString()
        val agent = makeAgent()
        val caseCustomFields = listOf(
            makeCustomField(),
            makeCustomField()
        ).sortedBy(CustomField::id)
        val expected = chatThread.asCopyable().copy(
            contactId = TestContactId,
            scrollToken = scrollToken,
            threadAgent = agent.toAgent(),
            messages = messages.mapNotNull(MessageModel::toMessage),
            threadState = ChatThreadState.Ready,
            threadName = nextString(),
            fields = caseCustomFields
        )
        assertNotEquals(chatThread, expected.asMutable())
        val actual = testCallback(::get) {
            sendServerMessage(
                ServerResponse.LivechatRecovered(
                    thread = expected,
                    messages = messages.toTypedArray(),
                    scrollToken = scrollToken,
                    agent = agent
                )
            )
        }
        assertEquals(expected, actual)
    }

    @Test
    fun endContact_when_closed_does_nothing() {
        chatThread.update(chatThread.asCopyable().copy(threadState = ChatThreadState.Closed))
        assertSendsNothing {
            thread.endContact()
        }
    }

    @Test(expected = InvalidStateException::class)
    fun endContact_throws_when_not_ready() {
        chatThread.update(chatThread.asCopyable().copy(threadState = ChatThreadState.Loaded))
        assertSendsNothing {
            thread.endContact()
        }
    }

    @Test
    fun livechat_recovered_with_previous_inbox_assignee_only_sets_thread_agent() {
        val previousAgent = makeAgent()
        val expected = chatThread.asCopyable().copy(
            contactId = TestContactId,
            threadAgent = previousAgent.toAgent(),
            threadState = ChatThreadState.Ready,
        )
        val actual = testCallback(::get) {
            sendServerMessage(
                ServerResponse.LivechatRecovered(
                    thread = expected,
                    agent = null,
                    previousInboxAssignee = previousAgent,
                    scrollToken = "",
                )
            )
        }
        assertEquals(expected, actual)
    }

    @Test
    fun livechat_recovered_with_owner_assignee_only_sets_thread_agent() {
        val ownerAgent = makeAgent()
        val expected = chatThread.asCopyable().copy(
            contactId = TestContactId,
            threadAgent = ownerAgent.toAgent(),
            threadState = ChatThreadState.Ready,
        )
        val actual = testCallback(::get) {
            sendServerMessage(
                ServerResponse.LivechatRecovered(
                    thread = expected,
                    agent = null,
                    ownerAssignee = ownerAgent,
                    scrollToken = "",
                )
            )
        }
        assertEquals(expected, actual)
    }

    @Test
    fun livechat_ignored_closed_thread() {
        val messages = listOf(
            makeMessageModel(),
            makeMessageModel()
        )
        val expected = chatThread.snapshot()
        val actual = testCallback(::get) {
            sendServerMessage(
                ServerResponse.LivechatRecovered(
                    thread = expected,
                    messages = messages.toTypedArray(),
                    scrollToken = nextString(),
                    agent = null,
                    status = ContactStatus.Closed,
                )
            )
        }
        assertEquals(expected, actual)
    }

    // --- Begin Conversation init path ---

    @Test
    fun create_sends_begin_conversation_on_new_pending_thread() {
        clearMocks(socket)
        every { socket.send(text = any()) } returns true

        chat.threads().create()
        testScope.advanceUntilIdle()

        verify(exactly = 1) {
            socket.send(text = match { it.contains(BEGIN_CONVERSATION_MESSAGE) })
        }
    }

    @Test
    fun thread_does_not_send_begin_conversation() {
        // thread() passes isThreadCreated = false — no Begin Conversation should be sent
        clearMocks(socket)
        every { socket.send(text = any()) } returns true

        chat.threads().thread(chatThread)
        testScope.advanceUntilIdle()

        verify(exactly = 0) {
            socket.send(text = match { it.contains(BEGIN_CONVERSATION_MESSAGE) })
        }
    }

    @Test
    fun thread_does_not_send_begin_conversation_when_thread_is_pending() {
        // Even a Pending thread with no messages must not trigger BEGIN_CONVERSATION when
        // accessed via thread() — isThreadCreated = false - we are trying to recover backend state.
        val pendingThread = chatThread.asCopyable().copy(
            threadState = ChatThreadState.Pending,
            messages = emptyList(),
        )
        clearMocks(socket)
        every { socket.send(text = any()) } returns true

        chat.threads().thread(pendingThread)
        testScope.advanceUntilIdle()

        verify(exactly = 0) {
            socket.send(text = match { it.contains(BEGIN_CONVERSATION_MESSAGE) })
        }
    }

    // ---

    private fun get(listener: (ChatThread) -> Unit): Cancellable {
        val job = testScope.launch { thread.threadFlow.drop(1).collect { listener(it) } }
        return Cancellable { job.cancel() }
    }

    private fun updateChatThread(updatedThread: ChatThread) {
        val threadMutable = updatedThread.asMutable() // Handlers are memoized, therefore mutating the thread is required
        if (::chatThread.isInitialized) {
            chatThread.update(threadMutable)
        } else {
            chatThread = threadMutable
        }
        thread = chat.threads().thread(threadMutable)
    }
}
