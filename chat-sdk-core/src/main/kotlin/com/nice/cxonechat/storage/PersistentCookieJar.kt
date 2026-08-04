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
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.error
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Clock

internal class PersistentCookieJar private constructor(
    private val cookieDataStore: EncryptedCookieDataStore,
    private val writeScope: CoroutineScope,
    initialCookies: Map<String, List<Cookie>>,
    logger: Logger,
) : CookieJar, LoggerScope by LoggerScope("PersistentCookieJar", logger) {

    // Host → cookies. ConcurrentHashMap.compute() provides per-key atomic merge.
    private val cookieCache = ConcurrentHashMap(initialCookies)

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        // Remove same-identity cookies from other buckets before writing to the canonical bucket.
        // This prevents stale cross-bucket duplicates when a domain cookie is re-bucketed under a
        // subdomain host key (e.g. cold-start key "example.com" vs runtime key "api.example.com").
        val incomingIdentities = cookies.mapTo(HashSet()) { Triple(it.name, it.domain, it.path) }
        cookieCache.keys.filter { it != url.host }.forEach { bucket ->
            cookieCache.computeIfPresent(bucket) { _, list ->
                list.filterNot { Triple(it.name, it.domain, it.path) in incomingIdentities }
                    .takeIf { it.isNotEmpty() }
            }
        }
        cookieCache.compute(url.host) { _, existing ->
            val now = System.currentTimeMillis()
            val base = (existing ?: emptyList()).filter { it.isValidAt(now) }
            base.filterNot { stored ->
                cookies.any { new -> stored.name == new.name && stored.matches(url) }
            } + cookies
        }
        writeScope.launch {
            try {
                cookieDataStore.saveCookies(url, cookies)
            } catch (expectedCancellation: CancellationException) {
                throw expectedCancellation
            } catch (expected: Exception) {
                error("Failed to persist cookies for ${url.host}", expected)
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = Clock.System.now().toEpochMilliseconds()
        return cookieCache.values.flatten()
            .filter { cookie -> cookie.isValidAt(now) && cookie.matches(url) }
    }

    internal suspend fun awaitPendingWrites() {
        val thisJob = currentCoroutineContext()[Job]
        writeScope.coroutineContext[Job]?.children
            // Excludes self when caller is a child of writeScope (test-only); no-op in production where writeScope is process-scoped.
            ?.filter { it !== thisJob }
            ?.toList()
            ?.joinAll()
    }

    suspend fun clearAllCookies() {
        cookieCache.clear()
        // Queued on writeScope, like saveFromResponse()'s persisted write, so an in-flight save's
        // *persisted* write can't land after this persisted clear; joined so callers can rely on
        // completion. This does NOT protect cookieCache itself: saveFromResponse() mutates it
        // synchronously on whatever thread OkHttp calls it from, independent of writeScope, so a
        // response that OkHttp delivers concurrently with (or just after) this clear can still
        // repopulate cookieCache in memory even though the persisted store ends up cleared. Closing
        // that gap needs request cancellation or a generation-tagged cache, not just write-ordering
        // -- known limitation, not addressed here.
        writeScope.launch {
            try {
                cookieDataStore.clear()
            } catch (expectedCancellation: CancellationException) {
                throw expectedCancellation
            } catch (expected: Exception) {
                error("Failed to clear cookie storage", expected)
            }
        }.join()
    }

    companion object {
        internal suspend fun create(
            cookieDataStore: EncryptedCookieDataStore,
            writeScope: CoroutineScope,
            logger: Logger = LoggerNoop,
        ): PersistentCookieJar {
            val allCookies = cookieDataStore.loadAllCookies().groupBy { cookie ->
                cookie.domain.trimStart('.')
            }
            return PersistentCookieJar(cookieDataStore, writeScope, allCookies, logger)
        }

        suspend fun create(
            context: Context,
            writeScope: CoroutineScope,
            logger: Logger = LoggerNoop,
        ): PersistentCookieJar {
            val store = EncryptedCookieDataStore.create(context, logger)
            return create(store, writeScope, logger)
        }

        // A cookie is valid while its expiry is still in the future. OkHttp uses MAX_DATE for
        // session/non-expiring cookies (so they pass this check) and Long.MIN_VALUE as the sentinel
        // for a deletion cookie (Max-Age<=0 / past Expires) — MIN_VALUE must therefore be treated as
        // expired, not valid, so server-sent deletions actually drop the cookie instead of resurrecting it.
        private fun Cookie.isValidAt(at: Long) = expiresAt > at
    }
}
