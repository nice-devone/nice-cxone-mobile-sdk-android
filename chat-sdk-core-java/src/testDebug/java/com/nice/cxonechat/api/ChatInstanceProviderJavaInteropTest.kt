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

import android.content.Context
import com.nice.cxonechat.ChatInstanceProvider
import com.nice.cxonechat.ChatInstanceProvider.ConfigurationScope
import com.nice.cxonechat.exceptions.CXoneException
import com.nice.cxonechat.exceptions.InternalError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatInstanceProviderJavaInteropTest {

    // ---- closeSuspendingAsync ----

    @Test
    fun `closeSuspendingAsync invokes onSuccess after closeSuspending completes`() = runTest {
        val provider = mockk<ChatInstanceProvider>()
        coEvery { provider.closeSuspending() } returns Unit
        val succeeded = AtomicBoolean(false)

        closeProviderSuspending(provider, onSuccess = { succeeded.set(true) })

        coVerify(exactly = 1) { provider.closeSuspending() }
        assertTrue(succeeded.get())
    }

    @Test
    fun `closeSuspendingAsync routes CXoneException to onError unchanged`() = runTest {
        val provider = mockk<ChatInstanceProvider>()
        val exception = mockk<CXoneException>(relaxed = true)
        coEvery { provider.closeSuspending() } throws exception
        val received = AtomicReference<CXoneException?>(null)

        closeProviderSuspending(provider, onError = { received.set(it) })

        assertSame(exception, received.get())
    }

    @Test
    fun `closeSuspendingAsync wraps non-CXoneException in InternalError`() = runTest {
        val provider = mockk<ChatInstanceProvider>()
        coEvery { provider.closeSuspending() } throws IllegalStateException("nope")
        val received = AtomicReference<CXoneException?>(null)

        closeProviderSuspending(provider, onError = { received.set(it) })

        assertIs<InternalError>(received.get())
    }

    @Test
    fun `closeSuspendingAsync public path delivers via callbackExecutor`() {
        val provider = mockk<ChatInstanceProvider>()
        coEvery { provider.closeSuspending() } returns Unit
        val executor = Executors.newSingleThreadExecutor { Thread(it, "close-cb") }
        val latch = CountDownLatch(1)
        val capturedThread = AtomicReference<String?>(null)

        closeSuspendingAsync(
            provider = provider,
            onSuccess = {
                capturedThread.set(Thread.currentThread().name)
                latch.countDown()
            },
            callbackExecutor = executor,
        )
        try {
            assertTrue(latch.await(2, TimeUnit.SECONDS))
            assertEquals(capturedThread.get()?.startsWith("close-cb"), true)
        } finally {
            executor.shutdown()
        }
    }

    // ---- signOutAsync / signOutBlocking ----

    @Test
    fun `signOutAsync invokes onSuccess after signOut completes`() = runTest {
        val provider = mockk<ChatInstanceProvider>()
        coEvery { provider.signOut() } returns Unit
        val succeeded = AtomicBoolean(false)

        signOutProvider(provider, onSuccess = { succeeded.set(true) })

        coVerify(exactly = 1) { provider.signOut() }
        assertTrue(succeeded.get())
    }

    @Test
    fun `signOutAsync routes CXoneException to onError unchanged`() = runTest {
        val provider = mockk<ChatInstanceProvider>()
        val exception = mockk<CXoneException>(relaxed = true)
        coEvery { provider.signOut() } throws exception
        val received = AtomicReference<CXoneException?>(null)

        signOutProvider(provider, onError = { received.set(it) })

        assertSame(exception, received.get())
    }

    @Test
    fun `signOutAsync wraps non-CXoneException in InternalError`() = runTest {
        val provider = mockk<ChatInstanceProvider>()
        coEvery { provider.signOut() } throws IllegalStateException("nope")
        val received = AtomicReference<CXoneException?>(null)

        signOutProvider(provider, onError = { received.set(it) })

        assertIs<InternalError>(received.get())
    }

    @Test
    fun `signOutAsync public path delivers via callbackExecutor`() {
        val provider = mockk<ChatInstanceProvider>()
        coEvery { provider.signOut() } returns Unit
        val executor = Executors.newSingleThreadExecutor { Thread(it, "signout-cb") }
        val latch = CountDownLatch(1)
        val capturedThread = AtomicReference<String?>(null)

        signOutAsync(
            provider = provider,
            onSuccess = {
                capturedThread.set(Thread.currentThread().name)
                latch.countDown()
            },
            callbackExecutor = executor,
        )
        try {
            assertTrue(latch.await(2, TimeUnit.SECONDS))
            assertEquals(capturedThread.get()?.startsWith("signout-cb"), true)
        } finally {
            executor.shutdown()
        }
    }

    @Test
    fun `signOutBlocking calls suspend signOut via runBlocking`() {
        val provider = mockk<ChatInstanceProvider>()
        coEvery { provider.signOut() } returns Unit

        signOutBlocking(provider)

        coVerify(exactly = 1) { provider.signOut() }
    }

    // ---- configureAsync / configureBlocking ----

    @Test
    fun `configureAsync invokes actions with the ConfigurationScope and onSuccess`() = runTest {
        val provider = mockk<ChatInstanceProvider>()
        val context = mockk<Context>()
        val capturedScope = AtomicReference<ConfigurationScope?>(null)
        coEvery { provider.configure(context, any()) } coAnswers {
            val block = secondArg<ConfigurationScope.() -> Unit>()
            val scope = mockk<ConfigurationScope>(relaxed = true)
            scope.block()
            capturedScope.set(scope)
        }
        val succeeded = AtomicBoolean(false)

        configureProvider(provider, context, actions = { capturedScope.set(it) }, onSuccess = { succeeded.set(true) })
        advanceUntilIdle()

        assertTrue(succeeded.get())
    }

    @Test
    fun `configureAsync routes CXoneException to onError unchanged`() = runTest {
        val provider = mockk<ChatInstanceProvider>()
        val context = mockk<Context>()
        val exception = mockk<CXoneException>(relaxed = true)
        coEvery { provider.configure(context, any()) } throws exception
        val received = AtomicReference<CXoneException?>(null)

        configureProvider(provider, context, actions = { }, onError = { received.set(it) })

        assertSame(exception, received.get())
    }

    @Test
    fun `configureAsync wraps non-CXoneException in InternalError`() = runTest {
        val provider = mockk<ChatInstanceProvider>()
        val context = mockk<Context>()
        coEvery { provider.configure(context, any()) } throws IllegalStateException("nope")
        val received = AtomicReference<CXoneException?>(null)

        configureProvider(provider, context, actions = { }, onError = { received.set(it) })

        assertIs<InternalError>(received.get())
    }

    /**
     * Regression test: consumer-supplied `actions` throwing must propagate unwrapped, exactly
     * like ChatJavaInterop.connectChat() treats consumer failures in onSuccess -- not get
     * mislabeled as an SDK InternalError, since the SDK's own configure() has no try/catch
     * around invoking `actions`.
     */
    @Test
    fun `configureAsync lets a consumer action exception propagate unwrapped, not as InternalError`() = runTest {
        val provider = mockk<ChatInstanceProvider>()
        val context = mockk<Context>()
        coEvery { provider.configure(context, any()) } coAnswers {
            val block = secondArg<ConfigurationScope.() -> Unit>()
            val scope = mockk<ConfigurationScope>(relaxed = true)
            scope.block()
        }
        val consumerBug = IllegalStateException("consumer bug")
        val received = AtomicReference<CXoneException?>(null)

        val thrown = assertFailsWith<IllegalStateException> {
            configureProvider(provider, context, actions = { throw consumerBug }, onError = { received.set(it) })
        }

        assertSame(consumerBug, thrown)
        assertNull(received.get())
    }

    @Test
    fun `configureBlocking calls suspend configure via runBlocking`() {
        val provider = mockk<ChatInstanceProvider>()
        val context = mockk<Context>()
        coEvery { provider.configure(context, any()) } returns Unit

        configureBlocking(provider, context, actions = { })

        coVerify(exactly = 1) { provider.configure(context, any()) }
    }
}
