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

import com.nice.cxonechat.ChatEventHandler
import com.nice.cxonechat.analytics.ActionMetadata
import com.nice.cxonechat.exceptions.CXoneException
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress(
    "TooManyFunctions" // Keep the tests together
)
internal class ChatEventHandlerCoroutineWrapperMethodsTest {

    // --- Success tests for individual *Async methods ---

    @Test
    fun `chatWindowOpenAsync calls onSuccess on success`() = runTest {
        assertSuccessCallbackInvoked { onSuccess -> chatWindowOpenAsync(onSuccess = onSuccess) }
    }

    @Test
    fun `conversionAsync calls onSuccess on success`() = runTest {
        assertSuccessCallbackInvoked { onSuccess -> conversionAsync("sale", 99.99, onSuccess = onSuccess) }
    }

    @Test
    fun `customVisitorAsync calls onSuccess on success`() = runTest {
        assertSuccessCallbackInvoked { onSuccess -> customVisitorAsync("data", onSuccess = onSuccess) }
    }

    @Test
    fun `pageViewAsync calls onSuccess on success`() = runTest {
        assertSuccessCallbackInvoked { onSuccess -> pageViewAsync("Start", "/start", onSuccess = onSuccess) }
    }

    @Test
    fun `pageViewEndedAsync calls onSuccess on success`() = runTest {
        assertSuccessCallbackInvoked { onSuccess -> pageViewEndedAsync("End", "/end", onSuccess = onSuccess) }
    }

    @Test
    fun `proactiveActionClickAsync calls onSuccess on success`() = runTest {
        val metadata = mockk<ActionMetadata>(relaxed = true)
        assertSuccessCallbackInvoked { onSuccess -> proactiveActionClickAsync(metadata, onSuccess = onSuccess) }
    }

    @Test
    fun `proactiveActionDisplayAsync calls onSuccess on success`() = runTest {
        val metadata = mockk<ActionMetadata>(relaxed = true)
        assertSuccessCallbackInvoked { onSuccess -> proactiveActionDisplayAsync(metadata, onSuccess = onSuccess) }
    }

    @Test
    fun `proactiveActionFailureAsync calls onSuccess on success`() = runTest {
        val metadata = mockk<ActionMetadata>(relaxed = true)
        assertSuccessCallbackInvoked { onSuccess -> proactiveActionFailureAsync(metadata, onSuccess = onSuccess) }
    }

    @Test
    fun `proactiveActionSuccessAsync calls onSuccess on success`() = runTest {
        val metadata = mockk<ActionMetadata>(relaxed = true)
        assertSuccessCallbackInvoked { onSuccess -> proactiveActionSuccessAsync(metadata, onSuccess = onSuccess) }
    }

    @Test
    fun `eventAsync calls onSuccess on success`() = runTest {
        assertSuccessCallbackInvoked { onSuccess -> eventAsync(UUID.randomUUID(), onSuccess = onSuccess) }
    }

    // --- Error callback tests for individual *Async methods ---

    @Test
    fun `chatWindowOpenAsync calls onError on failure`() = runTest {
        assertErrorCallbackInvoked { onError -> chatWindowOpenAsync(onError = onError) }
    }

    @Test
    fun `conversionAsync calls onError on failure`() = runTest {
        assertErrorCallbackInvoked { onError -> conversionAsync("sale", 99.99, onError = onError) }
    }

    @Test
    fun `customVisitorAsync calls onError on failure`() = runTest {
        assertErrorCallbackInvoked { onError -> customVisitorAsync("data", onError = onError) }
    }

    @Test
    fun `pageViewAsync calls onError on failure`() = runTest {
        assertErrorCallbackInvoked { onError -> pageViewAsync("Start-Error", "/start-error", onError = onError) }
    }

    @Test
    fun `pageViewEndedAsync calls onError on failure`() = runTest {
        assertErrorCallbackInvoked { onError -> pageViewEndedAsync("End-Error", "/end-error", onError = onError) }
    }

    @Test
    fun `proactiveActionClickAsync calls onError on failure`() = runTest {
        val metadata = mockk<ActionMetadata>(relaxed = true)
        assertErrorCallbackInvoked { onError -> proactiveActionClickAsync(metadata, onError = onError) }
    }

    @Test
    fun `proactiveActionDisplayAsync calls onError on failure`() = runTest {
        val metadata = mockk<ActionMetadata>(relaxed = true)
        assertErrorCallbackInvoked { onError -> proactiveActionDisplayAsync(metadata, onError = onError) }
    }

    @Test
    fun `proactiveActionFailureAsync calls onError on failure`() = runTest {
        val metadata = mockk<ActionMetadata>(relaxed = true)
        assertErrorCallbackInvoked { onError -> proactiveActionFailureAsync(metadata, onError = onError) }
    }

    @Test
    fun `proactiveActionSuccessAsync calls onError on failure`() = runTest {
        val metadata = mockk<ActionMetadata>(relaxed = true)
        assertErrorCallbackInvoked { onError -> proactiveActionSuccessAsync(metadata, onError = onError) }
    }

    @Test
    fun `eventAsync calls onError on failure`() = runTest {
        assertErrorCallbackInvoked { onError -> eventAsync(UUID.randomUUID(), onError = onError) }
    }

    // --- Helpers ---

    private fun TestScope.assertSuccessCallbackInvoked(
        invoke: ChatEventHandlerCoroutineWrapper.(Runnable) -> Unit,
    ) {
        val handler = mockk<ChatEventHandler>()
        coEvery { handler.trigger(any()) } returns Unit
        val successCalled = AtomicBoolean(false)
        val wrapper = ChatEventHandlerCoroutineWrapper(handler, this)

        wrapper.invoke(Runnable { successCalled.set(true) })
        advanceUntilIdle()

        assertTrue(successCalled.get(), "onSuccess should have been invoked")
        wrapper.close()
    }

    private fun TestScope.assertErrorCallbackInvoked(
        invoke: ChatEventHandlerCoroutineWrapper.(ErrorCallback) -> Unit,
    ) {
        val handler = mockk<ChatEventHandler>()
        val exception = mockk<CXoneException>(relaxed = true)
        coEvery { handler.trigger(any()) } throws exception
        val receivedError = AtomicReference<CXoneException?>(null)
        val wrapper = ChatEventHandlerCoroutineWrapper(handler, this)

        wrapper.invoke(ErrorCallback { receivedError.set(it) })
        advanceUntilIdle()

        val error = assertNotNull(receivedError.get(), "ErrorCallback should have been invoked")
        assertSame(exception, error)
        wrapper.close()
    }
}
