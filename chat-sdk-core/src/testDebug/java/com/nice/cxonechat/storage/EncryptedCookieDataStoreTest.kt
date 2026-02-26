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
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.TimeUnit

internal class EncryptedCookieDataStoreTest {
    private lateinit var context: Context
    private lateinit var dataStore: EncryptedCookieDataStore
    private lateinit var testDir: File

    @Rule
    @JvmField
    val tempFolder = TemporaryFolder()

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        testDir = tempFolder.newFolder()
        dataStore = createTestCookieDataStore(testDir)
    }

    @Test
    fun saveCookies_storesMultipleCookies() {
        runBlocking {
            val url = "https://example.com/".toHttpUrl()
            val cookies = listOf(
                createCookie("session", "123", "example.com"),
                createCookie("token", "abc", "example.com")
            )

            dataStore.saveCookies(url, cookies)
            val loaded = dataStore.loadCookies(url)

            loaded shouldHaveSize 2
        }
    }

    @Test
    fun loadCookies_returnsEmptyWhenNoCookiesStored() {
        runBlocking {
            val url = "https://example.com/".toHttpUrl()

            val loaded = dataStore.loadCookies(url)

            loaded.shouldBeEmpty()
        }
    }

    @Test
    fun loadCookies_filtersExpiredCookies() {
        runBlocking {
            val url = "https://example.com/".toHttpUrl()
            val pastTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
            val futureTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)

            val cookies = listOf(
                createCookie("expired", "value1", "example.com", pastTime),
                createCookie("valid", "value2", "example.com", futureTime)
            )

            dataStore.saveCookies(url, cookies)
            val loaded = dataStore.loadCookies(url)

            loaded shouldHaveSize 1
            loaded.first().name shouldBe "valid"
        }
    }

    @Test
    fun loadCookies_includesPersistentCookies() {
        runBlocking {
            val url = "https://example.com/".toHttpUrl()
            val persistentCookie = createCookie("persistent", "value", "example.com", Long.MIN_VALUE)
            val regularCookie = createCookie(
                "regular", "value2", "example.com",
                System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1)
            )

            dataStore.saveCookies(url, listOf(persistentCookie, regularCookie))
            val loaded = dataStore.loadCookies(url)

            loaded shouldHaveSize 2
        }
    }


    @Test
    fun saveCookies_handlesEmptyList() {
        runBlocking {
            val url = "https://example.com/".toHttpUrl()

            dataStore.saveCookies(url, emptyList())
            val loaded = dataStore.loadCookies(url)

            loaded.shouldBeEmpty()
        }
    }

    @Test
    fun loadCookies_filtersPathMatchingCookies() {
        runBlocking {
            val url = "https://example.com/api/".toHttpUrl()
            val cookies = listOf(
                createCookie("test", "value", "example.com", path = "/api/")
            )

            dataStore.saveCookies(url, cookies)
            val loaded = dataStore.loadCookies(url)

            loaded shouldHaveSize 1
        }
    }

    @Test
    fun loadCookies_currentTimeCalculationIsAccurate() {
        runBlocking {
            val url = "https://example.com/".toHttpUrl()
            val expiresAtTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(100)
            val cookie = createCookie("test", "value", "example.com", expiresAtTime)

            dataStore.saveCookies(url, listOf(cookie))
            val loaded = dataStore.loadCookies(url)

            loaded shouldHaveSize 1
        }
    }

    @Test
    fun saveCookies_withHttpUrl() {
        runBlocking {
            val url = "https://api.example.com/v1/endpoint".toHttpUrl()
            val cookies = listOf(
                createCookie("auth", "token123", "api.example.com")
            )

            dataStore.saveCookies(url, cookies)
            val loaded = dataStore.loadCookies(url)

            loaded shouldHaveSize 1
            loaded.first().name shouldBe "auth"
        }
    }

    private fun createCookie(
        name: String,
        value: String,
        domain: String,
        expiresAt: Long = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1),
        path: String = "/",
    ): Cookie = Cookie.Builder()
        .name(name)
        .value(value)
        .domain(domain)
        .path(path)
        .expiresAt(expiresAt)
        .build()
}
