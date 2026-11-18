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

package com.nice.cxonechat.internal

import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.withTimeout
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Close/signOut lifecycle of [ChatImpl]: draining pending storage/cookie writes, keeping the
 * storage scope usable for reconnect, and cancelling in-flight session work before clearing
 * storage. Auth-body construction lives in [ChatImplTest]; connection flow in
 * [ChatImplConnectTest]; state emission in [ChatImplStateFlowTest].
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatImplLifecycleTest : AbstractChatImplTest() {

    /**
     * close() is now `runBlocking { closeSuspending() }`, which genuinely blocks the calling
     * thread until the storage/cookie drain completes. Testing "close() waits for the drain"
     * therefore requires running close() on a separate thread while the test thread controls the
     * drain gate -- calling close() on the test thread itself would deadlock (the gate can never
     * be released). A CountDownLatch proves close() has actually reached the gated await, not
     * merely that it hasn't returned yet; all waits are bounded so a regression here fails fast
     * instead of hanging. This pattern is shared by the four tests below.
     */
    @Test
    fun `close drains pending storage writes before finishing`() {
        val writeCompleted = AtomicBoolean(false)
        val gate = CompletableDeferred<Unit>()
        val drainGate = CompletableDeferred<Unit>()
        val awaitPendingWritesEntered = CountDownLatch(1)
        coEvery { entrails.cookieJar.awaitPendingWrites() } coAnswers {
            awaitPendingWritesEntered.countDown()
            drainGate.await()
        }
        entrails.threading.storageWriteScope.launch {
            gate.await()
            writeCompleted.set(true)
        }
        gate.complete(Unit)

        var closeError: Throwable? = null
        val closeThread = Thread { runCatching { chatImpl.close() }.onFailure { closeError = it } }.startDaemon()
        assertTrue(awaitPendingWritesEntered.await(5, SECONDS), "close() must enter awaitPendingWrites()")
        assertTrue(closeThread.isAlive, "close() must block until the drain gate is released")

        drainGate.complete(Unit)
        closeThread.join(5_000)
        assertFalse(closeThread.isAlive, "close() must return once the drain completes")
        assertNull(closeError, "close() must not throw")
        assertTrue(writeCompleted.get(), "close() must drain pending writes before returning")
    }

    @Test
    fun `close drains pending cookie writes before finishing`() {
        val drainGate = CompletableDeferred<Unit>()
        val awaitPendingWritesEntered = CountDownLatch(1)
        coEvery { entrails.cookieJar.awaitPendingWrites() } coAnswers {
            awaitPendingWritesEntered.countDown()
            drainGate.await()
        }

        var closeError: Throwable? = null
        val closeThread = Thread { runCatching { chatImpl.close() }.onFailure { closeError = it } }.startDaemon()
        assertTrue(awaitPendingWritesEntered.await(5, SECONDS), "close() must enter awaitPendingWrites()")
        assertTrue(closeThread.isAlive, "close() must block until the drain gate is released")

        drainGate.complete(Unit)
        closeThread.join(5_000)
        assertFalse(closeThread.isAlive, "close() must return once the drain completes")
        assertNull(closeError, "close() must not throw")
        coVerify { entrails.cookieJar.awaitPendingWrites() }
    }

    @Test
    fun `close leaves storage write scope usable for a later reconnect`() {
        val drainGate = CompletableDeferred<Unit>()
        val awaitPendingWritesEntered = CountDownLatch(1)
        coEvery { entrails.cookieJar.awaitPendingWrites() } coAnswers {
            awaitPendingWritesEntered.countDown()
            drainGate.await()
        }

        var closeError: Throwable? = null
        val closeThread = Thread { runCatching { chatImpl.close() }.onFailure { closeError = it } }.startDaemon()
        assertTrue(awaitPendingWritesEntered.await(5, SECONDS), "close() must enter awaitPendingWrites()")
        assertTrue(closeThread.isAlive, "close() must block until the drain gate is released")

        drainGate.complete(Unit)
        closeThread.join(5_000)
        assertFalse(closeThread.isAlive, "close() must return once the drain completes")
        assertNull(closeError, "close() must not throw")

        // close() keeps connect()/events() usable per the public contract — storage writes issued
        // by the next session on this instance must still execute.
        val lateWriteCompleted = AtomicBoolean(false)
        val lateWrite = entrails.threading.storageWriteScope.launch { lateWriteCompleted.set(true) }
        runBlocking { lateWrite.join() }
        assertTrue(
            lateWriteCompleted.get(), "storage write after close() must execute — close() must not cancel the storage scope"
        )
    }

    @Test
    fun `drain sentinel does not cancel jobs launched after close`() {
        val drainGate = CompletableDeferred<Unit>()
        val awaitPendingWritesEntered = CountDownLatch(1)
        coEvery { entrails.cookieJar.awaitPendingWrites() } coAnswers {
            awaitPendingWritesEntered.countDown()
            drainGate.await()
        }

        var closeError: Throwable? = null
        val closeThread = Thread { runCatching { chatImpl.close() }.onFailure { closeError = it } }.startDaemon()
        assertTrue(awaitPendingWritesEntered.await(5, SECONDS), "close() must enter awaitPendingWrites()")
        assertTrue(closeThread.isAlive, "close() must block until the drain gate is released")

        // A later connect() on this instance launches fresh listener jobs on the chat scope while
        // close() is still draining — those jobs must not be swept by close()'s own cleanup.
        val freshConnectJob = entrails.threading.coroutineScope.launch { awaitCancellation() }

        drainGate.complete(Unit)
        closeThread.join(5_000)
        assertFalse(closeThread.isAlive, "close() must return once the drain completes")
        assertNull(closeError, "close() must not throw")
        assertTrue(
            freshConnectJob.isActive, "close() must not cancel jobs launched by a connect() that follows it"
        )
        freshConnectJob.cancel()
    }

    @Test
    fun `closeSuspending does not deadlock when invoked from within storageWriteScope`() {
        val completed = AtomicBoolean(false)

        // closeSuspending()'s own coroutine would be a child of storageJob here -- without
        // excluding itself, storageJob.children.joinAll() would wait on its own (currently
        // running, not-yet-complete) Job forever.
        val job = entrails.threading.storageWriteScope.launch {
            chatImpl.closeSuspending()
            completed.set(true)
        }

        runBlocking { withTimeout(5_000) { job.join() } }
        assertTrue(completed.get(), "closeSuspending() invoked from storageWriteScope must not deadlock on its own Job")
    }

    /**
     * Regression test: signOut() must cancel in-flight session work (e.g. a token refresh
     * already past its network await, like TokenRefreshHelper.refreshAccessToken()) before
     * clearing storage -- otherwise that coroutine can still reach a property setter and
     * persist fresh credentials via storageWriteScope after clearStorage() already ran,
     * re-persisting them after logout.
     */
    @Test
    fun `signOut cancels in-flight session work before clearing storage`() {
        lateinit var sessionJob: Job
        // Job.cancel() flips isCancelled synchronously regardless of dispatcher/thread, so
        // checking it inside the clearStorage() mock answer directly proves ordering -- no
        // dependency on when the cancelled coroutine's own cleanup happens to run.
        var sessionCancelledBeforeClear = false
        coEvery { storage.clearStorage() } coAnswers {
            sessionCancelledBeforeClear = sessionJob.isCancelled
        }

        sessionJob = entrails.threading.coroutineScope.launch { awaitCancellation() }
        val testScheduler = (entrails.threading.coroutineScope as TestScope).testScheduler
        testScheduler.advanceUntilIdle()
        assertTrue(sessionJob.isActive, "session job must be active (suspended) before signOut() runs")

        // closeSuspending() now joins the cancelled session job before proceeding, which needs this
        // virtual-time scheduler pumped -- run signOut() on its own thread and pump concurrently.
        var signOutError: Throwable? = null
        val signOutThread = Thread {
            runCatching { runBlocking { withTimeout(5_000) { chatImpl.signOut() } } }.onFailure { signOutError = it }
        }.startDaemon()
        val deadline = System.nanoTime() + SECONDS.toNanos(5)
        while (signOutThread.isAlive && System.nanoTime() < deadline) {
            testScheduler.advanceUntilIdle()
        }
        signOutThread.join(1_000)
        assertFalse(signOutThread.isAlive, "signOut() must complete")
        assertNull(signOutError, "signOut() must not throw")

        assertTrue(sessionCancelledBeforeClear, "signOut() must cancel in-flight session work before calling clearStorage()")
    }

    /**
     * Regression test: cancellation only lands at a suspend point. A session job blocked past its
     * last one can still launch a storage write before it notices. closeSuspending() must join the
     * cancelled children before draining, so that write is already captured -- otherwise it races
     * past close()/signOut() and can persist stale data after they've already returned.
     *
     * Uses real dispatchers (not the shared virtual-time TestScope) so the race can be staged
     * deterministically with latches instead of a scheduler.
     */
    @Test
    fun `closeSuspending awaits a session job's cancellation cleanup before draining storage writes`() {
        val realScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val realStorageDispatcher = Dispatchers.IO.limitedParallelism(1)
        val realThreading = ThreadingExecutor(
            coroutineScope = realScope,
            storageWriteScope = CoroutineScope(
                realScope.coroutineContext + SupervisorJob(realScope.coroutineContext[Job]) + realStorageDispatcher
            ),
            storageDispatcher = realStorageDispatcher,
        )
        val realEntrails = object : ChatEntrails by entrails {
            override val threading = realThreading
        }
        val chat = ChatImpl(connection, realEntrails, dependencies, configuration, chatStateListener)

        val readyToRace = CountDownLatch(1)
        val proceed = CountDownLatch(1)
        val writeGate = CompletableDeferred<Unit>()
        val lateWriteCompleted = AtomicBoolean(false)
        realScope.launch {
            readyToRace.countDown()
            proceed.await() // blocks the thread, not a coroutine suspend point -- cancel() won't stop this
            realThreading.storageWriteScope.launch {
                writeGate.await()
                lateWriteCompleted.set(true)
            }
        }
        assertTrue(readyToRace.await(5, SECONDS), "session job must be running before close() starts")

        val awaitPendingWritesEntered = CountDownLatch(1)
        coEvery { entrails.cookieJar.awaitPendingWrites() } coAnswers { awaitPendingWritesEntered.countDown() }

        var closeError: Throwable? = null
        val closeThread = Thread { runCatching { chat.close() }.onFailure { closeError = it } }.startDaemon()
        assertFalse(
            actual = awaitPendingWritesEntered.await(300, TimeUnit.MILLISECONDS),
            message = "close() must not reach awaitPendingWrites() before the cancelled session job's cleanup completes"
        )

        proceed.countDown()
        assertTrue(awaitPendingWritesEntered.await(5, SECONDS), "close() must proceed once cleanup completes")
        assertTrue(closeThread.isAlive, "close() must still be draining the late write")

        writeGate.complete(Unit)
        closeThread.join(5_000)
        assertFalse(closeThread.isAlive, "close() must return once the late write completes")
        assertNull(closeError, "close() must not throw")
        assertTrue(lateWriteCompleted.get(), "close() must await the write launched during session-job cancellation cleanup")
    }
}
