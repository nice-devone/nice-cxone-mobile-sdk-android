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

package com.nice.cxonechat.api

import com.nice.cxonechat.ChatEventHandler
import com.nice.cxonechat.event.ChatEvent
import com.nice.cxonechat.exceptions.CXoneException
import com.nice.cxonechat.exceptions.InternalError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatEventHandlerCoroutineWrapperTest {

    @Test
    fun `triggerAsync calls onSuccess on success`() = runTest {
        val handler = mockk<ChatEventHandler>()
        val event = mockk<ChatEvent<Any>>()
        coEvery { handler.trigger(event) } returns Unit
        val successCalled = AtomicBoolean(false)
        val wrapper = ChatEventHandlerCoroutineWrapper(handler, this)

        wrapper.triggerAsync(event, { successCalled.set(true) })
        advanceUntilIdle()

        assertTrue(successCalled.get())
        wrapper.close()
    }

    @Test
    fun `triggerAsync just triggers`() = runTest {
        val handler = mockk<ChatEventHandler>()
        val event = mockk<ChatEvent<Any>>()
        coEvery { handler.trigger(event) } returns Unit
        val wrapper = ChatEventHandlerCoroutineWrapper(handler, this)

        wrapper.triggerAsync(event, null, null)
        advanceUntilIdle()

        coVerify { handler.trigger(event) }
        wrapper.close()
    }

    @Test
    fun `triggerAsync calls onError on failure`() = runTest {
        val handler = mockk<ChatEventHandler>()
        val event = mockk<ChatEvent<Any>>()
        val exception = mockk<CXoneException>(relaxed = true)
        coEvery { handler.trigger(event) } throws exception
        val receivedError = AtomicReference<CXoneException?>(null)
        val wrapper = ChatEventHandlerCoroutineWrapper(handler, this)

        wrapper.triggerAsync(event) { receivedError.set(it) }
        advanceUntilIdle()

        assertSame(exception, receivedError.get())
        wrapper.close()
    }

    @Test
    fun `triggerAsync wraps non-CXoneException in InternalError`() = runTest {
        val handler = mockk<ChatEventHandler>()
        val event = mockk<ChatEvent<Any>>()
        coEvery { handler.trigger(event) } throws RuntimeException("unexpected")
        val receivedError = AtomicReference<CXoneException?>(null)
        val wrapper = ChatEventHandlerCoroutineWrapper(handler, this)

        wrapper.triggerAsync(event, null) { receivedError.set(it) }
        advanceUntilIdle()

        assertIs<InternalError>(receivedError.get())
        wrapper.close()
    }

    @Test
    fun `triggerAsync does not route CancellationException to onError`() = runTest {
        val handler = mockk<ChatEventHandler>()
        val event = mockk<ChatEvent<Any>>()
        coEvery { handler.trigger(event) } throws CancellationException("scope cancelled")
        val errorCalled = AtomicBoolean(false)
        val wrapper = ChatEventHandlerCoroutineWrapper(handler, this)

        wrapper.triggerAsync(event) { errorCalled.set(true) }
        advanceUntilIdle()

        assertFalse(errorCalled.get())
        wrapper.close()
    }

    @Test
    fun `triggerAsync is reusable after exception`() = runTest {
        val handler = mockk<ChatEventHandler>()
        val event = mockk<ChatEvent<Any>>()
        coEvery { handler.trigger(event) } throws RuntimeException("unexpected")
        val wrapper = ChatEventHandlerCoroutineWrapper(handler, this)

        wrapper.triggerAsync(event)
        advanceUntilIdle()

        coEvery { handler.trigger(event) } returns Unit
        val successCalled = AtomicBoolean(false)

        wrapper.triggerAsync(event, { successCalled.set(true) })
        advanceUntilIdle()

        assertTrue(successCalled.get())
        wrapper.close()
    }

    @Test
    fun `triggerAsync after close synthesizes onError synchronously`() = runTest {
        val handler = mockk<ChatEventHandler>()
        val event = mockk<ChatEvent<Any>>()
        coEvery { handler.trigger(event) } returns Unit
        val wrapper = ChatEventHandlerCoroutineWrapper(handler, this)
        val received = AtomicReference<CXoneException?>(null)
        val onSuccessCalled = AtomicBoolean(false)

        wrapper.close()
        wrapper.triggerAsync(
            event,
            onSuccess = { onSuccessCalled.set(true) },
            onError = { received.set(it) },
        )
        advanceUntilIdle()

        // After close, the launched job would be born-cancelled and the lambda would never run.
        // The wrapper synthesizes onError synchronously instead so the consumer notices.
        assertIs<InternalError>(received.get())
        assertFalse(onSuccessCalled.get())
    }

    @Test
    fun `triggerAsync routes onSuccess through the supplied Executor`() {
        val handler = mockk<ChatEventHandler>()
        val event = mockk<ChatEvent<Any>>()
        coEvery { handler.trigger(event) } returns Unit
        val callbackThreadName = java.util.concurrent.atomic.AtomicReference<String?>(null)
        val executor = java.util.concurrent.Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "wrapper-callback-thread")
        }
        val latch = java.util.concurrent.CountDownLatch(1)

        try {
            val wrapper = ChatEventHandlerCoroutineWrapper(handler, executor)
            wrapper.triggerAsync(event, {
                callbackThreadName.set(Thread.currentThread().name)
                latch.countDown()
            })

            assertTrue(latch.await(2, java.util.concurrent.TimeUnit.SECONDS))
            // Kotlin coroutines may append " @coroutine#N" — assert prefix.
            assertTrue(callbackThreadName.get()?.startsWith("wrapper-callback-thread") == true)
            wrapper.close()
        } finally {
            executor.shutdown()
        }
    }
}
