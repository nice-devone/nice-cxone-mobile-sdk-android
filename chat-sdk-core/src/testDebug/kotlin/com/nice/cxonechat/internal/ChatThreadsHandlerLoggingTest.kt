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
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.thread.ChatThread
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test
import java.util.UUID

internal class ChatThreadsHandlerLoggingTest {

    private val logger = mockk<Logger>(relaxed = true)
    private val origin = mockk<ChatThreadsHandler>(relaxed = true)
    private val handler = ChatThreadsHandlerLogging(origin, logger)

    @Test
    fun `thread returns same ChatThreadHandlerLogging for same thread id`() {
        val threadId = UUID.randomUUID()
        val thread = mockk<ChatThread> { every { id } returns threadId }
        every { origin.thread(thread) } returns mockk(relaxed = true)

        val first = handler.thread(thread)
        val second = handler.thread(thread)

        assertSame(first, second)
        verify(exactly = 1) { origin.thread(thread) }
    }

    @Test
    fun `thread creates distinct handler for different thread id`() {
        val thread1 = mockk<ChatThread> { every { id } returns UUID.randomUUID() }
        val thread2 = mockk<ChatThread> { every { id } returns UUID.randomUUID() }
        every { origin.thread(thread1) } returns mockk(relaxed = true)
        every { origin.thread(thread2) } returns mockk(relaxed = true)

        val handler1 = handler.thread(thread1)
        val handler2 = handler.thread(thread2)

        assertNotSame(handler1, handler2)
    }

    @Test
    fun `thread returns same handler for same thread id with different ChatThread instances`() {
        val threadId = UUID.randomUUID()
        val thread1 = mockk<ChatThread> { every { id } returns threadId }
        val thread2 = mockk<ChatThread> { every { id } returns threadId }
        every { origin.thread(any()) } returns mockk(relaxed = true)

        val handlerA = handler.thread(thread1)
        val handlerB = handler.thread(thread2)

        assertSame(handlerA, handlerB)
        verify(exactly = 1) { origin.thread(any()) }
    }

    @Test
    fun `create returns a new handler on each call`() {
        val originHandler1 = mockk<ChatThreadHandler>(relaxed = true)
        every { originHandler1.get() } returns mockk<ChatThread> { every { id } returns UUID.randomUUID() }
        val originHandler2 = mockk<ChatThreadHandler>(relaxed = true)
        every { originHandler2.get() } returns mockk<ChatThread> { every { id } returns UUID.randomUUID() }
        every { origin.create(any(), any()) } returnsMany listOf(originHandler1, originHandler2)

        val first = handler.create(emptyMap(), emptySequence())
        val second = handler.create(emptyMap(), emptySequence())

        assertNotSame(first, second)
    }

    @Test
    fun `thread after create returns same ChatThreadHandlerLogging as create`() {
        val threadId = UUID.randomUUID()
        val originHandler = mockk<ChatThreadHandler>(relaxed = true)
        every { originHandler.get() } returns mockk<ChatThread> { every { id } returns threadId }
        every { origin.create(any(), any()) } returns originHandler

        val thread = mockk<ChatThread> { every { id } returns threadId }

        val createdHandler = handler.create(emptyMap(), emptySequence())
        val lookedUpHandler = handler.thread(thread)

        assertSame(createdHandler, lookedUpHandler)
        verify(exactly = 0) { origin.thread(any()) }
    }

    @Test
    fun `thread memoized handler is evicted when thread disappears from threadsFlow`() =
        runTest(UnconfinedTestDispatcher()) {
            val threadId = UUID.randomUUID()
            val thread = mockk<ChatThread> { every { id } returns threadId }
            every { origin.thread(thread) } returnsMany listOf(mockk(relaxed = true), mockk(relaxed = true))

            val sharedFlow = MutableSharedFlow<List<ChatThread>>()
            every { origin.threadsFlow } returns sharedFlow
            val loggingHandler = ChatThreadsHandlerLogging(origin, logger)

            val first = loggingHandler.thread(thread)

            // UnconfinedTestDispatcher runs launch eagerly: collector subscribes before emit fires.
            val collectJob = launch { loggingHandler.threadsFlow.collect {} }
            sharedFlow.emit(emptyList())
            collectJob.cancel()

            val second = loggingHandler.thread(thread)

            assertNotSame(first, second)
        }

    @Test
    fun `thread handler for remaining thread survives partial eviction`() =
        runTest(UnconfinedTestDispatcher()) {
            val idA = UUID.randomUUID()
            val idB = UUID.randomUUID()
            val threadA = mockk<ChatThread> { every { id } returns idA }
            val threadB = mockk<ChatThread> { every { id } returns idB }
            every { origin.thread(threadA) } returns mockk(relaxed = true)
            every { origin.thread(threadB) } returnsMany listOf(mockk(relaxed = true), mockk(relaxed = true))

            val sharedFlow = MutableSharedFlow<List<ChatThread>>()
            every { origin.threadsFlow } returns sharedFlow
            val loggingHandler = ChatThreadsHandlerLogging(origin, logger)

            val handlerA = loggingHandler.thread(threadA)
            val handlerB = loggingHandler.thread(threadB)

            val collectJob = launch { loggingHandler.threadsFlow.collect {} }
            sharedFlow.emit(listOf(mockk<ChatThread> { every { id } returns idA }))
            collectJob.cancel()

            assertSame(handlerA, loggingHandler.thread(threadA))
            assertNotSame(handlerB, loggingHandler.thread(threadB))
        }
}
