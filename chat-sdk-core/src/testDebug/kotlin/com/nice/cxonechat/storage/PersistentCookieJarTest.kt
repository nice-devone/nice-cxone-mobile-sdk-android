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
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
internal class PersistentCookieJarTest {
    private lateinit var mockDataStore: EncryptedCookieDataStore
    private val testUrl: HttpUrl = "https://example.com/path".toHttpUrl()

    @Before
    fun setUp() {
        mockDataStore = mockk(relaxed = true) {
            coEvery { loadAllCookies() } returns emptyList()
        }
    }

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
    fun `loadForRequest returns non-expired cookies from cache`() = runTest {
        val futureTime = Clock.System.now().toEpochMilliseconds() + TimeUnit.DAYS.toMillis(1)
        val cookies = listOf(
            createCookie("valid1", "value1", "example.com", futureTime),
            createCookie("valid2", "value2", "example.com", futureTime)
        )
        coEvery { mockDataStore.loadAllCookies() } returns cookies
        val cookieJar = PersistentCookieJar.create(mockDataStore, this)

        val result = cookieJar.loadForRequest(testUrl)

        result shouldHaveSize 2
    }

    @Test
    fun `loadForRequest filters expired cookies from cache`() = runTest {
        val pastTime = Clock.System.now().toEpochMilliseconds() - TimeUnit.DAYS.toMillis(1)
        val futureTime = Clock.System.now().toEpochMilliseconds() + TimeUnit.DAYS.toMillis(1)
        val cookies = listOf(
            createCookie("expired", "value1", "example.com", pastTime),
            createCookie("valid", "value2", "example.com", futureTime)
        )
        coEvery { mockDataStore.loadAllCookies() } returns cookies
        val cookieJar = PersistentCookieJar.create(mockDataStore, this)

        val result = cookieJar.loadForRequest(testUrl)

        result shouldHaveSize 1
        result.first().name shouldBe "valid"
    }

    @Test
    fun `loadForRequest returns empty when all cookies expired`() = runTest {
        val pastTime = Clock.System.now().toEpochMilliseconds() - TimeUnit.HOURS.toMillis(1)
        val cookies = listOf(
            createCookie("expired1", "value1", "example.com", pastTime),
            createCookie("expired2", "value2", "example.com", pastTime)
        )
        coEvery { mockDataStore.loadAllCookies() } returns cookies
        val cookieJar = PersistentCookieJar.create(mockDataStore, this)

        val result = cookieJar.loadForRequest(testUrl)

        result.shouldBeEmpty()
    }

    @Test
    fun `loadForRequest filters cookies from different root domain`() = runTest {
        val futureTime = Clock.System.now().toEpochMilliseconds() + TimeUnit.DAYS.toMillis(1)
        val exampleCookie = createCookie("id", "123", "example.com", futureTime)
        val otherCookie = createCookie("id", "456", "other.com", futureTime)
        coEvery { mockDataStore.loadAllCookies() } returns listOf(exampleCookie, otherCookie)
        val cookieJar = PersistentCookieJar.create(mockDataStore, this)

        val result = cookieJar.loadForRequest(testUrl)

        result.any { it.domain == "example.com" } shouldBe true
        result.any { it.domain == "other.com" } shouldBe false
    }

    @Test
    fun `clearAllCookies calls data store clear`() = runTest {
        val cookieJar = PersistentCookieJar.create(mockDataStore, this)

        cookieJar.clearAllCookies()
        advanceUntilIdle()

        coVerify { mockDataStore.clear() }
    }

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
    fun `clearAllCookies clears cache immediately before async write completes`() = runTest {
        val futureTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)
        coEvery { mockDataStore.loadAllCookies() } returns listOf(
            createCookie("existing", "value", "example.com", futureTime)
        )
        val cookieJar = PersistentCookieJar.create(mockDataStore, this)

        cookieJar.clearAllCookies()

