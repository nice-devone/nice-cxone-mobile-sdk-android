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

import com.nice.cxonechat.log.Level
import com.nice.cxonechat.log.Logger
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.Cookie
import org.junit.Test
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Write-side behavior of [PersistentCookieJar]: save delegation, error logging, and
 * [PersistentCookieJar.clearAllCookies]. Read filtering lives in [PersistentCookieJarLoadTest];
 * in-memory cache and pending-write semantics live in [PersistentCookieJarCacheTest].
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class PersistentCookieJarTest : AbstractPersistentCookieJarTest() {

    @Test
    fun `saveFromResponse passes url and cookies to data store`() = runTest {
        val cookieJar = PersistentCookieJar.create(mockDataStore, this)
        val cookies = listOf(
            createCookie("sessionId", "abc123", "example.com"),
            createCookie("token", "xyz789", "example.com")
        )

        cookieJar.saveFromResponse(testUrl, cookies)
        advanceUntilIdle()

        coVerify { mockDataStore.saveCookies(testUrl, cookies) }
    }

    @Test
    fun `saveFromResponse with empty cookie list delegates to store`() = runTest {
        val cookieJar = PersistentCookieJar.create(mockDataStore, this)
        val cookies = emptyList<Cookie>()

        cookieJar.saveFromResponse(testUrl, cookies)
        advanceUntilIdle()

        coVerify { mockDataStore.saveCookies(testUrl, cookies) }
    }

    @Test
    fun `failed saveCookies write is logged at Error level`() = runTest {
        val mockLogger = mockk<Logger>(relaxed = true)
        coEvery { mockDataStore.saveCookies(any(), any()) } throws IOException("disk full")
        val cookieJar = PersistentCookieJar.create(mockDataStore, this, mockLogger)

        cookieJar.saveFromResponse(testUrl, listOf(createCookie("token", "abc", "example.com")))
        advanceUntilIdle()

        verify {
            mockLogger.log(
                level = Level.Error,
                message = match { it.contains("Failed to persist cookies for example.com") },
                throwable = any<IOException>(),
            )
        }
    }

    @Test
    fun `failed clear write is logged at Error level`() = runTest {
        val mockLogger = mockk<Logger>(relaxed = true)
        coEvery { mockDataStore.clear() } throws IOException("disk full")
        val cookieJar = PersistentCookieJar.create(mockDataStore, this, mockLogger)

        cookieJar.clearAllCookies()
        advanceUntilIdle()

        verify {
            mockLogger.log(
                level = Level.Error,
                message = match { it.contains("Failed to clear cookie storage") },
                throwable = any<IOException>(),
            )
        }
    }

    @Test
    fun `cancellation during saveCookies is not logged as error`() = runTest {
        val mockLogger = mockk<Logger>(relaxed = true)
        coEvery { mockDataStore.saveCookies(any(), any()) } throws CancellationException("scope cancelled")
        val cookieJar = PersistentCookieJar.create(mockDataStore, this, mockLogger)

        cookieJar.saveFromResponse(testUrl, listOf(createCookie("token", "abc", "example.com")))
        advanceUntilIdle()

        verify(exactly = 0) { mockLogger.log(level = Level.Error, message = any(), throwable = any()) }
    }

    @Test
    fun `clearAllCookies calls data store clear`() = runTest {
        val cookieJar = PersistentCookieJar.create(mockDataStore, this)

        cookieJar.clearAllCookies()
        advanceUntilIdle()

        coVerify { mockDataStore.clear() }
    }

    /**
     * Regression test: [PersistentCookieJar.clearAllCookies] must suspend until its DataStore
     * write completes before returning to the caller — it is not fire-and-forget like
     * [PersistentCookieJar.saveFromResponse]. Callers such as [com.nice.cxonechat.internal.ChatImpl.signOut]
     * rely on [clearAllCookies] being a properly awaited suspend call; if it were fire-and-forget,
     * [Chat.signOut] could return with cookies still in flight, and a subsequent
     * [ChatInstanceProvider.prepare] could race the persisted clear.
     */
    @Test
    fun `clearAllCookies is self-awaiting - store clear is done before the call returns`() = runTest {
        val clearEntered = kotlinx.coroutines.CompletableDeferred<Unit>()
        val clearGate = kotlinx.coroutines.CompletableDeferred<Unit>()
        coEvery { mockDataStore.clear() } coAnswers {
            clearEntered.complete(Unit)
            clearGate.await()
        }
        val cookieJar = PersistentCookieJar.create(mockDataStore, this)

        val clearCall = async { cookieJar.clearAllCookies() }

        clearEntered.await()
        clearCall.isCompleted shouldBe false

        clearGate.complete(Unit)
        clearCall.await()
    }

    @Test
    fun `clearAllCookies clears cache immediately before async write completes`() = runTest {
        val futureTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)
        coEvery { mockDataStore.loadAllCookies() } returns listOf(
            createCookie("existing", "value", "example.com", futureTime)
        )
        val cookieJar = PersistentCookieJar.create(mockDataStore, this)

        cookieJar.clearAllCookies()

        cookieJar.loadForRequest(testUrl).shouldBeEmpty()
    }
}
