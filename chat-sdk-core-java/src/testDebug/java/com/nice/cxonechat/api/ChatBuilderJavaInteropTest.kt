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
import com.nice.cxonechat.Chat
import com.nice.cxonechat.ChatBuilder
import com.nice.cxonechat.SocketFactoryConfiguration
import com.nice.cxonechat.exceptions.CXoneException
import com.nice.cxonechat.exceptions.InternalError
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatBuilderJavaInteropTest {

    @After
    fun tearDown() {
        unmockkObject(ChatBuilder.Companion)
    }

    // ---- buildAsync ----

    @Test
    fun `buildAsync delivers built Chat to onSuccess`() = runTest {
        val builder = mockk<ChatBuilder>()
        val chat = mockk<Chat>()
        coEvery { builder.build() } returns chat
        val received = AtomicReference<Chat?>(null)

        buildChat(builder = builder, onSuccess = { received.set(it) })

        assertSame(chat, received.get())
    }

    @Test
    fun `buildAsync routes CXoneException to onError unchanged`() = runTest {
        val builder = mockk<ChatBuilder>()
        val exception = mockk<CXoneException>(relaxed = true)
        coEvery { builder.build() } throws exception
        val received = AtomicReference<CXoneException?>(null)

        buildChat(
            builder = builder,
            onSuccess = { },
            onError = { received.set(it) },
        )

        assertSame(exception, received.get())
    }

    @Test
    fun `buildAsync wraps non-CXoneException in InternalError`() = runTest {
        val builder = mockk<ChatBuilder>()
        coEvery { builder.build() } throws IllegalStateException("nope")
        val received = AtomicReference<CXoneException?>(null)

        buildChat(
            builder = builder,
            onSuccess = { },
            onError = { received.set(it) },
        )

        assertIs<InternalError>(received.get())
    }

    // ---- getDefaultAsync ----

    @Test
    fun `getDefaultAsync delivers builder to onSuccess`() = runTest {
        val context = mockk<Context>()
        val config = mockk<SocketFactoryConfiguration>()
        val builder = mockk<ChatBuilder>()
        mockkObject(ChatBuilder.Companion)
        coEvery { ChatBuilder.getDefault(context, config, any()) } returns builder
        val received = AtomicReference<ChatBuilder?>(null)

        getDefaultChatBuilder(
            context = context,
            config = config,
            onSuccess = { received.set(it) },
        )
        advanceUntilIdle()

        assertSame(builder, received.get())
    }

    @Test
    fun `getDefaultAsync routes CXoneException to onError unchanged`() = runTest {
        val context = mockk<Context>()
        val config = mockk<SocketFactoryConfiguration>()
        val exception = mockk<CXoneException>(relaxed = true)
        mockkObject(ChatBuilder.Companion)
        coEvery { ChatBuilder.getDefault(context, config, any()) } throws exception
        val received = AtomicReference<CXoneException?>(null)

        getDefaultChatBuilder(
            context = context,
            config = config,
            onSuccess = { },
            onError = { received.set(it) },
        )
        advanceUntilIdle()

        assertSame(exception, received.get())
    }

    @Test
    fun `getDefaultAsync wraps non-CXoneException in InternalError`() = runTest {
        val context = mockk<Context>()
        val config = mockk<SocketFactoryConfiguration>()
        mockkObject(ChatBuilder.Companion)
        coEvery { ChatBuilder.getDefault(context, config, any()) } throws IllegalStateException("nope")
        val received = AtomicReference<CXoneException?>(null)

        getDefaultChatBuilder(
            context = context,
            config = config,
            onSuccess = { },
            onError = { received.set(it) },
        )
        advanceUntilIdle()

        assertIs<InternalError>(received.get())
    }

    // ---- Public API surface (Executor / internal-scope creation) ----

    @Test
    fun `buildAsync public path delivers via callbackExecutor`() {
        val builder = mockk<ChatBuilder>()
        val chat = mockk<Chat>()
        coEvery { builder.build() } returns chat
        val executor = Executors.newSingleThreadExecutor { Thread(it, "build-cb") }
        val latch = CountDownLatch(1)
        val capturedThread = AtomicReference<String?>(null)
        val received = AtomicReference<Chat?>(null)

        buildAsync(
            builder = builder,
            onSuccess = {
                received.set(it)
                capturedThread.set(Thread.currentThread().name)
                latch.countDown()
            },
            callbackExecutor = executor,
        )
        try {
            assertTrue(latch.await(2, TimeUnit.SECONDS))
            assertSame(chat, received.get())
            assertEquals(capturedThread.get()?.startsWith("build-cb"), true)
        } finally {
            executor.shutdown()
        }
    }

    @Test
    fun `buildAsync public path routes onError via callbackExecutor`() {
        val builder = mockk<ChatBuilder>()
        val exception = mockk<CXoneException>(relaxed = true)
        coEvery { builder.build() } throws exception
        val executor = Executors.newSingleThreadExecutor { Thread(it, "build-err") }
        val latch = CountDownLatch(1)
        val received = AtomicReference<CXoneException?>(null)

        buildAsync(
            builder = builder,
            onSuccess = { },
            onError = {
                received.set(it)
                latch.countDown()
            },
            callbackExecutor = executor,
        )
        try {
            assertTrue(latch.await(2, TimeUnit.SECONDS))
            assertSame(exception, received.get())
        } finally {
            executor.shutdown()
        }
    }

    @Test
    fun `getDefaultAsync public path delivers via callbackExecutor`() {
        val context = mockk<Context>()
        val config = mockk<SocketFactoryConfiguration>()
        val builder = mockk<ChatBuilder>()
        mockkObject(ChatBuilder.Companion)
        coEvery { ChatBuilder.getDefault(context, config, any()) } returns builder
        val executor = Executors.newSingleThreadExecutor { Thread(it, "getdefault-cb") }
        val latch = CountDownLatch(1)
        val capturedThread = AtomicReference<String?>(null)
        val received = AtomicReference<ChatBuilder?>(null)

        getDefaultAsync(
            context = context,
            config = config,
            onSuccess = {
                received.set(it)
                capturedThread.set(Thread.currentThread().name)
                latch.countDown()
            },
            callbackExecutor = executor,
        )
        try {
            assertTrue(latch.await(2, TimeUnit.SECONDS))
            assertSame(builder, received.get())
            assertEquals(capturedThread.get()?.startsWith("getdefault-cb"), true)
        } finally {
            executor.shutdown()
        }
    }

    @Test
    fun `getDefaultAsync public path routes onError via callbackExecutor`() {
        val context = mockk<Context>()
        val config = mockk<SocketFactoryConfiguration>()
        val exception = mockk<CXoneException>(relaxed = true)
        mockkObject(ChatBuilder.Companion)
        coEvery { ChatBuilder.getDefault(context, config, any()) } throws exception
        val executor = Executors.newSingleThreadExecutor { Thread(it, "getdefault-err") }
        val latch = CountDownLatch(1)
        val received = AtomicReference<CXoneException?>(null)

        getDefaultAsync(
            context = context,
            config = config,
            onSuccess = { },
            onError = {
                received.set(it)
                latch.countDown()
            },
            callbackExecutor = executor,
        )
        try {
            assertTrue(latch.await(2, TimeUnit.SECONDS))
            assertSame(exception, received.get())
        } finally {
            executor.shutdown()
        }
    }

    // ---- getDefaultBlocking ----

    @Test
    fun `getDefaultBlocking returns ChatBuilder from suspend getDefault`() {
        val context = mockk<Context>()
        val config = mockk<SocketFactoryConfiguration>()
        val builder = mockk<ChatBuilder>()
        mockkObject(ChatBuilder.Companion)
        coEvery { ChatBuilder.getDefault(context, config, any()) } returns builder

        val result = getDefaultBlocking(context, config)

        assertSame(builder, result)
    }
}
