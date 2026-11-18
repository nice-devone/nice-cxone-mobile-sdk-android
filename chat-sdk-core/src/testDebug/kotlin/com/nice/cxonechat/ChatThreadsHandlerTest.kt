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

@file:Suppress("FunctionMaxLength")

package com.nice.cxonechat

import com.nice.cxonechat.FakeChatStateListener.ChatStateConnection.Ready
import com.nice.cxonechat.internal.ChatLogging
import com.nice.cxonechat.internal.ChatThreadsHandlerMemoizeHandlers
import com.nice.cxonechat.internal.ChatWithParameters
import com.nice.cxonechat.internal.model.ChatThreadMutable.Companion.asMutable
import com.nice.cxonechat.internal.model.network.EventCaseStatusChanged.CaseStatus.Closed
import com.nice.cxonechat.internal.model.network.EventCaseStatusChanged.CaseStatus.Open
import com.nice.cxonechat.model.makeAgent
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.model.makeMessageModel
import com.nice.cxonechat.server.ServerRequest
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState
import com.nice.cxonechat.thread.ChatThreadState.Loaded
import com.nice.cxonechat.thread.ChatThreadState.Received
import com.nice.cxonechat.thread.ContactHandler.CXone
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Handler identity/memoization, refresh/create request emission, and thread-list notification for
 * the multi-thread [ChatThreadsHandler]. `threadsFlow` emission mechanics live in
 * [ChatThreadsHandlerFlowTest]; metadata loading in [ChatThreadsHandlerMetadataTest]; the messages
 * cache in [ChatThreadsHandlerMessagesCacheTest].
 */
internal class ChatThreadsHandlerTest : AbstractChatThreadsHandlerTest() {

    @Test
    fun `threads returns same handler instance across calls`() {
        val first = chat.threads()
        val second = chat.threads()
        assertSame(first, second)
    }

    @Test
    fun `ChatLogging threads returns same handler across calls`() {
        val logging = ChatLogging(chat as ChatWithParameters)
        val first = logging.threads()
        val second = logging.threads()
        assertSame(first, second)
    }

    @Test
    fun refresh_sendsExpectedMessage() {
        assertSendText(ServerRequest.FetchThreadList(connection)) {
            threads.refresh()
        }
    }

    @Test
    fun threads_notifies_withInitialList() {
        val initial = List(2) { makeChatThread(threadState = Received, contactId = null) }
        val message = makeMessageModel(threadIdOnExternalPlatform = initial[0].id)
        val agentModel = makeAgent()
        val expected = listOf(
            initial[0].copy(threadAgent = agentModel.toAgent(), messages = listOfNotNull(message.toMessage()), threadState = Loaded),
            initial[1]
        ).map {
            it.asMutable()
        }
        reconnect()
        // verify that the metadata is loaded when the list is received
        assertSendTexts(
            ServerRequest.FetchThreadList(connection),
            ServerRequest.LoadThreadMetadata(connection, initial[0]),
            ServerRequest.LoadThreadMetadata(connection, initial[1])
        ) {
            // Multithread threads should start in READY state.
            assertEquals(Ready, chatStateListener.connection)
            val actual = testCallback(::threads) {
                sendServerMessage(ServerResponse.ThreadListFetched(initial))
                sendServerMessage(
                    ServerResponse.ThreadMetadataLoaded(agent = agentModel, message = message)
                )
            }
            assertEquals(expected, actual)
        }
    }

    @Test
    fun create_passesNewThread() {
        val handler = chat.threads().create()
        val thread = handler.get()
        assertNotNull(thread)
    }

    @Test
    fun create_withCustomParameters_passesNewThread() {
        val handler = chat.threads().create(emptyMap())
        val thread = handler.get()
        assertNotNull(thread)
    }

    @Test
    fun threads_notifies_caseClosed() {
        val initial = List(2) { makeChatThread(threadState = Received, contactId = null) }
        val expected = initial.toMutableList().also {
            it[0] = it[0].copy(
                canAddMoreMessages = false,
                threadState = ChatThreadState.Closed,
                contactId = TestContactId,
            )
        }
        assertEquals(true, initial[0].canAddMoreMessages)
        val actual = testCallback(::threads) {
            sendServerMessage(ServerResponse.ThreadListFetched(initial))
            sendServerMessage(ServerResponse.CaseStatusChanged(expected[0], Closed))
        }
        assertNotEquals(expected, initial)
        assertEquals(expected, actual)
    }

    @Test
    fun `threads reports a handler-only case status change without archiving the thread`() {
        val initial = List(2) { makeChatThread(threadState = Received, contactId = null) }
        val actual = testCallback<List<ChatThread>>({ trigger ->
            threads { list -> if (list.first().contactHandler == CXone) trigger(list) }
        }) {
            sendServerMessage(ServerResponse.ThreadListFetched(initial))
            sendServerMessage(ServerResponse.CaseStatusChanged(initial[0], Open, handler = "cxone"))
        }
        assertEquals(CXone, actual[0].contactHandler)
        assertEquals(true, actual[0].canAddMoreMessages)
        assertNull(actual[1].contactHandler)
    }

    @Test
    fun thread_createsHandlerForExistingThread() {
        val existingThread = makeChatThread(threadState = Loaded, contactId = null)
        val handler = threads.thread(existingThread)
        assertNotNull(handler)
        assertEquals(existingThread.id, handler.get().id)
    }

    @Test
    fun thread_createsHandlerForThreadWithMessages() {
        val message = makeMessageModel().toMessage()
        val threadWithMessages = makeChatThread(
            threadState = Loaded,
            contactId = null,
            messages = listOfNotNull(message)
        )
        val handler = threads.thread(threadWithMessages)
        assertNotNull(handler)
        assertEquals(threadWithMessages.id, handler.get().id)
        assertEquals(1, handler.get().messages.size)
    }

    @Test
    fun `thread returns the same handler instance for repeated calls with the same id`() {
        val thread = makeChatThread(
            id = UUID.fromString("cc000000-0000-0000-0000-00000000000a"),
            threadState = Received,
            contactId = null,
        )
        reconnect()
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf(thread)))

        // Simulates the list screen and the conversation screen independently calling .thread(id)
        // for the same thread within the same session — they must share one handler, not each get
        // their own independent copy of the thread state.
        val first = threads.thread(thread)
        val second = threads.thread(thread)

        assertSame(first, second, "Repeated .thread(id) calls for the same id must return the same memoized handler")
    }

    @Test
    fun `thread calls origin exactly once when called twice with the same thread id`() {
        // ChatThreadsHandlerLogging re-wraps results on every call, so assertSame cannot work here.
        val thread = makeChatThread(
            id = UUID.fromString("cc000000-0000-0000-0000-00000000000a"),
            threadState = Received,
            contactId = null,
        )
        var callCount = 0
        val fakeOrigin = mockk<ChatThreadsHandler>(relaxed = true)
        every { fakeOrigin.thread(any()) } answers {
            callCount++
            mockk(relaxed = true)
        }
        val memoize = ChatThreadsHandlerMemoizeHandlers(fakeOrigin)

        memoize.thread(thread)
        memoize.thread(thread)

        assertEquals(1, callCount, "origin.thread() must be called exactly once — second call must use the memoized handler")

        memoize.thread(makeChatThread())
        assertEquals(2, callCount, "origin.thread() must be called exactly once per thread")
    }
}