        cookieJar.loadForRequest(testUrl).shouldBeEmpty()
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
    fun `loadForRequest handles cookies from different API endpoints on same host`() = runTest {
        val futureTime = Clock.System.now().toEpochMilliseconds() + TimeUnit.DAYS.toMillis(1)
        val allCookies = listOf(
            createCookie("authToken", "auth123", "api.example.com", futureTime),
            createCookie("chatSession", "chat456", "api.example.com", futureTime),
            createCookie("socketToken", "socket789", "api.example.com", futureTime)
        )
        coEvery { mockDataStore.loadAllCookies() } returns allCookies
        val cookieJar = PersistentCookieJar.create(mockDataStore, this)

        cookieJar.loadForRequest("https://api.example.com/oauth/token".toHttpUrl()) shouldHaveSize 3
        cookieJar.loadForRequest("https://api.example.com/chat/messages".toHttpUrl()) shouldHaveSize 3
        cookieJar.loadForRequest("https://api.example.com/socket/connect".toHttpUrl()) shouldHaveSize 3
    }

    @Test
    fun `loadForRequest applies both expiration and domain matching filters`() = runTest {
        val futureTime = Clock.System.now().toEpochMilliseconds() + TimeUnit.DAYS.toMillis(1)
        val pastTime = Clock.System.now().toEpochMilliseconds() - TimeUnit.HOURS.toMillis(1)

        coEvery { mockDataStore.loadAllCookies() } returns listOf(
            createCookie("valid", "value1", "example.com", futureTime),
            createCookie("expired", "value2", "example.com", pastTime),
            createCookie("wrongDomain", "value3", "other.com", futureTime)
        )
        val cookieJar = PersistentCookieJar.create(mockDataStore, this)

        val result = cookieJar.loadForRequest(testUrl)

        result shouldHaveSize 1
        result.first().name shouldBe "valid"
    }

    @Test
    fun `loadForRequest does not include cookies from different root domain`() = runTest {
        val futureTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)
        coEvery { mockDataStore.loadAllCookies() } returns listOf(
            createCookie("sessionId", "123", "example.com", futureTime)
        )
        val cookieJar = PersistentCookieJar.create(mockDataStore, this)

        val result = cookieJar.loadForRequest("https://other.com/path".toHttpUrl())

        result.shouldBeEmpty()
    }

    @Test
    fun `loadForRequest deduplicates parent-domain cookie re-bucketed by saveFromResponse`() = runTest {
        // Cold start: .example.com cookie → "example.com" bucket.
        // saveFromResponse for api.example.com re-buckets the same cookie → "api.example.com" bucket.
        // loadForRequest must return exactly 1 cookie, not 2.
        val parentDomainCookie = Cookie.parse(
            "https://example.com/".toHttpUrl(),
            "auth=token123; Domain=.example.com; Path=/"
        ) ?: error("Failed to parse parent-domain cookie")
        coEvery { mockDataStore.loadAllCookies() } returns listOf(parentDomainCookie)
        val cookieJar = PersistentCookieJar.create(mockDataStore, this)

        cookieJar.saveFromResponse("https://api.example.com/path".toHttpUrl(), listOf(parentDomainCookie))

        val result = cookieJar.loadForRequest("https://api.example.com/path".toHttpUrl())
        result.count { it.name == "auth" } shouldBe 1
    }

    @Test
    fun `loadForRequest serves parent-domain cookie to subdomain request on cold start`() = runTest {
        // Cookie with Domain=.example.com (non-hostOnly) is keyed under "example.com" on cold start.
        // loadForRequest("https://api.example.com/") must still find it via cookie.matches().
        val parentDomainCookie = Cookie.parse(
            "https://example.com/".toHttpUrl(),
            "auth=token123; Domain=.example.com; Path=/"
        ) ?: error("Failed to parse parent-domain cookie")
        coEvery { mockDataStore.loadAllCookies() } returns listOf(parentDomainCookie)
        val cookieJar = PersistentCookieJar.create(mockDataStore, this)

        val result = cookieJar.loadForRequest("https://api.example.com/path".toHttpUrl())

        result.any { it.name == "auth" } shouldBe true
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

    private fun createCookie(
        name: String,
        value: String,
        domain: String,
        expiresAt: Long = Long.MIN_VALUE,
    ): Cookie = Cookie.Builder()
        .name(name)
        .value(value)
        .domain(domain)
        .path("/")
        .expiresAt(expiresAt)
        .build()
}
