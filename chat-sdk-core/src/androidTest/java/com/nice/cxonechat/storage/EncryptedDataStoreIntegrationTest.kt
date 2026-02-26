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
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class EncryptedDataStoreIntegrationTest {

    private lateinit var context: Context
    private lateinit var store: EncryptedDataStore

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        store = EncryptedDataStoreProvider.getInstance(context)
        runBlocking { store.clear() }
    }

    @Test
    fun encryptsAndDecryptsValuesCorrectly() = runBlocking {
        val key = "testKey"
        val value = "secretValue"
        store.putString(key, value)
        val result = store.getString(key).first()
        assertEquals(value, result)
    }

    @Test
    fun storesEncryptedData_notPlaintext() = runBlocking {
        val key = "testKey"
        val value = "plainText"
        store.putString(key, value)
        val prefs = store.dataStore.data.first()
        val encrypted = prefs[androidx.datastore.preferences.core.stringPreferencesKey(key)]
        assertNotNull(encrypted)
        assertNotEquals(value, encrypted)
    }

    @Test
    fun dataPersistsAcrossStoreInstances() = runBlocking {
        val key = "persistKey"
        val value = "persistentValue"
        store.putString(key, value)
        // Simulate app restart by creating a new instance
        val newStore = EncryptedDataStoreProvider.getInstance(context)
        val result = newStore.getString(key).first()
        assertEquals(value, result)
    }

    @Test
    fun dataSurvivesAppRestart() = runBlocking {
        val key = "persisted"
        val value = "persistentValue"
        store.putString(key, value)
        // Simulate app restart by creating a new instance
        val newStore = EncryptedDataStoreProvider.getInstance(context)
        val decrypted = newStore.getString(key).first()
        assertEquals(value, decrypted)
    }
}
