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

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Before

/**
 * Shared fixtures for the [PersistentCookieJar] test classes: the mocked data store, the request
 * URL, the [Before] setup, and the [createCookie] builder used across the delegation, load,
 * and cache scenarios.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal abstract class AbstractPersistentCookieJarTest {
    protected lateinit var mockDataStore: EncryptedCookieDataStore
    protected val testUrl: HttpUrl = "https://example.com/path".toHttpUrl()

    @Before
    fun setUp() {
        mockDataStore = mockk(relaxed = true) {
            coEvery { loadAllCookies() } returns emptyList()
        }
    }

    protected fun createCookie(
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
