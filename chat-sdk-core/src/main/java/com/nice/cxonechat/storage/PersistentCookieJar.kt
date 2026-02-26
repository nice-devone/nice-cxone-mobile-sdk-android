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
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerNoop
import kotlinx.coroutines.runBlocking
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

internal class PersistentCookieJar(
    context: Context,
    testCookieDataStore: EncryptedCookieDataStore? = null,
    logger: Logger = LoggerNoop,
) : CookieJar {
    private val cookieDataStore = testCookieDataStore ?: runBlocking { EncryptedCookieDataStore.create(context, logger) }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        runBlocking {
            cookieDataStore.saveCookies(url, cookies)
        }
    }

    /**
     * Loads cookies for the given URL, filtering by expiration time and domain matching.
     *
     * **Important:** The [EncryptedCookieDataStore.loadCookies] call already filters by domain matching,
     * but we apply the [Cookie.matches] check again here to ensure expiration filtering is also applied.
     * This dual filtering is intentional: it provides consistent expiration validation at the retrieval layer.
     *
     * @param url The URL for which to load cookies
     * @return A list of valid (non-expired) cookies that match the given URL's domain and path
     */
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        val allCookies = runBlocking {
            cookieDataStore.loadCookies(url)
        }
        return allCookies.filter {
            it.expiresAt > now && it.matches(url)
        }
    }

    fun clearAllCookies() {
        runBlocking {
            cookieDataStore.clear()
        }
    }
}
