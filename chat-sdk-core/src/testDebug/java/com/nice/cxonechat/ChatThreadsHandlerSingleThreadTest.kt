/*
 * Copyright (c) 2021-2025. NICE Ltd. All rights reserved.
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

import android.annotation.SuppressLint
import com.nice.cxonechat.exceptions.MissingThreadListFetchException
import com.nice.cxonechat.exceptions.UnsupportedChannelConfigException
import com.nice.cxonechat.internal.model.ChannelConfiguration
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.tool.nextStringMap
import org.junit.Test
import kotlin.test.assertEquals

internal class ChatThreadsHandlerSingleThreadTest : AbstractChatTest() {

    private lateinit var threads: ChatThreadsHandler

    override val config: ChannelConfiguration
        get() {
            val config = super.config.let(::requireNotNull)
            return config.copy(settings = config.settings.copy(hasMultipleThreadsPerEndUser = false))
        }

    override fun prepare() {
        super.prepare()
        threads = chat.threads()
    }

    // ---

    @Test(expected = UnsupportedChannelConfigException::class)
    fun create_throws_whenCannotCreateMultipleThreads() {
        with(chat.threads()) {
            threads {}
            serverResponds(ServerResponse.ThreadListFetched(listOf(makeChatThread())))
            create()
        }
    }

    @Test(expected = UnsupportedChannelConfigException::class)
    fun create_withCustomFields_throws_whenCannotCreateMultipleThreads() {
        with(chat.threads()) {
            threads {}
            serverResponds(ServerResponse.ThreadListFetched(listOf(makeChatThread())))
            create(nextStringMap())
        }
    }

    @Test(expected = MissingThreadListFetchException::class)
    fun create_throws_whenThreadsList_isRegisteredButNotLoaded() {
        with(chat.threads()) {
            threads {}
            create()
        }
    }

    @Test(expected = MissingThreadListFetchException::class)
    fun create_withCustomFields_throws_whenThreadsList_isRegisteredButNotLoaded() {
        with(chat.threads()) {
            threads {}
            create(nextStringMap())
        }
    }

    @Test(expected = MissingThreadListFetchException::class)
    fun create_throws_whenThreadsList_isNotRegistered() {
        chat.threads().create()
    }

    @Test(expected = MissingThreadListFetchException::class)
    fun create__withCustomFields_throws_whenThreadsList_isNotRegistered() {
        chat.threads().create(nextStringMap())
    }

    @SuppressLint("CheckResult")
    @Test
    fun create_permitsSingularThread() {
        with(chat.threads()) {
            threads {}
            serverResponds(ServerResponse.ThreadListFetched(listOf()))
            create()
        }
    }

    @SuppressLint("CheckResult")
    @Test
    fun create_withCustomFields_permitsSingularThread() {
        with(chat.threads()) {
            threads {}
            serverResponds(ServerResponse.ThreadListFetched(listOf()))
            create(nextStringMap())
        }
    }

    @Test
    fun threads_notifies_whenNewThreadIsCreated_inSingleThreadMode() {
        var callCount = 0
        val receivedThreadLists = mutableListOf<List<com.nice.cxonechat.thread.ChatThread>>()

        val cancellable = threads.threads { threadList ->
            callCount++
            receivedThreadLists.add(threadList)
        }

        // Send empty thread list - should trigger first callback
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf()))

        assertEquals(1, callCount, "Expected one callback for initial empty thread list")
        assertEquals(0, receivedThreadLists[0].size, "Expected zero threads in initial list")

        // Create a new thread - should trigger second callback with updated list
        threads.create()

        assertEquals(2, callCount, "Expected callback when new thread is created")
        assertEquals(1, receivedThreadLists[1].size, "Expected one thread after creation")

        cancellable.cancel()
    }

    @Test
    fun threads_doesNotNotify_afterCancellableIsCancelled_inSingleThreadMode() {
        var callCount = 0
        var secondListenerCallCount = 0

        val cancellable = threads.threads { _ ->
            callCount++
        }

        // Send empty thread list - should trigger first callback
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf()))

        assertEquals(1, callCount, "Expected one callback for initial empty thread list")

        // Create a thread - should trigger second callback
        threads.create()

        assertEquals(2, callCount, "Expected callback when thread is created")

        // Cancel the listener
        cancellable.cancel()

        // Verify cancellable can be called multiple times without error
        cancellable.cancel()

        // Register a second listener - this should still work
        val secondCancellable = threads.threads { _ ->
            secondListenerCallCount++
        }

        // Send a new thread list - should only trigger the second listener
        socketServer.sendServerMessage(ServerResponse.ThreadListFetched(listOf(makeChatThread())))
        // Verify first listener was not called again after cancellation
        assertEquals(2, callCount, "First listener should not be called after cancellation")

        // Verify second listener was called
        assertEquals(1, secondListenerCallCount, "Second listener should receive callback")

        secondCancellable.cancel()
    }
}
