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

package com.nice.cxonechat.api

import com.nice.cxonechat.ChatThreadsHandler
import com.nice.cxonechat.thread.ChatThread
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatThreadsHandlerJavaInteropTest {

    @Test
    fun `observeThreads delivers each emission to listener`() = runTest {
        val flow = MutableSharedFlow<List<ChatThread>>()
        val handler = mockk<ChatThreadsHandler>()
        every { handler.threadsFlow } returns flow.asSharedFlow()
        val received = mutableListOf<List<ChatThread>>()
        val first = listOf(mockk<ChatThread>())
        val second = listOf(mockk<ChatThread>(), mockk<ChatThread>())

        val job = launch { collectThreadsFlow(handler, listener = { received += it }) }
        runCurrent()
        flow.emit(first)
        flow.emit(second)
        advanceUntilIdle()
        job.cancel()

        assertEquals(listOf(first, second), received)
    }

    @Test
    fun `observeThreads Cancellable stops emissions`() = runTest {
        val flow = MutableSharedFlow<List<ChatThread>>()
        val handler = mockk<ChatThreadsHandler>()
        every { handler.threadsFlow } returns flow.asSharedFlow()
        val received = mutableListOf<List<ChatThread>>()

        val job = launch { collectThreadsFlow(handler, listener = { received += it }) }
        runCurrent()
        flow.emit(listOf(mockk()))
        advanceUntilIdle()
        job.cancel()
        flow.emit(listOf(mockk()))
        advanceUntilIdle()

        assertTrue(received.size == 1)
    }

    @Test
    fun `observeThreads public path delivers via callbackExecutor`() {
        val initial = listOf(mockk<ChatThread>())
        val flow = MutableSharedFlow<List<ChatThread>>(replay = 1).apply { tryEmit(initial) }
        val handler = mockk<ChatThreadsHandler>()
        every { handler.threadsFlow } returns flow.asSharedFlow()
        val executor = Executors.newSingleThreadExecutor { Thread(it, "threads-cb") }
        val latch = CountDownLatch(1)
        val capturedThread = AtomicReference<String?>(null)

        val cancellable = observeThreads(
            handler = handler,
            listener = {
                capturedThread.set(Thread.currentThread().name)
                latch.countDown()
            },
            callbackExecutor = executor,
        )
        try {
            assertTrue(latch.await(2, TimeUnit.SECONDS))
            assertTrue(capturedThread.get()?.startsWith("threads-cb") == true)
        } finally {
            cancellable.cancel()
            executor.shutdown()
        }
    }

    @Test
    fun `observeThreads default dispatcher delivers to listener`() {
        val event = listOf(mockk<ChatThread>())
        val flow = MutableSharedFlow<List<ChatThread>>(replay = 1).apply { tryEmit(event) }
        val handler = mockk<ChatThreadsHandler>()
        every { handler.threadsFlow } returns flow.asSharedFlow()
        val latch = CountDownLatch(1)
        val received = AtomicReference<List<ChatThread>?>(null)

        // No callbackExecutor -> exercises the default (null -> Dispatchers.IO) public path.
        val cancellable = observeThreads(
            handler = handler,
            listener = {
                received.set(it)
                latch.countDown()
            },
        )
        try {
            assertTrue(latch.await(2, TimeUnit.SECONDS))
            assertEquals(event, received.get())
        } finally {
            cancellable.cancel()
        }
    }
}
