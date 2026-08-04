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

package com.nice.cxonechat.internal

import com.nice.cxonechat.AbstractLiveChatTest
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.exceptions.InvalidStateException
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.model.makeAgent
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.server.ServerRequest
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState
import io.mockk.verify
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

internal class ChatThreadHandlerLiveChatTest : AbstractLiveChatTest() {
    private lateinit var chatThread: ChatThread
    private lateinit var thread: ChatThreadHandler

    override fun prepare() {
        super.prepare()

        val threadId = UUID.randomUUID()
        chatThread = makeChatThread(messages = listOf(), id = threadId, threadAgent = null)
        thread = chat.threads().thread(chatThread)
    }

    @Test
    fun `position in queue is updated - simple scenario`() {
        val expected = chatThread.asCopyable().copy(
            positionInQueue = 10,
            hasOnlineAgent = true,
            contactId = TestContactId
        )

        var actual: ChatThread? = null
        val job = testScope.launch {
            actual = thread.threadFlow.drop(1).first()
        }
        socketServer.sendServerMessage(ServerResponse.SetPositionInQueue(position = 10, isAgentAvailable = true, threadId = chatThread.id))
        testScope.advanceUntilIdle()
        job.cancel()

        assertEquals(expected, actual?.asCopyable()?.copy())
    }

    @Test
    fun `position in queue update is not skipped if agent was assigned - handover scenario`() {
        val agent = makeAgent()
        val expectedPosition = 1
        val expected = chatThread.asCopyable().copy(
            positionInQueue = null,
            hasOnlineAgent = true,
            contactId = TestContactId,
            threadAgent = agent.toAgent()
        )

        var actual: ChatThread? = null
        val job = testScope.launch {
            actual = thread.threadFlow.drop(1).take(2).last()
        }
        socketServer.sendServerMessage(ServerResponse.CaseInboxAssigneeChanged(chatThread, agent, connection))
        socketServer.sendServerMessage(ServerResponse.SetPositionInQueue(position = expectedPosition, isAgentAvailable = true, threadId = chatThread.id))
        testScope.advanceUntilIdle()
        job.cancel()

        assertEquals(expected, actual?.asCopyable()?.copy())
    }

    @Test
    fun `position in queue is stored as zero when event reports zero`() {
        val expected = chatThread.asCopyable().copy(
            positionInQueue = 0,
            hasOnlineAgent = false,
            contactId = TestContactId
        )

        var actual: ChatThread? = null
        val job = testScope.launch {
            actual = thread.threadFlow.drop(1).first()
        }
        socketServer.sendServerMessage(
            ServerResponse.SetPositionInQueue(
                position = 0,
                isAgentAvailable = false,
                threadId = chatThread.id
            )
        )
        testScope.advanceUntilIdle()
        job.cancel()

        assertEquals(expected, actual?.asCopyable()?.copy())
    }

    @Test
    fun `livechat recovery is available to threadFlow subscribers that attach after the recovery event fires`() {
        val agent = makeAgent()

        // Recovery fires with no active threadFlow subscriber (race condition on slow devices)
        socketServer.sendServerMessage(
            ServerResponse.LivechatRecovered(thread = chatThread, agent = agent)
        )
        testScope.advanceUntilIdle()

        // Late subscriber — simulating UI that initialises after recovery completes
        var actual: ChatThread? = null
        val job = testScope.launch {
            actual = thread.threadFlow.first()
        }
        testScope.advanceUntilIdle()
        job.cancel()

        assertNotNull(actual)
        assertEquals(agent.toAgent(), actual.threadAgent)
    }

    @Test
    fun `livechat recovered emits updated thread via threadFlow`() {
        val agent = makeAgent()
        var actual: ChatThread? = null
        val job = testScope.launch {
            actual = thread.threadFlow.drop(1).first()
        }
        socketServer.sendServerMessage(
            ServerResponse.LivechatRecovered(thread = chatThread, agent = agent)
        )
        testScope.advanceUntilIdle()
        job.cancel()

        assertNotNull(actual)
        assertEquals(agent.toAgent(), actual.threadAgent)
    }

    @Test
    fun `end contact on closed thread does not throw`() {
        val closedThread = chat.threads().thread(
            makeChatThread(threadState = ChatThreadState.Closed)
        )
        closedThread.endContact()
        verify(exactly = 0) { socket.send(text = any()) }
    }

    @Test
    fun `end contact on pending thread throws InvalidStateException`() {
        val pendingThread = chat.threads().thread(
            makeChatThread(threadState = ChatThreadState.Pending)
        )
        assertFailsWith<InvalidStateException> {
            pendingThread.endContact()
        }
    }

    @Test
    fun `refresh sends recovery event for ready thread`() {
        assertSendText(ServerRequest.RecoverLiveChatThread(connection, chatThread)) {
            thread.refresh()
        }
    }
}
