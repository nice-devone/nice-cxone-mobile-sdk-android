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

package com.nice.cxonechat.api

import com.nice.cxonechat.ChatThreadEventHandler
import com.nice.cxonechat.EventResponse
import com.nice.cxonechat.event.thread.ChatThreadEvent
import com.nice.cxonechat.exceptions.CXoneException
import com.nice.cxonechat.exceptions.InternalError
import com.nice.cxonechat.message.Action
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatThreadEventHandlerCoroutineWrapperTest {

    @Test
    fun `triggerAsync calls onSuccess on success`() = runTest {
        val handler = mockk<ChatThreadEventHandler>()
        val event = mockk<ChatThreadEvent>()
        coEvery { handler.trigger(event) } returns null
        val successCalled = AtomicBoolean(false)
        val wrapper = ChatThreadEventHandlerCoroutineWrapper(handler, this)

        wrapper.triggerAsync(event, { successCalled.set(true) })
        advanceUntilIdle()

        assertTrue(successCalled.get())
        wrapper.close()
    }

    @Test
    fun `triggerAsync just triggers`() = runTest {
        val handler = mockk<ChatThreadEventHandler>()
        val event = mockk<ChatThreadEvent>()
        coEvery { handler.trigger(event) } returns null
        val wrapper = ChatThreadEventHandlerCoroutineWrapper(handler, this)

        wrapper.triggerAsync(event, null, null)
        advanceUntilIdle()

        coVerify { handler.trigger(event) }
        wrapper.close()
    }

    @Test
    fun `triggerAsync calls onError on failure`() = runTest {
        val handler = mockk<ChatThreadEventHandler>()
        val event = mockk<ChatThreadEvent>()
        val exception = mockk<CXoneException>(relaxed = true)
        coEvery { handler.trigger(event) } throws exception
        val receivedError = AtomicReference<CXoneException?>(null)
        val wrapper = ChatThreadEventHandlerCoroutineWrapper(handler, this)

        wrapper.triggerAsync(event) { receivedError.set(it) }
        advanceUntilIdle()

        assertSame(exception, receivedError.get())
        wrapper.close()
    }

    @Test
    fun `triggerAsync wraps non-CXoneException in InternalError`() = runTest {
        val handler = mockk<ChatThreadEventHandler>()
        val event = mockk<ChatThreadEvent>()
        coEvery { handler.trigger(event) } throws RuntimeException("unexpected")
        val receivedError = AtomicReference<CXoneException?>(null)
        val wrapper = ChatThreadEventHandlerCoroutineWrapper(handler, this)

        wrapper.triggerAsync(event, null) { receivedError.set(it) }
        advanceUntilIdle()

        assertIs<InternalError>(receivedError.get())
        wrapper.close()
    }

    @Test
    fun `triggerAsync does not route CancellationException to onError`() = runTest {
        val handler = mockk<ChatThreadEventHandler>()
        val event = mockk<ChatThreadEvent>()
        coEvery { handler.trigger(event) } throws CancellationException("scope cancelled")
        val errorCalled = AtomicBoolean(false)
        val wrapper = ChatThreadEventHandlerCoroutineWrapper(handler, this)

        wrapper.triggerAsync(event) { errorCalled.set(true) }
        advanceUntilIdle()

        assertFalse(errorCalled.get())
        wrapper.close()
    }

    @Test
    fun `triggerAsync is reusable after exception`() = runTest {
        val handler = mockk<ChatThreadEventHandler>()
        val event = mockk<ChatThreadEvent>()
        coEvery { handler.trigger(event) } throws RuntimeException("unexpected")
        val wrapper = ChatThreadEventHandlerCoroutineWrapper(handler, this)

        wrapper.triggerAsync(event)
        advanceUntilIdle()

        coEvery { handler.trigger(event) } returns null
        val successCalled = AtomicBoolean(false)

        wrapper.triggerAsync(event, { successCalled.set(true) })
        advanceUntilIdle()

        assertTrue(successCalled.get())
        wrapper.close()
    }

    @Test
    fun `markThreadReadAsync calls onSuccess on success`() = runTest {
        val handler = mockk<ChatThreadEventHandler>()
        coEvery { handler.trigger(any()) } returns null
        val successCalled = AtomicBoolean(false)
        val wrapper = ChatThreadEventHandlerCoroutineWrapper(handler, this)

        wrapper.markThreadReadAsync(onSuccess = { successCalled.set(true) })
        advanceUntilIdle()

        assertTrue(successCalled.get())
        wrapper.close()
    }

    @Test
    fun `sendTranscriptAsync calls onSuccess with EventResponse`() = runTest {
        val handler = mockk<ChatThreadEventHandler>()
        coEvery { handler.trigger(any()) } returns EventResponse.Success
        val receivedResponse = AtomicReference<EventResponse?>(null)
        val wrapper = ChatThreadEventHandlerCoroutineWrapper(handler, this)

        wrapper.sendTranscriptAsync("test@example.com", onSuccess = { receivedResponse.set(it) })
        advanceUntilIdle()

        assertSame(EventResponse.Success, receivedResponse.get())
        wrapper.close()
    }

    @Test
    fun `typingEndAsync calls onSuccess on success`() = runTest {
        val handler = mockk<ChatThreadEventHandler>()
        coEvery { handler.trigger(any()) } returns null
        val successCalled = AtomicBoolean(false)
        val wrapper = ChatThreadEventHandlerCoroutineWrapper(handler, this)

        wrapper.typingEndAsync(onSuccess = { successCalled.set(true) })
        advanceUntilIdle()

        assertTrue(successCalled.get())
        wrapper.close()
    }

    @Test
    fun `typingStartAsync calls onSuccess on success`() = runTest {
        val handler = mockk<ChatThreadEventHandler>()
        coEvery { handler.trigger(any()) } returns null
        val successCalled = AtomicBoolean(false)
        val wrapper = ChatThreadEventHandlerCoroutineWrapper(handler, this)

        wrapper.typingStartAsync(onSuccess = { successCalled.set(true) })
        advanceUntilIdle()

        assertTrue(successCalled.get())
        wrapper.close()
    }

    @Test
    fun `loadMetadataAsync calls onSuccess on success`() = runTest {
        val handler = mockk<ChatThreadEventHandler>()
        coEvery { handler.trigger(any()) } returns null
        val successCalled = AtomicBoolean(false)
        val wrapper = ChatThreadEventHandlerCoroutineWrapper(handler, this)

        wrapper.loadMetadataAsync(onSuccess = { successCalled.set(true) })
        advanceUntilIdle()

        assertTrue(successCalled.get())
        wrapper.close()
    }

    @Test
    fun `triggerActionAsync calls onError for unsupported action`() = runTest {
        val handler = mockk<ChatThreadEventHandler>()
        coEvery { handler.trigger(any()) } returns null
        val receivedError = AtomicReference<CXoneException?>(null)
        val wrapper = ChatThreadEventHandlerCoroutineWrapper(handler, this)
        val action = mockk<Action>(relaxed = true)

        wrapper.triggerActionAsync(action, onError = { receivedError.set(it) })
        advanceUntilIdle()

        assertIs<CXoneException>(receivedError.get())
        wrapper.close()
    }

    // --- Error callback tests for all *Async methods ---

    @Test
    fun `markThreadReadAsync calls onError on failure`() = runTest {
        assertErrorCallbackInvoked { onError -> markThreadReadAsync(onError = onError) }
    }

    @Test
    fun `typingStartAsync calls onError on failure`() = runTest {
        assertErrorCallbackInvoked { onError -> typingStartAsync(onError = onError) }
    }

    @Test
    fun `typingEndAsync calls onError on failure`() = runTest {
        assertErrorCallbackInvoked { onError -> typingEndAsync(onError = onError) }
    }

    @Test
    fun `loadMetadataAsync calls onError on failure`() = runTest {
        assertErrorCallbackInvoked { onError -> loadMetadataAsync(onError = onError) }
    }

    @Test
    fun `sendTranscriptAsync calls onError on failure`() = runTest {
        assertErrorCallbackInvoked { onError -> sendTranscriptAsync("test@example.com", onError = onError) }
    }

    private fun TestScope.assertErrorCallbackInvoked(
        invoke: ChatThreadEventHandlerCoroutineWrapper.(ErrorCallback) -> Unit,
    ) {
        val handler = mockk<ChatThreadEventHandler>()
        val exception = mockk<CXoneException>(relaxed = true)
        coEvery { handler.trigger(any()) } throws exception
        val receivedError = AtomicReference<CXoneException?>(null)
        val wrapper = ChatThreadEventHandlerCoroutineWrapper(handler, this)

        wrapper.invoke(ErrorCallback { receivedError.set(it) })
        advanceUntilIdle()

        val error = assertNotNull(receivedError.get(), "ErrorCallback should have been invoked")
        assertSame(exception, error)
        wrapper.close()
    }
}
