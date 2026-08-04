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

package com.nice.cxonechat.ui.storage

import androidx.media3.exoplayer.offline.DownloadManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.ConcurrentHashMap

/**
 * Instrumented tests for [DownloadUtil] verifying coroutine-safe lazy singleton
 * initialisation and correct lifecycle behaviour of [DownloadManager].
 */
@RunWith(AndroidJUnit4::class)
@MediumTest
class DownloadUtilInstrumentedTest {

    private val context by lazy {
        InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    }

    @Before
    fun setUp() = runBlocking {
        DownloadUtil.resetForTesting()
    }

    @After
    fun tearDown() = runBlocking {
        DownloadUtil.release()
    }

    @Test
    fun getDownloadManager_returnsSingletonInstance() = runBlocking {
        val first = DownloadUtil.getDownloadManager(context)
        val second = DownloadUtil.getDownloadManager(context)
        assertSame("Subsequent calls must return the exact same instance", first, second)
    }

    @Test
    fun getDownloadManager_concurrentCalls_returnSameInstance() = runBlocking {
        val threadCount = 10
        val results = ConcurrentHashMap<Int, DownloadManager>()
        // Gate that releases all coroutines simultaneously to maximise contention
        val gate = CompletableDeferred<Unit>()

        val jobs = (0 until threadCount).map { index ->
            launch(Dispatchers.Default) {
                gate.await()
                results[index] = DownloadUtil.getDownloadManager(context)
            }
        }

        gate.complete(Unit)
        jobs.joinAll()

        assertEquals("All threads must receive a result", threadCount, results.size)
        assertEquals(
            "All concurrent calls must return the same DownloadManager instance",
            1,
            results.values.toSet().size
        )
    }

    @Test
    fun release_clearsInstance_allowsNewInstanceCreation() = runBlocking {
        val first = DownloadUtil.getDownloadManager(context)
        assertNotNull(first)

        DownloadUtil.release()

        // After release, a fresh instance must be created
        val second = DownloadUtil.getDownloadManager(context)
        assertNotNull(second)
        assertNotSame("release() must clear the singleton so a new instance is created", first, second)
    }
}
