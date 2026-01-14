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
import com.nice.cxonechat.internal.model.ChannelConfiguration
import com.nice.cxonechat.internal.model.ChatThreadMutable.Companion.asMutable
import com.nice.cxonechat.internal.model.network.EventCaseStatusChanged.CaseStatus.Closed
import com.nice.cxonechat.model.makeAgent
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.model.makeMessageModel
import com.nice.cxonechat.server.ServerRequest
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState
import com.nice.cxonechat.thread.ChatThreadState.Loaded
import com.nice.cxonechat.thread.ChatThreadState.Received
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

internal class ChatThreadsHandlerTest : AbstractChatTest() {

    private lateinit var threads: ChatThreadsHandler

    override val config: ChannelConfiguration
        get() = requireNotNull(super.config)

    override fun prepare() {
        super.prepare()
        threads = chat.threads()
    }

    // ---

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
        connect()
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
        val initial = List(2) { i -> makeChatThread(threadState = Received, contactId = null) }
        val expected = initial.toMutableList().also {
            it[0] = it[0].copy(
                canAddMoreMessages = false,
                threadState = ChatThreadState.Closed,
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
    fun threads_notifies_whenNewThreadIsCreated() {
        val initial = List(1) { makeChatThread(threadState = Received, contactId = null) }
        var callCount = 0
        val receivedThreadLists = mutableListOf<List<ChatThread>>()

        val cancellable = threads.threads { threadList ->
            callCount++
            receivedThreadLists.add(threadList)
        }

        // Send initial thread list - should trigger first callback
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(initial))

        assertEquals(1, callCount, "Expected one callback for initial thread list")
        assertEquals(1, receivedThreadLists[0].size, "Expected one thread in initial list")

        // Create a new thread - should trigger second callback with updated list
        threads.create()

        assertEquals(2, callCount, "Expected callback when new thread is created")
        assertEquals(2, receivedThreadLists[1].size, "Expected two threads after creation")

        cancellable.cancel()
    }

    @Test
    fun threads_doesNotNotify_afterCancellableIsCancelled() {
        val initial = List(1) { makeChatThread(threadState = Received, contactId = null) }
        var callCount = 0

        val cancellable = threads.threads { _ ->
            callCount++
        }

        // Send initial thread list - should trigger first callback
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(initial))

        assertEquals(1, callCount, "Expected one callback for initial thread list")

        // Create a thread - should trigger second callback
        threads.create()

        assertEquals(2, callCount, "Expected callback when thread is created")

        // Cancel the listener
        cancellable.cancel()

        // Create another thread - should NOT trigger listener
        threads.create()

        // Verify listener was not called again
        assertEquals(2, callCount, "Listener should not be called after cancellation")
    }

    @Test
    fun threads_handlesMultipleThreadListUpdates() {
        val firstList = List(2) { makeChatThread(threadState = Received, contactId = null) }
        val secondList = List(3) { makeChatThread(threadState = Received, contactId = null) }
        val receivedThreadLists = mutableListOf<List<ChatThread>>()

        val cancellable = threads.threads { threadList ->
            receivedThreadLists.add(threadList)
        }

        // Send first thread list
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(firstList))
        assertEquals(1, receivedThreadLists.size, "Expected one callback")
        assertEquals(2, receivedThreadLists[0].size, "Expected two threads in first list")

        // Create a new thread
        threads.create()
        assertEquals(2, receivedThreadLists.size, "Expected callback for thread creation")
        assertEquals(3, receivedThreadLists[1].size, "Expected three threads after creation")

        // Send second thread list (simulating a refresh)
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(secondList))
        assertEquals(3, receivedThreadLists.size, "Expected callback for second list")
        assertEquals(4, receivedThreadLists[2].size, "Expected four threads in second list")

        // Create another thread
        threads.create()
        assertEquals(4, receivedThreadLists.size, "Expected callback for second thread creation")
        assertEquals(5, receivedThreadLists[3].size, "Expected five threads after second creation")

        cancellable.cancel()
    }

    @Test
    fun threads_maintainsIndependentListsForMultipleListeners() {
        val initial = List(1) { makeChatThread(threadState = Received, contactId = null) }
        val receivedThreadLists1 = mutableListOf<List<ChatThread>>()
        val receivedThreadLists2 = mutableListOf<List<ChatThread>>()

        val cancellable1 = threads.threads { threadList ->
            receivedThreadLists1.add(threadList)
        }

        // Send initial thread list - first listener receives it
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(initial))
        assertEquals(1, receivedThreadLists1.size)
        assertEquals(1, receivedThreadLists1[0].size)

        // Register second listener
        val cancellable2 = threads.threads { threadList ->
            receivedThreadLists2.add(threadList)
        }

        // Second listener starts with empty list, no thread list sent yet to it
        assertEquals(0, receivedThreadLists2.size)

        // Create a thread - both listeners should be notified
        threads.create()
        assertEquals(2, receivedThreadLists1.size, "First listener should receive update")
        assertEquals(2, receivedThreadLists1[1].size, "First listener should have 2 threads")
        assertEquals(1, receivedThreadLists2.size, "Second listener should receive update")
        assertEquals(1, receivedThreadLists2[0].size, "Second listener should have 1 thread")

        cancellable1.cancel()
        cancellable2.cancel()
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

    fun threads(listener: (List<ChatThread>) -> Unit): Cancellable =
        threads.threads(listener = { listener(it) })
}
