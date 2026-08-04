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

import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.exceptions.CXoneException
import com.nice.cxonechat.exceptions.InternalError
import com.nice.cxonechat.thread.ChatThread
import io.mockk.coEvery
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
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatThreadHandlerJavaInteropTest {

    @Test
    fun `archiveAsync delivers value to onSuccess`() = runTest {
        val handler = mockk<ChatThreadHandler>()
        coEvery { handler.archive() } returns true
        val received = AtomicReference<Boolean?>(null)

        archiveThread(handler, onSuccess = { received.set(it) })

        assertEquals(true, received.get())
    }

    @Test
    fun `archiveAsync routes CXoneException to onError unchanged`() = runTest {
        val handler = mockk<ChatThreadHandler>()
        val exception = mockk<CXoneException>(relaxed = true)
        coEvery { handler.archive() } throws exception
        val received = AtomicReference<CXoneException?>(null)

        archiveThread(handler, onSuccess = { }, onError = { received.set(it) })

        assertSame(exception, received.get())
    }

    @Test
    fun `archiveAsync wraps non-CXoneException in InternalError`() = runTest {
        val handler = mockk<ChatThreadHandler>()
        coEvery { handler.archive() } throws RuntimeException("nope")
        val received = AtomicReference<CXoneException?>(null)

        archiveThread(handler, onSuccess = { }, onError = { received.set(it) })

        assertIs<InternalError>(received.get())
    }

    @Test
    fun `observeThread delivers emissions to listener`() = runTest {
        val flow = MutableSharedFlow<ChatThread>()
        val handler = mockk<ChatThreadHandler>()
        every { handler.threadFlow } returns flow.asSharedFlow()
        val received = mutableListOf<ChatThread>()
        val a = mockk<ChatThread>()
        val b = mockk<ChatThread>()

        val job = launch { collectThreadFlow(handler, listener = { received += it }) }
        runCurrent()
        flow.emit(a)
        flow.emit(b)
        advanceUntilIdle()
        job.cancel()

        assertEquals(listOf(a, b), received)
    }

    @Test
    fun `observeThread Cancellable stops emissions`() = runTest {
        val flow = MutableSharedFlow<ChatThread>()
        val handler = mockk<ChatThreadHandler>()
        every { handler.threadFlow } returns flow.asSharedFlow()
        val received = mutableListOf<ChatThread>()

        val job = launch { collectThreadFlow(handler, listener = { received += it }) }
        runCurrent()
        flow.emit(mockk())
        advanceUntilIdle()
        job.cancel()
        flow.emit(mockk())
        advanceUntilIdle()

        kotlin.test.assertTrue(received.size == 1)
    }

    @Test
    fun `archiveAsync public path delivers via callbackExecutor`() {
        val handler = mockk<ChatThreadHandler>()
        coEvery { handler.archive() } returns true
        val executor = Executors.newSingleThreadExecutor { Thread(it, "archive-cb") }
        val latch = CountDownLatch(1)
        val capturedThread = AtomicReference<String?>(null)
        val received = AtomicReference<Boolean?>(null)

        archiveAsync(
            handler = handler,
            onSuccess = {
                received.set(it)
                capturedThread.set(Thread.currentThread().name)
                latch.countDown()
            },
            callbackExecutor = executor,
        )
        try {
            assertTrue(latch.await(2, TimeUnit.SECONDS))
            assertEquals(true, received.get())
            assertTrue(capturedThread.get()?.startsWith("archive-cb") == true)
        } finally {
            executor.shutdown()
        }
    }

    @Test
    fun `archiveAsync public path routes onError via callbackExecutor`() {
        val handler = mockk<ChatThreadHandler>()
        val exception = mockk<CXoneException>(relaxed = true)
        coEvery { handler.archive() } throws exception
        val executor = Executors.newSingleThreadExecutor { Thread(it, "archive-err") }
        val latch = CountDownLatch(1)
        val received = AtomicReference<CXoneException?>(null)
        val onSuccessCalled = AtomicBoolean(false)

        archiveAsync(
            handler = handler,
            onSuccess = { onSuccessCalled.set(true) },
            onError = {
                received.set(it)
                latch.countDown()
            },
            callbackExecutor = executor,
        )
        try {
            assertTrue(latch.await(2, TimeUnit.SECONDS))
            assertSame(exception, received.get())
            kotlin.test.assertFalse(onSuccessCalled.get())
        } finally {
            executor.shutdown()
        }
    }

    @Test
    fun `observeThread public path delivers via callbackExecutor`() {
        val event = mockk<ChatThread>()
        val flow = MutableSharedFlow<ChatThread>(replay = 1).apply { tryEmit(event) }
        val handler = mockk<ChatThreadHandler>()
        every { handler.threadFlow } returns flow.asSharedFlow()
        val executor = Executors.newSingleThreadExecutor { Thread(it, "thread-cb") }
        val latch = CountDownLatch(1)
        val capturedThread = AtomicReference<String?>(null)

        val cancellable = observeThread(
            handler = handler,
            listener = {
                capturedThread.set(Thread.currentThread().name)
                latch.countDown()
            },
            callbackExecutor = executor,
        )
        try {
            assertTrue(latch.await(2, TimeUnit.SECONDS))
            assertTrue(capturedThread.get()?.startsWith("thread-cb") == true)
        } finally {
            cancellable.cancel()
            executor.shutdown()
        }
    }
}
