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

import com.nice.cxonechat.log.LoggerNoop
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

internal class PersistentCookieJarTest {
    private lateinit var mockDataStore: EncryptedCookieDataStore
    private lateinit var cookieJar: PersistentCookieJar
    private lateinit var testUrl: HttpUrl

    @Before
    fun setUp() {
        mockDataStore = mockk(relaxed = true)
        cookieJar = PersistentCookieJar(
            context = mockk(relaxed = true),
            testCookieDataStore = mockDataStore,
            logger = LoggerNoop
        )
        testUrl = "https://example.com/path".toHttpUrl()
    }

    @Test
    fun saveFromResponse_passesUrlAndCookiesToDataStore() {
        val cookies = listOf(
            createCookie("sessionId", "abc123", "example.com"),
            createCookie("token", "xyz789", "example.com")
        )

        cookieJar.saveFromResponse(testUrl, cookies)

        coVerify { mockDataStore.saveCookies(testUrl, cookies) }
    }

    @Test
    fun saveFromResponse_withEmptyCookieList() {
        val cookies = emptyList<Cookie>()

        cookieJar.saveFromResponse(testUrl, cookies)

        coVerify { mockDataStore.saveCookies(testUrl, cookies) }
    }

    @Test
    fun loadForRequest_returnsFilteredNonExpiredCookies() {
        val futureTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)
        val cookies = listOf(
            createCookie("valid1", "value1", "example.com", futureTime),
            createCookie("valid2", "value2", "example.com", futureTime)
        )

        coEvery { mockDataStore.loadCookies(testUrl) } returns cookies

        val result = cookieJar.loadForRequest(testUrl)

        result shouldHaveSize 2
    }

    @Test
    fun loadForRequest_filtersExpiredCookies() {
        val pastTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
        val futureTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)
        val cookies = listOf(
            createCookie("expired", "value1", "example.com", pastTime),
            createCookie("valid", "value2", "example.com", futureTime)
        )

        coEvery { mockDataStore.loadCookies(testUrl) } returns cookies

        val result = cookieJar.loadForRequest(testUrl)

        result shouldHaveSize 1
        result.first().name shouldBe "valid"
    }

    @Test
    fun loadForRequest_returnsEmptyWhenAllCookiesExpired() {
        val pastTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)
        val cookies = listOf(
            createCookie("expired1", "value1", "example.com", pastTime),
            createCookie("expired2", "value2", "example.com", pastTime)
        )

        coEvery { mockDataStore.loadCookies(testUrl) } returns cookies

        val result = cookieJar.loadForRequest(testUrl)

        result.shouldBeEmpty()
    }

    @Test
    fun loadForRequest_filtersNonMatchingDomains() {
        val futureTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)
        val exampleCookie = createCookie("id", "123", "example.com", futureTime)
        val otherCookie = createCookie("id", "456", "other.com", futureTime)

        coEvery { mockDataStore.loadCookies(testUrl) } returns listOf(exampleCookie, otherCookie)

        val result = cookieJar.loadForRequest(testUrl)

        result.any { it.domain == "example.com" } shouldBe true
        result.any { it.domain == "other.com" } shouldBe false
    }

    @Test
    fun clearAllCookies_callsDataStoreClear() {
        cookieJar.clearAllCookies()

        coVerify { mockDataStore.clear() }
    }

    @Test
    fun saveFromResponse_doesNotFilterCookies() {
        val cookies = listOf(
            createCookie("cookie1", "val1", "example.com"),
            createCookie("cookie2", "val2", "example.com")
        )

        cookieJar.saveFromResponse(testUrl, cookies)

        coVerify { mockDataStore.saveCookies(testUrl, cookies) }
    }

    @Test
    fun loadForRequest_currentTimeIsCalculatedCorrectly() {
        val futureTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)
        val cookies = listOf(
            createCookie("test", "value", "example.com", futureTime)
        )

        coEvery { mockDataStore.loadCookies(testUrl) } returns cookies

        val result = cookieJar.loadForRequest(testUrl)

        result shouldHaveSize 1
    }

    @Test
    fun clearAllCookies_handlesAsyncClear() {
        cookieJar.clearAllCookies()

        coVerify { mockDataStore.clear() }
    }

    @Test
    fun loadForRequest_doesNotIncludeCookiesFromDifferentRootDomain() {
        val futureTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)
        val exampleCookie = createCookie("sessionId", "123", "example.com", futureTime)
        val otherDomainUrl = "https://other.com/path".toHttpUrl()

        coEvery { mockDataStore.loadCookies(otherDomainUrl) } returns listOf(exampleCookie)

        val result = cookieJar.loadForRequest(otherDomainUrl)

        // Cookie from different domain should not be included
        result.shouldBeEmpty()
    }

    @Test
    fun loadForRequest_handlesAuthUrlVsChatUrlVsSocketUrl() {
        val futureTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)
        // Simulate cookies from different endpoints in the same domain
        val authCookie = createCookie("authToken", "auth123", "api.example.com", futureTime)
        val chatCookie = createCookie("chatSession", "chat456", "api.example.com", futureTime)
        val socketCookie = createCookie("socketToken", "socket789", "api.example.com", futureTime)

        val allCookies = listOf(authCookie, chatCookie, socketCookie)

        // Auth URL
        val authUrl = "https://api.example.com/oauth/token".toHttpUrl()
        coEvery { mockDataStore.loadCookies(authUrl) } returns allCookies
        val authResult = cookieJar.loadForRequest(authUrl)
        authResult shouldHaveSize 3

        // Chat URL - should get the same cookies if domain matches
        val chatUrl = "https://api.example.com/chat/messages".toHttpUrl()
        coEvery { mockDataStore.loadCookies(chatUrl) } returns allCookies
        val chatResult = cookieJar.loadForRequest(chatUrl)
        chatResult shouldHaveSize 3

        // Socket URL - should also get cookies
        val socketUrl = "https://api.example.com/socket/connect".toHttpUrl()
        coEvery { mockDataStore.loadCookies(socketUrl) } returns allCookies
        val socketResult = cookieJar.loadForRequest(socketUrl)
        socketResult shouldHaveSize 3
    }

    @Test
    fun loadForRequest_doesNotDuplicateFilteringLogic() {
        // This test documents that loadForRequest applies both
        // expiration AND domain matching filters
        val futureTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)
        val pastTime = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(1)

        val validCookie = createCookie("valid", "value1", "example.com", futureTime)
        val expiredCookie = createCookie("expired", "value2", "example.com", pastTime)
        val wrongDomainCookie = createCookie("wrong", "value3", "other.com", futureTime)

        coEvery { mockDataStore.loadCookies(testUrl) } returns listOf(
            validCookie,
            expiredCookie,
            wrongDomainCookie
        )

        val result = cookieJar.loadForRequest(testUrl)

        // Only the valid cookie should be returned
        result shouldHaveSize 1
        result.first().name shouldBe "valid"
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
