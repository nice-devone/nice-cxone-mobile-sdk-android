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

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.time.Clock

/**
 * In-memory cache semantics of [PersistentCookieJar] (immediate updates, merging, replacement,
 * no redundant reads) and [PersistentCookieJar.awaitPendingWrites]. Write-side behavior lives in
 * [PersistentCookieJarTest]; read filtering lives in [PersistentCookieJarLoadTest].
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class PersistentCookieJarCacheTest : AbstractPersistentCookieJarTest() {

    @Test
    fun `loadForRequest reads from cache without calling data store loadCookies`() = runTest {
        val futureTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)
        coEvery { mockDataStore.loadAllCookies() } returns listOf(
            createCookie("cached", "value", "example.com", futureTime)
        )
        val cookieJar = PersistentCookieJar.create(mockDataStore, this)

        cookieJar.loadForRequest(testUrl)
        cookieJar.loadForRequest(testUrl)

        coVerify(exactly = 0) { mockDataStore.loadCookies(any()) }
    }

    @Test
    fun `saveFromResponse updates cache immediately before async write completes`() = runTest {
        val cookieJar = PersistentCookieJar.create(mockDataStore, this)
        val futureTime = Clock.System.now().toEpochMilliseconds() + TimeUnit.HOURS.toMillis(1)
        val cookie = createCookie("immediate", "value", "example.com", futureTime)

        cookieJar.saveFromResponse(testUrl, listOf(cookie))

        val result = cookieJar.loadForRequest(testUrl)
        result.any { it.name == "immediate" } shouldBe true
    }

    @Test
    fun `saveFromResponse merges without discarding pre-existing same-host cookies`() = runTest {
        val futureTime = Clock.System.now().toEpochMilliseconds() + TimeUnit.DAYS.toMillis(1)
        val existing = createCookie("existing", "oldValue", "example.com", futureTime)
        coEvery { mockDataStore.loadAllCookies() } returns listOf(existing)
        val cookieJar = PersistentCookieJar.create(mockDataStore, this)

        val newCookie = createCookie("newCookie", "newValue", "example.com", futureTime)
        cookieJar.saveFromResponse(testUrl, listOf(newCookie))

        val result = cookieJar.loadForRequest(testUrl)
        result.any { it.name == "existing" } shouldBe true
        result.any { it.name == "newCookie" } shouldBe true
    }

    @Test
    fun `saveFromResponse replaces same-named cookie for same host without duplication`() = runTest {
        val futureTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)
        val oldCookie = createCookie("session", "old-value", "example.com", futureTime)
        coEvery { mockDataStore.loadAllCookies() } returns listOf(oldCookie)
        val cookieJar = PersistentCookieJar.create(mockDataStore, this)

        val newCookie = createCookie("session", "new-value", "example.com", futureTime)
        cookieJar.saveFromResponse(testUrl, listOf(newCookie))

        val result = cookieJar.loadForRequest(testUrl)
        result.count { it.name == "session" } shouldBe 1
        result.first { it.name == "session" }.value shouldBe "new-value"
    }

    @Test
    fun `awaitPendingWrites suspends until pending clearAllCookies DataStore write completes`() = runTest {
        var clearCompleted = false
        coEvery { mockDataStore.clear() } coAnswers { clearCompleted = true }
        val cookieJar = PersistentCookieJar.create(mockDataStore, this)

        cookieJar.clearAllCookies()
        cookieJar.awaitPendingWrites()

        clearCompleted shouldBe true
    }

    @Test
    fun `awaitPendingWrites suspends until pending saveFromResponse write completes`() = runTest {
        var saveCompleted = false
        coEvery { mockDataStore.saveCookies(any(), any()) } coAnswers { saveCompleted = true }
        val cookieJar = PersistentCookieJar.create(mockDataStore, this)
        val cookie = createCookie("token", "abc", "example.com")

        cookieJar.saveFromResponse(testUrl, listOf(cookie))
        cookieJar.awaitPendingWrites()

        saveCompleted shouldBe true
    }

    @Test
    fun `awaitPendingWrites waits for all concurrent pending writes`() = runTest {
        var completionCount = 0
        coEvery { mockDataStore.saveCookies(any(), any()) } coAnswers { completionCount++ }
        val cookieJar = PersistentCookieJar.create(mockDataStore, this)
        val futureTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)

        cookieJar.saveFromResponse(testUrl, listOf(createCookie("a", "1", "example.com", futureTime)))
        cookieJar.saveFromResponse(testUrl, listOf(createCookie("b", "2", "example.com", futureTime)))
        cookieJar.saveFromResponse(testUrl, listOf(createCookie("c", "3", "example.com", futureTime)))
        cookieJar.awaitPendingWrites()

        completionCount shouldBe 3
    }

    @Test
    fun `awaitPendingWrites returns immediately when no writes are pending`() = runTest {
        val cookieJar = PersistentCookieJar.create(mockDataStore, this)
        cookieJar.awaitPendingWrites()
    }
}
