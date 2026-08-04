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

import com.nice.cxonechat.Chat
import com.nice.cxonechat.ChatStateEvent
import com.nice.cxonechat.exceptions.CXoneException
import com.nice.cxonechat.exceptions.InternalError
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatJavaInteropTest {

    // ---- connectAsync ----

    @Test
    fun `connectAsync calls onSuccess on success`() = runTest {
        val chat = mockk<Chat>()
        coEvery { chat.connect() } returns Unit
        val called = AtomicBoolean(false)

        connectChat(chat, onSuccess = { called.set(true) })

        assertTrue(called.get())
    }

    @Test
    fun `connectAsync routes CXoneException to onError unchanged`() = runTest {
        val chat = mockk<Chat>()
        val exception = mockk<CXoneException>(relaxed = true)
        coEvery { chat.connect() } throws exception
        val received = AtomicReference<CXoneException?>(null)

        connectChat(chat, onError = { received.set(it) })

        assertSame(exception, received.get())
    }

    @Test
    fun `connectAsync wraps non-CXoneException in InternalError`() = runTest {
        val chat = mockk<Chat>()
        coEvery { chat.connect() } throws RuntimeException("boom")
        val received = AtomicReference<CXoneException?>(null)

        connectChat(chat, onError = { received.set(it) })

        assertIs<InternalError>(received.get())
    }

    @Test
    fun `connectAsync returned Cancellable cancels the job`() = runTest {
        val chat = mockk<Chat>()
        coEvery { chat.connect() } returns Unit
        val onSuccessCalled = AtomicBoolean(false)

        val job = launch { connectChat(chat, onSuccess = { onSuccessCalled.set(true) }) }
        job.cancel()
        advanceUntilIdle()

        assertFalse(onSuccessCalled.get())
    }

    @Test
    fun `connectAsync routes onSuccess through the supplied Executor`() {
        val chat = mockk<Chat>()
        coEvery { chat.connect() } returns Unit
        val callbackThreadName = AtomicReference<String?>(null)
        val executor = java.util.concurrent.Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "interop-callback-thread")
        }
        val latch = java.util.concurrent.CountDownLatch(1)

        try {
            connectAsync(
                chat = chat,
                onSuccess = {
                    callbackThreadName.set(Thread.currentThread().name)
                    latch.countDown()
                },
                callbackExecutor = executor,
            )

            assertTrue(latch.await(2, java.util.concurrent.TimeUnit.SECONDS))
            // Kotlin coroutines may append a debug suffix like " @coroutine#42" to the thread
            // name when running on a wrapped executor; assert the prefix matches the executor's
            // dedicated thread.
            assertTrue(callbackThreadName.get()?.startsWith("interop-callback-thread") == true)
        } finally {
            executor.shutdown()
        }
    }

    @Test
    fun `connectAsync routes onError through the supplied Executor`() {
        val chat = mockk<Chat>()
        val exception = mockk<CXoneException>(relaxed = true)
        coEvery { chat.connect() } throws exception
        val callbackThreadName = AtomicReference<String?>(null)
        val executor = java.util.concurrent.Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "interop-callback-thread")
        }
        val latch = java.util.concurrent.CountDownLatch(1)

        try {
            connectAsync(
                chat = chat,
                onError = {
                    callbackThreadName.set(Thread.currentThread().name)
                    latch.countDown()
                },
                callbackExecutor = executor,
            )

            assertTrue(latch.await(2, java.util.concurrent.TimeUnit.SECONDS))
            // Kotlin coroutines may append a debug suffix like " @coroutine#42" to the thread
            // name when running on a wrapped executor; assert the prefix matches the executor's
            // dedicated thread.
            assertTrue(callbackThreadName.get()?.startsWith("interop-callback-thread") == true)
        } finally {
            executor.shutdown()
        }
    }

    // ---- getChannelAvailabilityAsync ----

    @Test
    fun `getChannelAvailabilityAsync delivers value to onSuccess`() = runTest {
        val chat = mockk<Chat>()
        coEvery { chat.getChannelAvailability() } returns true
        val received = AtomicReference<Boolean?>(null)

        fetchChannelAvailability(chat, onSuccess = { received.set(it) })

        assertEquals(true, received.get())
    }

    @Test
    fun `getChannelAvailabilityAsync routes CXoneException to onError unchanged`() = runTest {
        val chat = mockk<Chat>()
        val exception = mockk<CXoneException>(relaxed = true)
        coEvery { chat.getChannelAvailability() } throws exception
        val received = AtomicReference<CXoneException?>(null)

        fetchChannelAvailability(chat, onSuccess = { }, onError = { received.set(it) })

        assertSame(exception, received.get())
    }

    @Test
    fun `getChannelAvailabilityAsync wraps non-CXoneException in InternalError`() = runTest {
        val chat = mockk<Chat>()
        coEvery { chat.getChannelAvailability() } throws IllegalStateException("fail")
        val received = AtomicReference<CXoneException?>(null)

        fetchChannelAvailability(chat, onSuccess = { }, onError = { received.set(it) })

        assertIs<InternalError>(received.get())
    }

    @Test
    fun `getChannelAvailabilityAsync public path delivers via callbackExecutor`() {
        val chat = mockk<Chat>()
        coEvery { chat.getChannelAvailability() } returns true
        val executor = java.util.concurrent.Executors.newSingleThreadExecutor { Thread(it, "availability-cb") }
        val latch = java.util.concurrent.CountDownLatch(1)
        val capturedThread = AtomicReference<String?>(null)
        val received = AtomicReference<Boolean?>(null)

        getChannelAvailabilityAsync(
            chat = chat,
            onSuccess = {
                received.set(it)
                capturedThread.set(Thread.currentThread().name)
                latch.countDown()
            },
            callbackExecutor = executor,
        )
        try {
            assertTrue(latch.await(2, java.util.concurrent.TimeUnit.SECONDS))
            assertEquals(true, received.get())
            assertTrue(capturedThread.get()?.startsWith("availability-cb") == true)
        } finally {
            executor.shutdown()
        }
    }

    @Test
    fun `getChannelAvailabilityAsync public path routes onError via callbackExecutor`() {
        val chat = mockk<Chat>()
        val exception = mockk<CXoneException>(relaxed = true)
        coEvery { chat.getChannelAvailability() } throws exception
        val executor = java.util.concurrent.Executors.newSingleThreadExecutor { Thread(it, "availability-err") }
        val latch = java.util.concurrent.CountDownLatch(1)
        val received = AtomicReference<CXoneException?>(null)

        getChannelAvailabilityAsync(
            chat = chat,
            onSuccess = { },
            onError = {
                received.set(it)
                latch.countDown()
            },
            callbackExecutor = executor,
        )
        try {
            assertTrue(latch.await(2, java.util.concurrent.TimeUnit.SECONDS))
            assertSame(exception, received.get())
        } finally {
            executor.shutdown()
        }
    }

    // ---- observeState ----

    @Test
    fun `observeState delivers each emission to listener`() = runTest {
        val flow = MutableSharedFlow<ChatStateEvent>()
        val chat = mockk<Chat>()
        every { chat.stateFlow } returns flow.asSharedFlow()
        val received = mutableListOf<ChatStateEvent>()
        val event1 = mockk<ChatStateEvent>()
        val event2 = mockk<ChatStateEvent>()

        val job = launch { collectStateFlow(chat, listener = { received += it }) }
        runCurrent()
        flow.emit(event1)
        flow.emit(event2)
        advanceUntilIdle()
        job.cancel()

        assertEquals(listOf(event1, event2), received)
    }

    @Test
    fun `observeState Cancellable stops further emissions`() = runTest {
        val flow = MutableSharedFlow<ChatStateEvent>()
        val chat = mockk<Chat>()
        every { chat.stateFlow } returns flow.asSharedFlow()
        val received = mutableListOf<ChatStateEvent>()
        val first = mockk<ChatStateEvent>()
        val second = mockk<ChatStateEvent>()

        val job = launch { collectStateFlow(chat, listener = { received += it }) }
        runCurrent()
        flow.emit(first)
        advanceUntilIdle()
        job.cancel()
        flow.emit(second)
        advanceUntilIdle()

        assertEquals(listOf(first), received)
    }

    @Test
    fun `observeState keeps collector alive when listener throws on a prior emission`() = runTest {
        val flow = MutableSharedFlow<ChatStateEvent>()
        val chat = mockk<Chat>()
        every { chat.stateFlow } returns flow.asSharedFlow()
        val received = mutableListOf<ChatStateEvent>()
        val first = mockk<ChatStateEvent>()
        val second = mockk<ChatStateEvent>()

        // Replace the default uncaught-exception handler so the `safeAccept` re-route doesn't
        // crash the test JVM when the listener throws.
        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { _, _ -> /* swallow for the test */ }
        try {
            val job = launch {
                collectStateFlow(chat, listener = { event ->
                    received += event
                    if (event === first) throw RuntimeException("listener bug")
                })
            }
            runCurrent()
            flow.emit(first)
            flow.emit(second)
            advanceUntilIdle()
            job.cancel()

            // Without `safeAccept`, the throw on `first` would terminate the collect and
            // `second` would never arrive.
            assertEquals(listOf(first, second), received)
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(originalHandler)
        }
    }

    @Test
    fun `observeState public path delivers via callbackExecutor`() {
        val event = mockk<ChatStateEvent>()
        val flow = MutableSharedFlow<ChatStateEvent>(replay = 1).apply { tryEmit(event) }
        val chat = mockk<Chat>()
        every { chat.stateFlow } returns flow.asSharedFlow()
        val executor = java.util.concurrent.Executors.newSingleThreadExecutor { Thread(it, "state-cb") }
        val latch = java.util.concurrent.CountDownLatch(1)
        val capturedThread = AtomicReference<String?>(null)

        val cancellable = observeState(
            chat = chat,
            listener = {
                capturedThread.set(Thread.currentThread().name)
                latch.countDown()
            },
            callbackExecutor = executor,
        )
        try {
            assertTrue(latch.await(2, java.util.concurrent.TimeUnit.SECONDS))
            assertTrue(capturedThread.get()?.startsWith("state-cb") == true)
        } finally {
            cancellable.cancel()
            executor.shutdown()
        }
    }

    @Test
    fun `connectAsync throwing onSuccess does NOT route to onError`() = runTest {
        val chat = mockk<Chat>()
        coEvery { chat.connect() } returns Unit
        val onErrorCalled = AtomicBoolean(false)
        val capturedThrow = AtomicReference<Throwable?>(null)
        // The throw from `onSuccess` propagates past the SDK try/catch into the launched
        // coroutine. Without a CoroutineExceptionHandler, `runTest` would surface it as a
        // test failure. Install a swallow handler that also lets us assert the throw escaped
        // the SDK try/catch (proving it wasn't mislabeled as InternalError).
        val swallowHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, t ->
            capturedThrow.set(t)
        }
        // CoroutineExceptionHandler only fires for *root* coroutines; SupervisorJob makes the
        // launched coroutine root-like (its uncaught throws route to the handler rather than
        // propagating up to TestScope's exception reporter).
        val scopeWithHandler = kotlinx.coroutines.CoroutineScope(
            this.coroutineContext + swallowHandler + kotlinx.coroutines.SupervisorJob()
        )

        scopeWithHandler.launch {
            connectChat(
                chat = chat,
                onSuccess = { throw RuntimeException("consumer bug in onSuccess") },
                onError = { onErrorCalled.set(true) },
            )
        }
        advanceUntilIdle()

        // Consumer-side bug in onSuccess must NOT be mislabeled as an SDK error via onError.
        assertFalse(onErrorCalled.get())
        // Bonus: the throw DID escape the SDK try/catch (otherwise the handler wouldn't see it).
        assertIs<RuntimeException>(capturedThrow.get())
    }

    @Test
    fun `collectWithErrorCallback routes upstream CXoneException to onError`() = runTest {
        val capturedError = AtomicReference<CXoneException?>(null)
        val expectedException = mockk<CXoneException>(relaxed = true)
        val received = mutableListOf<ChatStateEvent>()
        val firstEvent = mockk<ChatStateEvent>()

        launch {
            collectWithErrorCallback(
                source = flow {
                    emit(firstEvent)
                    throw expectedException
                },
                listener = { received += it },
                onError = { capturedError.set(it) },
                errorContext = "Unexpected error in stateFlow observer",
            )
        }
        advanceUntilIdle()

        assertEquals(1, received.size)
        assertSame(expectedException, capturedError.get())
    }

    @Test
    fun `collectWithErrorCallback wraps non-CXoneException upstream throws in InternalError`() = runTest {
        val capturedError = AtomicReference<CXoneException?>(null)

        launch {
            collectWithErrorCallback(
                source = flow<ChatStateEvent> {
                    throw IllegalStateException("upstream boom")
                },
                listener = { },
                onError = { capturedError.set(it) },
                errorContext = "Unexpected error in stateFlow observer",
            )
        }
        advanceUntilIdle()

        assertIs<InternalError>(capturedError.get())
    }

    @Test
    fun `collectWithErrorCallback rethrows upstream errors when onError is null`() = runTest {
        val capturedThrow = AtomicReference<Throwable?>(null)
        val swallowHandler = kotlinx.coroutines.CoroutineExceptionHandler { _, t ->
            capturedThrow.set(t)
        }
        val scopeWithHandler = kotlinx.coroutines.CoroutineScope(
            this.coroutineContext + swallowHandler + kotlinx.coroutines.SupervisorJob()
        )

        scopeWithHandler.launch {
            collectWithErrorCallback(
                source = flow<ChatStateEvent> {
                    throw IllegalStateException("upstream boom")
                },
                listener = { },
                onError = null,
                errorContext = "Unexpected error in stateFlow observer",
            )
        }
        advanceUntilIdle()

        // Null onError must NOT swallow upstream errors — they escape so the host's
        // CoroutineExceptionHandler / UncaughtExceptionHandler can observe them.
        assertIs<InternalError>(capturedThrow.get())
    }

    // ---- JVM-bytecode invisibility of CoroutineScope constructor (R2.6) ----

    @Test
    fun `wrapper Java-visible constructors do not expose CoroutineScope`() {
        // Java consumers see only non-synthetic public constructors — `javac` refuses to resolve
        // calls to synthetic ones. The companion `invoke` operator emits a `public synthetic`
        // constructor bridge that is NOT Java-callable; filter those out and assert the
        // remaining surface contains no CoroutineScope parameter.
        val classes = listOf(
            com.nice.cxonechat.api.ChatEventHandlerCoroutineWrapper::class.java,
            com.nice.cxonechat.api.ChatThreadEventHandlerCoroutineWrapper::class.java,
            com.nice.cxonechat.api.ChatThreadMessageHandlerCoroutineWrapper::class.java,
        )
        classes.forEach { cls ->
            val leak = cls.constructors.any { ctor ->
                !ctor.isSynthetic &&
                    ctor.parameterTypes.any { it == kotlinx.coroutines.CoroutineScope::class.java }
            }
            assertFalse(leak, "${cls.simpleName}: CoroutineScope leaked to Java-visible constructor")
        }
    }
}
