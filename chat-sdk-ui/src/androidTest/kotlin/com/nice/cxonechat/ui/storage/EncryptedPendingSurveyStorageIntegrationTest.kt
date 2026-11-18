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

package com.nice.cxonechat.ui.storage

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

/**
 * Exercises the real Android Keystore-backed encryption - unlike [EncryptedPendingSurveyStorage]'s
 * unit test coverage (which always goes through a mocked [ValueStorage]), this is the only coverage
 * that a real device/emulator's Keystore round-trips correctly. Mirrors chat-sdk-core's
 * `EncryptedDataStoreIntegrationTest`.
 */
internal class EncryptedPendingSurveyStorageIntegrationTest {

    private lateinit var storage: EncryptedPendingSurveyStorage

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        storage = EncryptedPendingSurveyStorage(context)
        runBlocking { storage.clear() }
    }

    @Test
    fun encryptsAndDecryptsTheStoredValueCorrectly() = runBlocking {
        storage.update { "secret-survey-payload" }

        var observed: String? = null
        storage.update { current ->
            observed = current
            current
        }

        assertEquals("secret-survey-payload", observed)
    }

    @Test
    fun storesEncryptedData_notPlaintext() = runBlocking {
        storage.update { "plain-text-survey-payload" }

        val onDisk = storage.dataStore.data.first()[storage.valueKey]

        assertNotNull(onDisk)
        assertNotEquals("plain-text-survey-payload", onDisk)
    }

    @Test
    fun dataPersistsAcrossStorageInstances() = runBlocking {
        storage.update { "persisted-survey-payload" }

        // Simulates a process restart - a fresh instance must retrieve the same Keystore key, not
        // generate a new one, or the value above would no longer decrypt.
        val context = ApplicationProvider.getApplicationContext<Context>()
        val newStorage = EncryptedPendingSurveyStorage(context)
        var observed: String? = null
        newStorage.update { current ->
            observed = current
            current
        }

        assertEquals("persisted-survey-payload", observed)
    }
}
