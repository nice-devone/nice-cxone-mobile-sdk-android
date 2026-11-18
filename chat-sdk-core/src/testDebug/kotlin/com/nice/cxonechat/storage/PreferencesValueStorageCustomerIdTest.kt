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

import com.nice.cxonechat.internal.model.CustomerIdentityModel
import com.nice.cxonechat.internal.model.TransactionTokenModel
import com.nice.cxonechat.internal.serializer.Default
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
internal class PreferencesValueStorageCustomerIdTest {
    private lateinit var mockDataStore: EncryptedDataStoreContract

    @Before
    fun setUp() {
        mockDataStore = mockk(relaxed = true)
    }

    @Test
    fun `customerId auto-generates and persists when absent from store, like visitorId`() = runTest {
        val storage = PreferencesValueStorage.create(mockDataStore, this)
        val generatedId = storage.customerId
        assertTrue(generatedId != null, "A locally-generated customerId must be assigned when none is persisted")
        coVerify { mockDataStore.putString("share_customer_id", generatedId) }
    }

    @Test
    fun `customerId reads the persisted value instead of generating a new one`() = runTest {
        coEvery { mockDataStore.getString("share_customer_id") } returns flowOf("persisted-customer-id")
        val storage = PreferencesValueStorage.create(mockDataStore, this)
        storage.customerId shouldBe "persisted-customer-id"
    }

    @Test
    fun `customerId falls back to the cached transaction token's identity when its own key is absent`() = runTest {
        val cachedToken = TransactionTokenModel(
            transactionToken = "token",
            expiresIn = 3600,
            customerIdentity = CustomerIdentityModel(idOnExternalPlatform = "token-customer-id"),
        )
        coEvery { mockDataStore.getString("share_sdk_refresh_token") } returns
            flowOf(Default.serializer.encodeToString(cachedToken))
        val storage = PreferencesValueStorage.create(mockDataStore, this)
        storage.customerId shouldBe "token-customer-id"
        coVerify { mockDataStore.putString("share_customer_id", "token-customer-id") }
    }

    @Test
    fun `customerId prefers its own persisted key over the cached transaction token's identity`() = runTest {
        val cachedToken = TransactionTokenModel(
            transactionToken = "token",
            expiresIn = 3600,
            customerIdentity = CustomerIdentityModel(idOnExternalPlatform = "token-customer-id"),
        )
        coEvery { mockDataStore.getString("share_sdk_refresh_token") } returns
            flowOf(Default.serializer.encodeToString(cachedToken))
        coEvery { mockDataStore.getString("share_customer_id") } returns flowOf("persisted-customer-id")
        val storage = PreferencesValueStorage.create(mockDataStore, this)
        storage.customerId shouldBe "persisted-customer-id"
    }

    @Test
    fun `clearStorage replaces customerId with a fresh local one and persists it after the clear`() = runTest {
        coEvery { mockDataStore.getString("share_customer_id") } returns flowOf("persisted-customer-id")
        val storage = PreferencesValueStorage.create(mockDataStore, this)
        storage.clearStorage()
        val freshId = storage.customerId
        assertTrue(freshId != null, "A builder reused after signOut() must still have a customerId")
        freshId shouldNotBe "persisted-customer-id"
        coVerifyOrder {
            mockDataStore.clear()
            mockDataStore.putString("share_customer_id", freshId)
        }
    }
}
