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

package com.nice.cxonechat.api

import com.nice.cxonechat.ChatThreadMessageHandler
import com.nice.cxonechat.exceptions.CXoneException
import com.nice.cxonechat.exceptions.InternalError
import com.nice.cxonechat.message.OutboundMessage
import com.nice.cxonechat.tool.nextString
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame

@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatThreadMessageHandlerCoroutineWrapperTest {

    @Test
    fun `sendAsync calls onSuccess with message id`() = runTest {
        val handler = mockk<ChatThreadMessageHandler>()
        val expectedId = "test-message-id"
        coEvery { handler.send(any<OutboundMessage>()) } returns expectedId
        val receivedId = AtomicReference<String?>(null)
        val wrapper = ChatThreadMessageHandlerCoroutineWrapper(handler, this)

        wrapper.sendAsync(
            message = OutboundMessage(message = nextString()),
            onSuccess = { receivedId.set(it) },
        )
        advanceUntilIdle()

        assertEquals(expectedId, receivedId.get())
        wrapper.close()
    }

    @Test
    fun `sendAsync just sends`() = runTest {
        val handler = mockk<ChatThreadMessageHandler>()
        val expectedId = "test-message-id"
        coEvery { handler.send(any<OutboundMessage>()) } returns expectedId
        val wrapper = ChatThreadMessageHandlerCoroutineWrapper(handler, this)

        wrapper.sendAsync(
            message = OutboundMessage(message = nextString()),
            onSuccess = null,
            onError = null
        )
        advanceUntilIdle()
        wrapper.close()
    }

    @Test
    fun `sendAsync calls onError with CXoneException`() = runTest {
        val handler = mockk<ChatThreadMessageHandler>()
        val exception = mockk<CXoneException>(relaxed = true)
        coEvery { handler.send(any<OutboundMessage>()) } throws exception
        val receivedError = AtomicReference<CXoneException?>(null)
        val wrapper = ChatThreadMessageHandlerCoroutineWrapper(handler, this)

        wrapper.sendAsync(message = OutboundMessage(message = nextString())) { exception ->
            receivedError.set(exception)
        }
        advanceUntilIdle()

        assertSame(exception, receivedError.get())
        wrapper.close()
    }

    @Test
    fun `sendAsync wraps unexpected exception in InternalError`() = runTest {
        val handler = mockk<ChatThreadMessageHandler>()
        coEvery { handler.send(any<OutboundMessage>()) } throws RuntimeException("unexpected")
        val receivedError = AtomicReference<CXoneException?>(null)
        val wrapper = ChatThreadMessageHandlerCoroutineWrapper(handler, this)

        wrapper.sendAsync(message = OutboundMessage(message = nextString()), onSuccess = null) { exception ->
            receivedError.set(exception)
        }
        advanceUntilIdle()

        assertIs<InternalError>(receivedError.get())
        wrapper.close()
    }

    @Test
    fun `sendAsync does not route CancellationException to onError`() = runTest {
        val handler = mockk<ChatThreadMessageHandler>()
        coEvery { handler.send(any<OutboundMessage>()) } throws CancellationException("cancelled")
        val errorCalled = AtomicBoolean(false)
        val wrapper = ChatThreadMessageHandlerCoroutineWrapper(handler, this)

        wrapper.sendAsync(OutboundMessage(message = nextString())) {
            errorCalled.set(true)
        }
        advanceUntilIdle()

        assertFalse(errorCalled.get())
        wrapper.close()
    }

    @Test
    fun `sendAsync is reusable after exception`() = runTest {
        val handler = mockk<ChatThreadMessageHandler>()
        coEvery { handler.send(any<OutboundMessage>()) } throws RuntimeException("unexpected")
        val wrapper = ChatThreadMessageHandlerCoroutineWrapper(handler, this)

        // First call throws an unexpected exception
        wrapper.sendAsync(OutboundMessage(message = nextString()))
        advanceUntilIdle()

        val exception = mockk<CXoneException>(relaxed = true)
        coEvery { handler.send(any<OutboundMessage>()) } throws exception

        wrapper.sendAsync(OutboundMessage(message = nextString()))
        advanceUntilIdle()

        // Reset the mock to return a valid message ID
        val expectedId = "test-message-id"
        coEvery { handler.send(any<OutboundMessage>()) } returns expectedId

        // Second call should succeed
        val receivedId = AtomicReference<String?>(null)
        wrapper.sendAsync(OutboundMessage(message = nextString()), onSuccess = { id ->
            receivedId.set(id)
        })
        advanceUntilIdle()
        assertEquals(expectedId, receivedId.get())
        wrapper.close()
    }

    @Test
    fun `sendAsync with string delegates correctly`() = runTest {
        val handler = mockk<ChatThreadMessageHandler>()
        val expectedId = "test-id"
        coEvery { handler.send(any<OutboundMessage>()) } returns expectedId
        val receivedId = AtomicReference<String?>(null)
        val wrapper = ChatThreadMessageHandlerCoroutineWrapper(handler, this)

        wrapper.sendAsync("hello", "postback", onSuccess = { receivedId.set(it) })
        advanceUntilIdle()

        assertEquals(expectedId, receivedId.get())
        wrapper.close()
    }

    @Test
    fun `sendAsync with attachments delegates correctly`() = runTest {
        val handler = mockk<ChatThreadMessageHandler>()
        val expectedId = "test-id"
        coEvery { handler.send(any<OutboundMessage>()) } returns expectedId
        val receivedId = AtomicReference<String?>(null)
        val wrapper = ChatThreadMessageHandlerCoroutineWrapper(handler, this)

        wrapper.sendAsync(emptyList(), "message", "postback", onSuccess = { receivedId.set(it) })
        advanceUntilIdle()

        assertEquals(expectedId, receivedId.get())
        wrapper.close()
    }
}
