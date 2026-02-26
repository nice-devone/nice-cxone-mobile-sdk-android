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
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import com.nice.cxonechat.internal.serializer.Default
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.UUID

class PreferencesValueStorageMigrationTest {
    private lateinit var context: Context
    private lateinit var storage: PreferencesValueStorage
    private lateinit var dataStore: DataStore<Preferences>

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val file = File(context.filesDir, "test_${System.nanoTime()}.preferences_pb")
        if (file.exists()) file.delete()
        dataStore = PreferenceDataStoreFactory.create { file }
        val encryptedStore = FakeEncryptedDataStore(dataStore)
        storage = PreferencesValueStorage(context = context, store = encryptedStore)
    }

    @Test
    fun authToken_stores_and_reads_correctly() = runTest {
        storage.authToken = "TOKEN"
        val token = storage.authToken
        assertEquals("TOKEN", token)
        val dsValue = dataStore.data.first()[stringPreferencesKey("share_sdk_auth_token")]
        assertEquals("TOKEN", dsValue)
    }

    @Test
    fun customerId_stores_and_reads_correctly() = runTest {
        storage.customerId = "CUST123"
        val result = storage.customerId
        assertEquals("CUST123", result)
        val dsValue = dataStore.data.first()[stringPreferencesKey("share_customer_id")]
        assertEquals("CUST123", dsValue)
    }

    @Test
    fun welcomeMessage_uses_empty_string_when_missing() = runTest {
        val msg = storage.welcomeMessage
        assertEquals("", msg)
    }

    @Test
    fun authTokenExpDate_stores_and_reads_as_Date() = runTest {
        val now = System.currentTimeMillis()
        storage.authTokenExpDate = java.util.Date(now)
        val date = storage.authTokenExpDate
        assertEquals(now, date?.time)
    }

    @Test
    fun visitorId_stores_and_reads_correctly() = runTest {
        val id = UUID.randomUUID()
        storage.visitorId = id
        val result = storage.visitorId
        assertEquals(id, result)
        val dsValue = dataStore.data.first()[stringPreferencesKey("share_visitor_id")]
        assertEquals(id.toString(), dsValue)
    }

    @Test
    fun visitorId_auto_creates_when_missing() = runTest {
        val id = storage.visitorId
        val stored = dataStore.data.first()[stringPreferencesKey("share_visitor_id")]
        assertEquals(id.toString(), stored)
    }

    @Test
    fun visitDetails_stores_and_decodes_JSON() = runTest {
        val details = ValueStorage.VisitDetails()
        storage.visitDetails = details
        val result = storage.visitDetails
        assertEquals(details.visitId, result?.visitId)
    }

    @Test
    fun visitId_auto_creates_when_missing() = runTest {
        val id = storage.visitId
        val storedJson = dataStore.data.first()[stringPreferencesKey("share_visit_details")]!!
        val stored = Default.serializer.decodeFromString<ValueStorage.VisitDetails>(storedJson)
        assertEquals(id, stored.visitId)
    }

    @Test
    fun deviceToken_stores_and_reads_correctly() = runTest {
        storage.deviceToken = "DEVICE123"
        val token = storage.deviceToken
        assertEquals("DEVICE123", token)
    }

    @Test
    fun clearStorage_clears_datastore() = runTest {
        storage.authToken = "X"
        storage.deviceToken = "Y"
        storage.clearStorage()
        Assert.assertTrue(dataStore.data.first().asMap().isEmpty())
    }
}
