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
import com.nice.cxonechat.internal.model.TransactionTokenModel
import com.nice.cxonechat.internal.serializer.Default
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.UUID
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class PreferencesValueStorageMigrationTest {
    private lateinit var context: Context
    private lateinit var encryptedStore: FakeEncryptedDataStore
    private lateinit var dataStore: DataStore<Preferences>

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val file = File(context.filesDir, "test_${System.nanoTime()}.preferences_pb")
        if (file.exists()) file.delete()
        dataStore = PreferenceDataStoreFactory.create { file }
        encryptedStore = FakeEncryptedDataStore(dataStore)
    }

    @Test
    fun authTokenStoresInMemoryAndPersistsToDataStore() = runTest {
        val storage = PreferencesValueStorage.create(encryptedStore, backgroundScope)
        storage.authToken = "TOKEN"
        assertEquals("TOKEN", storage.authToken)
        advanceUntilIdle()
        val prefs = dataStore.data.first { it[stringPreferencesKey("share_sdk_auth_token")] == "TOKEN" }
        val dsValue = prefs[stringPreferencesKey("share_sdk_auth_token")]
        assertEquals("TOKEN", dsValue)
    }

    @Test
    fun customerIdStoresInMemoryAndPersistsToDataStore() = runTest {
        val storage = PreferencesValueStorage.create(encryptedStore, backgroundScope)
        storage.customerId = "CUST123"
        assertEquals("CUST123", storage.customerId)
        advanceUntilIdle()
        val custPrefs = dataStore.data.first { it[stringPreferencesKey("share_customer_id")] == "CUST123" }
        val dsValue = custPrefs[stringPreferencesKey("share_customer_id")]
        assertEquals("CUST123", dsValue)
    }

    @Test
    fun welcomeMessageDefaultsToEmptyStringWhenAbsentFromStore() = runTest {
        val storage = PreferencesValueStorage.create(encryptedStore, backgroundScope)
        assertEquals("", storage.welcomeMessage)
    }

    @Test
    fun authTokenExpDate_stores_and_reads_as_Instant() = runTest {
        val now = System.currentTimeMillis()
        val storage = PreferencesValueStorage.create(encryptedStore, backgroundScope)
        storage.authTokenExpDate = Instant.fromEpochMilliseconds(now)
        assertEquals(now, storage.authTokenExpDate?.toEpochMilliseconds())
    }

    @Test
    fun visitorIdReadsPersistedValueFromStore() = runTest {
        val knownId = UUID.randomUUID()
        encryptedStore.putString("share_visitor_id", knownId.toString())
        val storage = PreferencesValueStorage.create(encryptedStore, backgroundScope)
        assertEquals(knownId, storage.visitorId)
    }

    @Test
    fun visitorIdAutoGeneratesAndPersistsWhenAbsentFromStore() = runTest {
        val storage = PreferencesValueStorage.create(encryptedStore, backgroundScope)
        val id = storage.visitorId
        val stored = dataStore.data.first()[stringPreferencesKey("share_visitor_id")]
        assertEquals(id.toString(), stored)
    }

    @Test
    fun visitDetailsStoresInMemoryAndDecodesFromJson() = runTest {
        val details = ValueStorage.VisitDetails()
        val storage = PreferencesValueStorage.create(encryptedStore, backgroundScope)
        storage.visitDetails = details
        assertEquals(details.visitId, storage.visitDetails?.visitId)
    }

    @Test
    fun visitIdAutoCreatesAndPersistsWhenAbsent() = runTest {
        val storage = PreferencesValueStorage.create(encryptedStore, backgroundScope)
        val id = storage.visitId
        advanceUntilIdle()
        val visitPrefs = dataStore.data.first { it[stringPreferencesKey("share_visit_details")] != null }
        val storedJson = visitPrefs[stringPreferencesKey("share_visit_details")]!!
        val stored = Default.serializer.decodeFromString<ValueStorage.VisitDetails>(storedJson)
        assertEquals(id, stored.visitId)
    }

    @Test
    fun deviceTokenStoresInMemoryAndReadsBackCorrectly() = runTest {
        val storage = PreferencesValueStorage.create(encryptedStore, backgroundScope)
        storage.deviceToken = "DEVICE123"
        assertEquals("DEVICE123", storage.deviceToken)
    }

    @Test
    fun clearStorageClearsDataStoreAfterAsyncFlush() = runTest {
        val storage = PreferencesValueStorage.create(encryptedStore, backgroundScope)
        storage.authToken = "X"
        storage.deviceToken = "Y"
        advanceUntilIdle()
        storage.clearStorage()
        // DataStore's internal actor runs on real Dispatchers.IO — advanceUntilIdle() only drains
        // the test scheduler and cannot guarantee the store.clear() has landed. Wait reactively for
        // a DataStore emission where the cleared keys are absent instead.
        // clearStorage() re-persists share_visitor_id after clear, so the store is never empty.
        val prefs = dataStore.data.first {
            it[stringPreferencesKey("share_sdk_auth_token")] == null &&
                    it[stringPreferencesKey("device_token")] == null
        }
        Assert.assertNull(prefs[stringPreferencesKey("share_sdk_auth_token")])
        Assert.assertNull(prefs[stringPreferencesKey("device_token")])
    }

    @Test
    fun readsTransactionTokenModelWrittenAsJsonByPreviousImplementation() = runTest {
        val model = TransactionTokenModel(
            transactionToken = "legacy-transaction-token",
            expiresIn = 3600L,
            customerIdentity = null,
            thirdParty = null,
            createdAt = Instant.fromEpochMilliseconds(0),
        )
        encryptedStore.putString("share_sdk_refresh_token", Default.serializer.encodeToString(model))
        val storage = PreferencesValueStorage.create(encryptedStore, backgroundScope)
        assertEquals(model.transactionToken, storage.transactionTokenModel?.transactionToken)
    }

    @Test
    fun returnsNullTransactionTokenModelWhenStoredJsonIsCorrupt() = runTest {
        encryptedStore.putString("share_sdk_refresh_token", "not-valid-json{{{")
        val storage = PreferencesValueStorage.create(encryptedStore, backgroundScope)
        assertEquals(null, storage.transactionTokenModel)
        // create() must remove the corrupt key so repeated reconnects don't re-log the warning
        Assert.assertNull(dataStore.data.first()[stringPreferencesKey("share_sdk_refresh_token")])
    }

    @Test
    fun returnsNullVisitDetailsWhenStoredJsonIsCorrupt() = runTest {
        encryptedStore.putString("share_visit_details", "corrupted-data")
        val storage = PreferencesValueStorage.create(encryptedStore, backgroundScope)
        assertEquals(null, storage.visitDetails)
        // create() must remove the corrupt key so repeated reconnects don't re-log the warning
        Assert.assertNull(dataStore.data.first()[stringPreferencesKey("share_visit_details")])
    }

    @Test
    fun clearStorageResetsVisitorIdAndSecondSessionReadsFreshUuid() = runTest {
        val scope1 = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
        val storage1 = PreferencesValueStorage.create(encryptedStore, scope1)
        val originalId = storage1.visitorId
        storage1.clearStorage()
        val freshId = storage1.visitorId

        // In-memory: visitorId changed to a new UUID immediately
        assertNotEquals(originalId, freshId)

        // Drain and cancel (simulate close())
        scope1.coroutineContext[Job]?.children?.forEach { it.join() }
        scope1.cancel()

        // Second session: should read the same fresh UUID, not generate yet another one
        val scope2 = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
        val storage2 = PreferencesValueStorage.create(encryptedStore, scope2)
        assertEquals(freshId, storage2.visitorId)
        scope2.cancel()
    }

    /**
     * Lifecycle scenario: connect → close → reconnect.
     *
     * Values written during session 1 must survive the scope drain-and-cancel that ChatImpl.close()
     * performs, so that a second create() call (simulating reconnect) reads them from the DataStore.
     *
     * The drain pattern intentionally mirrors ChatImpl.close():
     *   join all active children of storageWriteScope → cancel scope → create new session.
     */
    @Test
    fun connectCloseReconnect_valuesWrittenInSession1AreVisibleInSession2() = runTest {
        // Session 1 — connect
        val scope1 = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
        val storage1 = PreferencesValueStorage.create(encryptedStore, scope1)
        val visitorId1 = storage1.visitorId // triggers auto-generation + async persist
        storage1.authToken = "session1-token"
        storage1.customerId = "cust-001"
        storage1.welcomeMessage = "Hello"

        // Drain — simulates ChatImpl.close() joining storageWriteScope's children
        scope1.coroutineContext[Job]?.children?.forEach { it.join() }
        scope1.cancel()

        // Session 2 — reconnect (new create() call against the same DataStore)
        val scope2 = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
        val storage2 = PreferencesValueStorage.create(encryptedStore, scope2)

        assertEquals("session1-token", storage2.authToken)
        assertEquals("cust-001", storage2.customerId)
        assertEquals("Hello", storage2.welcomeMessage)
        assertEquals(visitorId1, storage2.visitorId)

        scope2.cancel()
    }
}
