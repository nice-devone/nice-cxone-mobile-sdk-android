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
import androidx.annotation.VisibleForTesting
import com.nice.cxonechat.log.Level
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerNoop
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal object CookieJarProvider {
    private val mutex = Mutex()

    // Created lazily on first getInstance() call so the logger can be wired into the handler.
    // App-scoped: never cancelled — CookieJarProvider lifetime = process lifetime.
    @Volatile
    private var writeScope: CoroutineScope? = null

    @Volatile
    private var instance: PersistentCookieJar? = null

    suspend fun getInstance(
        context: Context,
        testCookieDataStore: EncryptedCookieDataStore? = null,
        logger: Logger = LoggerNoop,
    ): PersistentCookieJar = instance ?: mutex.withLock {
        instance ?: run {
            val scope = writeScope ?: CoroutineScope(
                SupervisorJob() +
                    Dispatchers.IO.limitedParallelism(1) +
                    CoroutineExceptionHandler { _, throwable ->
                        logger.log(Level.Error, "Cookie storage write failed", throwable)
                    }
            ).also { writeScope = it }
            (
                if (testCookieDataStore != null) {
                    PersistentCookieJar.create(testCookieDataStore, scope, logger)
                } else {
                    PersistentCookieJar.create(context.applicationContext, scope, logger)
                }
            ).also { instance = it }
        }
    }

    // Test-only. Uses synchronized() rather than the coroutine Mutex because unit tests run
    // sequentially (JUnit 4 default). Do NOT call from production code — there is no guarantee
    // this will not race with an in-progress getInstance() under concurrent execution.
    @VisibleForTesting
    fun clearInstance() {
        synchronized(this) {
            instance = null
            writeScope?.cancel()
            writeScope = null
        }
    }
}
