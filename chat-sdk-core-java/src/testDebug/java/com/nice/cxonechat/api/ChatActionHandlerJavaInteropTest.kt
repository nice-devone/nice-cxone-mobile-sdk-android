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

import com.nice.cxonechat.ChatActionHandler
import com.nice.cxonechat.PopupEvent
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
internal class ChatActionHandlerJavaInteropTest {

    @Test
    fun `onPopup delivers each emission to listener`() = runTest {
        val flow = MutableSharedFlow<PopupEvent>()
        val handler = mockk<ChatActionHandler>()
        every { handler.popupFlow } returns flow.asSharedFlow()
        val received = mutableListOf<PopupEvent>()
        val a = mockk<PopupEvent>()
        val b = mockk<PopupEvent>()

        val job = launch { collectPopupFlow(handler, listener = { received += it }) }
        runCurrent()
        flow.emit(a)
        flow.emit(b)
        advanceUntilIdle()
        job.cancel()

        assertEquals(listOf(a, b), received)
    }

    @Test
    fun `onPopup Cancellable stops emissions`() = runTest {
        val flow = MutableSharedFlow<PopupEvent>()
        val handler = mockk<ChatActionHandler>()
        every { handler.popupFlow } returns flow.asSharedFlow()
        val received = mutableListOf<PopupEvent>()

        val job = launch { collectPopupFlow(handler, listener = { received += it }) }
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
        val event = mockk<PopupEvent>()
        val flow = MutableSharedFlow<PopupEvent>(replay = 1).apply { tryEmit(event) }
        val handler = mockk<ChatActionHandler>()
        every { handler.popupFlow } returns flow.asSharedFlow()
        val executor = Executors.newSingleThreadExecutor { Thread(it, "popup-cb") }
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
            assertTrue(capturedThread.get()?.startsWith("popup-cb") == true)
        } finally {
            cancellable.cancel()
            executor.shutdown()
        }
    }
}
