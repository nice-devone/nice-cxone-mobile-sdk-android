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

import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadsHandler
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState.Received
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.UUID
import kotlin.test.assertEquals

/**
 * Unit tests for the memoized-handler eviction behavior of [ChatThreadsHandlerMemoizeHandlers].
 *
 * The eviction path is critical: when a thread disappears from the server's thread list, its
 * memoized [ChatThreadHandler] must be cleared so that the next [ChatThreadsHandlerMemoizeHandlers.thread]
 * call re-enters [ChatThreadsHandlerMessages], which then reinstalls the per-thread cache-watcher
 * coroutine. Without eviction, a handler for a purged+reappeared thread would keep serving a stale
 * instance whose watcher was already cancelled — silently defeating the message-cache machinery.
 *
 * The memoization identity tests live in [com.nice.cxonechat.ChatThreadsHandlerTest].
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatThreadsHandlerMemoizeHandlersEvictionTest {

    private val threadA = makeChatThread(
        id = UUID.fromString("aa000000-0000-0000-0000-000000000001"),
        threadState = Received,
        contactId = null,
    )
    private val threadB = makeChatThread(
        id = UUID.fromString("bb000000-0000-0000-0000-000000000002"),
        threadState = Received,
        contactId = null,
    )

    private fun fakeMemoize(
        originThreadsFlow: MutableSharedFlow<List<ChatThread>>,
        onThreadCalled: () -> Unit = {},
        createdThread: ChatThread? = null,
    ): ChatThreadsHandlerMemoizeHandlers {
        val origin = mockk<ChatThreadsHandler>(relaxed = true) {
            every { threadsFlow } returns originThreadsFlow
            every { thread(any()) } answers {
                onThreadCalled()
                mockk<ChatThreadHandler>(relaxed = true)
            }
            if (createdThread != null) {
                every { create(any(), any()) } returns mockk<ChatThreadHandler>(relaxed = true) {
                    every { get() } returns createdThread
                }
            }
        }
        return ChatThreadsHandlerMemoizeHandlers(origin)
    }

    @Test
    fun `thread calls origin again after the thread is evicted from the list`() = runTest(UnconfinedTestDispatcher()) {
        var callCount = 0
        val originThreadsFlow = MutableSharedFlow<List<ChatThread>>(replay = 1)
        val memoize = fakeMemoize(originThreadsFlow, onThreadCalled = { callCount++ })

        // Activate the eviction onEach by collecting threadsFlow.
        val collectJob = launch { memoize.threadsFlow.collect {} }

        // Both threads are present — populate memoized handlers.
        originThreadsFlow.emit(listOf(threadA, threadB))
        memoize.thread(threadA)
        memoize.thread(threadB)
        assertEquals(2, callCount, "origin.thread() must be called once per thread to populate the cache")

        // threadB disappears from the list — its memoized handler must be evicted.
        originThreadsFlow.emit(listOf(threadA))

        // Calling thread(B) must go back to origin, not the memoized handler.
        memoize.thread(threadB)
        assertEquals(3, callCount, "origin.thread() must be called again for threadB after it was evicted")

        collectJob.cancel()
    }

    @Test
    fun `thread still returns memoized handler for threads that remain in the list after eviction`() =
        runTest(UnconfinedTestDispatcher()) {
            var callCount = 0
            val originThreadsFlow = MutableSharedFlow<List<ChatThread>>(replay = 1)
            val memoize = fakeMemoize(originThreadsFlow, onThreadCalled = { callCount++ })

            val collectJob = launch { memoize.threadsFlow.collect {} }

            originThreadsFlow.emit(listOf(threadA, threadB))
            memoize.thread(threadA)
            memoize.thread(threadB)

            // Evict only threadB.
            originThreadsFlow.emit(listOf(threadA))

            // threadA is still in the list — its handler must remain memoized.
            memoize.thread(threadA)
            assertEquals(2, callCount, "origin.thread() must NOT be called again for threadA that was not evicted")

            collectJob.cancel()
        }

    @Test
    fun `thread calls origin again when evicted thread reappears so a fresh handler is built`() =
        runTest(UnconfinedTestDispatcher()) {
            var callCount = 0
            val originThreadsFlow = MutableSharedFlow<List<ChatThread>>(replay = 1)
            val memoize = fakeMemoize(originThreadsFlow, onThreadCalled = { callCount++ })

            val collectJob = launch { memoize.threadsFlow.collect {} }

            // Round 1: populate handlers.
            originThreadsFlow.emit(listOf(threadA, threadB))
            memoize.thread(threadA)
            memoize.thread(threadB)
            assertEquals(2, callCount)

            // Evict both.
            originThreadsFlow.emit(emptyList())

            // Reappear — fresh handlers must be built, not the old evicted ones.
            originThreadsFlow.emit(listOf(threadA, threadB))
            memoize.thread(threadA)
            memoize.thread(threadB)
            assertEquals(4, callCount, "origin.thread() must be called again for each thread that was evicted and then reappeared")

            collectJob.cancel()
        }

    @Test
    fun `create memoized handler survives eviction while the created thread is listed`() =
        runTest(UnconfinedTestDispatcher()) {
            var callCount = 0
            val originThreadsFlow = MutableSharedFlow<List<ChatThread>>(replay = 1)
            val memoize = fakeMemoize(originThreadsFlow, onThreadCalled = { callCount++ }, createdThread = threadA)

            val collectJob = launch { memoize.threadsFlow.collect {} }

            // Deliberately the two-argument overload: it is the one this class overrides, so it is
            // the one that memoizes. It reaches memoizeThreadHandler, keyed on
            // threadHandler.get().id — the second insertion path into the map, which the tests
            // above never exercise.
            memoize.create(customFields = emptyMap(), preChatSurveyResponse = emptySequence())

            // A freshly created thread is not on the server list yet; ChatThreadsHandlerReplayLastEmpty
            // — applied below MemoizeHandlers in ChatImpl.threadsHandler — prepends it to every
            // emission until the server confirms it. Reordering those two decorators would let
            // retainAll evict this handler and hand out a duplicate for the same thread id — which
            // ChatThreadHandlerWelcomeLiveChat's exclusive addCallback listeners cannot tolerate.
            originThreadsFlow.emit(listOf(threadA))

            memoize.thread(threadA)

            assertEquals(
                0,
                callCount,
                "create()-memoized handler must survive eviction while its thread is still listed",
            )

            collectJob.cancel()
        }
}
