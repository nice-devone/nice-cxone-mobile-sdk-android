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
import com.nice.cxonechat.internal.model.TransactionTokenModel
import com.nice.cxonechat.log.LoggerNoop
import com.nice.cxonechat.storage.ValueStorage.VisitDetails
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Test
import java.util.Date
import java.util.UUID

internal class PreferencesValueStorageTest {
    private lateinit var mockDataStore: EncryptedDataStoreContract
    private lateinit var storage: PreferencesValueStorage

    @Before
    fun setUp() {
        mockDataStore = mockk(relaxed = true)
        val mockContext: Context = mockk(relaxed = true)
        storage = PreferencesValueStorage(
            context = mockContext,
            logger = LoggerNoop,
            store = mockDataStore
        )
    }

    @Test
    fun authToken_callsDataStoreForGet() {
        coEvery { mockDataStore.getString("share_sdk_auth_token") } returns flowOf("test-token")

        val result = storage.authToken

        // Verify the result is correct
        result shouldBe "test-token"
    }

    @Test
    fun authToken_callsDataStoreForSet() {
        val token = "test-auth-token-12345"

        storage.authToken = token

        coVerify { mockDataStore.putString("share_sdk_auth_token", token) }
    }

    @Test
    fun transactionTokenModel_callsDataStoreForSet() {
        val tokenModel = TransactionTokenModel(
            transactionToken = "token-123",
            expiresIn = 3600,
            customerIdentity = null,
            thirdParty = null,
            createdAt = Date()
        )

        storage.transactionTokenModel = tokenModel

        coVerify(atLeast = 1) { mockDataStore.putString(key = "share_sdk_refresh_token", value = any()) }
    }

    @Test
    fun authTokenExpDate_callsDataStoreForSet() {
        val expDate = Date(1000000)

        storage.authTokenExpDate = expDate

        coVerify { mockDataStore.putString("share_sdk_auth_token_exp_date", "1000000") }
    }

    @Test
    fun authTokenExpDate_callsDataStoreRemoveWhenNull() {
        storage.authTokenExpDate = null

        coVerify { mockDataStore.remove("share_sdk_auth_token_exp_date") }
    }

    @Test
    fun customerId_callsDataStoreForSet() {
        val customerId = "customer-123-xyz"

        storage.customerId = customerId

        coVerify { mockDataStore.putString("share_customer_id", customerId) }
    }

    @Test
    fun customerId_callsDataStoreRemoveWhenNull() {
        storage.customerId = null

        coVerify { mockDataStore.remove("share_customer_id") }
    }

    @Test
    fun visitorId_callsDataStoreForSet() {
        val visitorId = UUID.randomUUID()

        storage.visitorId = visitorId

        coVerify { mockDataStore.putString("share_visitor_id", visitorId.toString()) }
    }

    @Test
    fun deviceToken_callsDataStoreForSet() {
        val token = "device-token-firebase-123"

        storage.deviceToken = token

        coVerify { mockDataStore.putString("device_token", token) }
    }

    @Test
    fun welcomeMessage_callsDataStoreForSet() {
        val message = "Welcome to our chat service!"

        storage.welcomeMessage = message

        coVerify { mockDataStore.putString("share_welcome_message", message) }
    }

    @Test
    fun clearStorage_callsDataStoreClear() {
        storage.clearStorage()

        coVerify { mockDataStore.clear() }
    }

    @Test
    fun destinationId_returnsConsistentValue() {
        val firstId = storage.destinationId
        val secondId = storage.destinationId

        firstId shouldBe secondId
    }

    @Test
    fun visitDetails_callsDataStoreForSet() {
        val visitDetails = VisitDetails(
            visitId = UUID.randomUUID(),
            validUntil = Date()
        )

        storage.visitDetails = visitDetails

        coVerify(atLeast = 1) { mockDataStore.putString(key = "share_visit_details", value = any()) }
    }

    @Test
    fun visitDetails_callsDataStoreRemoveWhenNull() {
        storage.visitDetails = null

        coVerify { mockDataStore.remove("share_visit_details") }
    }
}
