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

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.Cookie
import java.io.File
import java.util.UUID

// Store the scope reference for cleanup
private val dataStoreScopes = mutableMapOf<EncryptedCookieDataStore, CoroutineScope>()

/**
 * Creates a test EncryptedCookieDataStore using the provided temporary directory.
 *
 * Each test gets its own isolated DataStore instance with a unique file name to prevent conflicts.
 *
 * @param tempDir The temporary directory to use for the DataStore file. Should be provided by the test using @TempDir (JUnit 5) or TemporaryFolder (JUnit 4).
 */
internal fun createTestCookieDataStore(tempDir: File): EncryptedCookieDataStore {
    val testFile = File(tempDir, "cookies-${UUID.randomUUID()}.pb")

    // This ensures updateData() and data.first() don't access the file concurrently
    val singleThreadDispatcher = Dispatchers.IO.limitedParallelism(1)
    val scope = CoroutineScope(singleThreadDispatcher + SupervisorJob())

    val ds: DataStore<List<Cookie>> = DataStoreFactory.create(
        serializer = PlainCookieListSerializer(),
        produceFile = { testFile },
        corruptionHandler = null,
        scope = scope
    )

    return EncryptedCookieDataStore.createForTest(ds).also { dataStore ->
        dataStoreScopes[dataStore] = scope
    }
}
