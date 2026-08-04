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

import com.nice.cxonechat.log.Level
import com.nice.cxonechat.tool.MockLogger
import io.mockk.verify
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

internal class ThreadingTest {

    private lateinit var threading: Threading

    @Before
    fun setUp() {
        threading = Threading(MockLogger())
    }

    @After
    fun tearDown() {
        threading.coroutineScope.coroutineContext[Job]?.cancel()
    }

    @Test
    fun `invoke returns Threading with coroutineScope and storageWriteScope on separate jobs`() {
        val coroutineJob = checkNotNull(threading.coroutineScope.coroutineContext[Job])
        val storageJob = checkNotNull(threading.storageWriteScope.coroutineContext[Job])
        assertNotSame(coroutineJob, storageJob)
        assertFalse(coroutineJob.isCancelled)
        assertFalse(storageJob.isCancelled)
    }

    @Test
    fun `storageWriteScope is cancelled when coroutineScope job is cancelled`() {
        val storageJob = checkNotNull(threading.storageWriteScope.coroutineContext[Job])
        assertFalse(storageJob.isCancelled)

        threading.coroutineScope.coroutineContext[Job]!!.cancel()

        // Job.cancel() transitions all children to "Cancelling" synchronously.
        assertTrue(storageJob.isCancelled)
    }

    @Test
    fun `uncaught storageWriteScope exception is logged at Error level`() {
        val logger = MockLogger()
        val threadingWithLogger = Threading(logger)
        try {
            @Suppress("TooGenericExceptionThrown")
            val job = threadingWithLogger.storageWriteScope.launch { throw RuntimeException("write failure") }
            // storageWriteScope uses SupervisorJob, so the child failure does not cancel the scope.
            // storageErrorHandler is invoked synchronously before the job transitions to Completed.
            runBlocking { job.join() }

            verify { logger.log(Level.Error, match { it.contains("Storage write failed") }, any()) }
        } finally {
            threadingWithLogger.coroutineScope.coroutineContext[Job]?.cancel()
        }
    }

    @Test
    fun `uncaught coroutine exception is logged at Error level`() {
        val logger = MockLogger()
        val threadingWithLogger = Threading(logger)
        try {
            @Suppress("TooGenericExceptionThrown")
            val job = threadingWithLogger.coroutineScope.launch { throw RuntimeException("test failure") }
            // CoroutineExceptionHandler is called synchronously before the job transitions to
            // Completed, so join() guarantees the logger.log() call has already happened.
            runBlocking { job.join() }

            verify { logger.log(Level.Error, any(), any()) }
        } finally {
            threadingWithLogger.coroutineScope.coroutineContext[Job]?.cancel()
        }
    }
}
