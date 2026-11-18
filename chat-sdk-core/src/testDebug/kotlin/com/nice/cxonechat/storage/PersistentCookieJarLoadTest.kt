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

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.time.Clock

/**
 * Read-side filtering of [PersistentCookieJar.loadForRequest]: expiration filtering, domain
 * matching, and parent-domain cookie bucketing. Write-side behavior lives in
 * [PersistentCookieJarTest]; cache/pending-write semantics live in [PersistentCookieJarCacheTest].
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class PersistentCookieJarLoadTest : AbstractPersistentCookieJarTest() {

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
}
