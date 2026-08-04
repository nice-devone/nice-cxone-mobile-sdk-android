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

import com.nice.cxonechat.ChatThreadActionHandler
import com.nice.cxonechat.Popup
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
internal class ChatThreadActionHandlerJavaInteropTest {

    @Test
    fun `onPopup delivers each emission to listener`() = runTest {
        val flow = MutableSharedFlow<Popup>()
        val handler = mockk<ChatThreadActionHandler>()
        every { handler.popupFlow } returns flow.asSharedFlow()
        val received = mutableListOf<Popup>()
        val a = mockk<Popup>()
        val b = mockk<Popup>()

        val job = launch { collectThreadPopupFlow(handler, listener = { received += it }) }
        runCurrent()
        flow.emit(a)
        flow.emit(b)
        advanceUntilIdle()
        job.cancel()

        assertEquals(listOf(a, b), received)
    }

    @Test
    fun `onPopup Cancellable stops emissions`() = runTest {
        val flow = MutableSharedFlow<Popup>()
        val handler = mockk<ChatThreadActionHandler>()
        every { handler.popupFlow } returns flow.asSharedFlow()
        val received = mutableListOf<Popup>()

        val job = launch { collectThreadPopupFlow(handler, listener = { received += it }) }
        runCurrent()
        flow.emit(mockk())
        advanceUntilIdle()
        job.cancel()
        flow.emit(mockk())
        advanceUntilIdle()

        assertTrue(received.size == 1)
    }

    @Test
    fun `onPopup public path delivers via callbackExecutor`() {
        val event = mockk<Popup>()
        val flow = MutableSharedFlow<Popup>(replay = 1).apply { tryEmit(event) }
        val handler = mockk<ChatThreadActionHandler>()
        every { handler.popupFlow } returns flow.asSharedFlow()
        val executor = Executors.newSingleThreadExecutor { Thread(it, "thread-popup-cb") }
        val latch = CountDownLatch(1)
        val capturedThread = AtomicReference<String?>(null)

        val cancellable = onPopup(
            handler = handler,
            listener = {
                capturedThread.set(Thread.currentThread().name)
                latch.countDown()
            },
            callbackExecutor = executor,
        )
        try {
            assertTrue(latch.await(2, TimeUnit.SECONDS))
            assertTrue(capturedThread.get()?.startsWith("thread-popup-cb") == true)
        } finally {
            cancellable.cancel()
            executor.shutdown()
        }
    }

    @Test
    fun `onPopup default dispatcher delivers to listener`() {
        val event = mockk<Popup>()
        val flow = MutableSharedFlow<Popup>(replay = 1).apply { tryEmit(event) }
        val handler = mockk<ChatThreadActionHandler>()
        every { handler.popupFlow } returns flow.asSharedFlow()
        val latch = CountDownLatch(1)
        val received = AtomicReference<Popup?>(null)

        // No callbackExecutor -> exercises the default (null -> Dispatchers.IO) public path.
        val cancellable = onPopup(
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
