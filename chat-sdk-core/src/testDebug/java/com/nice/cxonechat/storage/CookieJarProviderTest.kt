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

package com.nice.cxonechat.storage

import android.content.Context
import io.mockk.mockk
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

class CookieJarProviderTest {
    private lateinit var context: Context
    @Rule @JvmField val tempFolder = TemporaryFolder()

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        CookieJarProvider.clearInstance()
    }

    @Test
    fun testSingletonInstance() {
        val testCookieDataStore = createTestCookieDataStore(tempDir = tempFolder.newFolder())
        val instance1 = CookieJarProvider.getInstance(context, testCookieDataStore)
        val instance2 = CookieJarProvider.getInstance(context, testCookieDataStore)
        assertSame(instance1, instance2)
    }

    @Test
    fun testThreadSafety() {
        val testCookieDataStore = createTestCookieDataStore(tempDir = tempFolder.newFolder())
        val results = mutableListOf<PersistentCookieJar>()
        val latch = CountDownLatch(10)
        val executor = Executors.newFixedThreadPool(10)
        repeat(10) {
            executor.execute {
                results.add(CookieJarProvider.getInstance(context, testCookieDataStore))
                latch.countDown()
            }
        }
        latch.await()
        val first = results.first()
        assertTrue(results.all { it === first })
    }

    @Test
    fun testDelegationToPersistentCookieJar() {
        val testCookieDataStore = createTestCookieDataStore(tempDir = tempFolder.newFolder())
        val jar = CookieJarProvider.getInstance(context, testCookieDataStore)
        val url = "https://cookie.com/".toHttpUrl()
        val cookie = Cookie.Builder().name("c").value("v").domain("cookie.com").build()
        jar.saveFromResponse(url, listOf(cookie))
        val loaded = jar.loadForRequest(url)
        assertTrue(loaded.any { it.name == "c" && it.value == "v" })
    }
}
