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

import com.nice.cxonechat.internal.model.TransactionTokenModel
import com.nice.cxonechat.storage.ValueStorage.VisitDetails
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
internal class PreferencesValueStorageTest {
    private lateinit var mockDataStore: EncryptedDataStoreContract

    @Before
    fun setUp() {
        mockDataStore = mockk(relaxed = true)
    }

    @Test
    fun `authToken reads from store on creation`() = runTest {
        coEvery { mockDataStore.getString("share_sdk_auth_token") } returns flowOf("test-token")
        val storage = PreferencesValueStorage.create(mockDataStore, this)
        storage.authToken shouldBe "test-token"
    }

    @Test
    fun `authToken write calls store putString`() = runTest {
        val storage = PreferencesValueStorage.create(mockDataStore, this)
        storage.authToken = "test-auth-token-12345"
        advanceUntilIdle()
        coVerify { mockDataStore.putString("share_sdk_auth_token", "test-auth-token-12345") }
    }

    @Test
    fun `authToken read after write returns backing field value without waiting for persistence`() = runTest {
        val storage = PreferencesValueStorage.create(mockDataStore, this)
        storage.authToken = "immediate-value"
        // Read before advanceUntilIdle() — backing field update is synchronous
        storage.authToken shouldBe "immediate-value"
    }

    @Test
    fun `transactionTokenModel write calls store putString`() = runTest {
        val tokenModel = TransactionTokenModel(
            transactionToken = "token-123",
            expiresIn = 3600,
            customerIdentity = null,
            thirdParty = null,
            createdAt = Clock.System.now()
        )
        val storage = PreferencesValueStorage.create(mockDataStore, this)
        storage.transactionTokenModel = tokenModel
        advanceUntilIdle()
        coVerify(atLeast = 1) { mockDataStore.putString(key = "share_sdk_refresh_token", value = any()) }
    }

    @Test
    fun `authTokenExpDate write calls store putString`() = runTest {
        val expDate = Instant.fromEpochMilliseconds(1000000)
        val storage = PreferencesValueStorage.create(mockDataStore, this)
        storage.authTokenExpDate = expDate
        advanceUntilIdle()
        coVerify { mockDataStore.putString("share_sdk_auth_token_exp_date", "1000000") }
    }

    @Test
    fun `authTokenExpDate write null calls store remove`() = runTest {
        val storage = PreferencesValueStorage.create(mockDataStore, this)
        storage.authTokenExpDate = null
        advanceUntilIdle()
        coVerify { mockDataStore.remove("share_sdk_auth_token_exp_date") }
    }

    @Test
    fun `customerId write calls store putString`() = runTest {
        val customerId = "customer-123-xyz"
        val storage = PreferencesValueStorage.create(mockDataStore, this)
        storage.customerId = customerId
        advanceUntilIdle()
        coVerify { mockDataStore.putString("share_customer_id", customerId) }
    }

    @Test
    fun `customerId write null calls store remove`() = runTest {
        val storage = PreferencesValueStorage.create(mockDataStore, this)
        storage.customerId = null
        advanceUntilIdle()
        coVerify { mockDataStore.remove("share_customer_id") }
    }

    @Test
    fun `visitorId auto-generates and persists when absent from store`() = runTest {
        val storage = PreferencesValueStorage.create(mockDataStore, this)
        val generatedId = storage.visitorId
        coVerify { mockDataStore.putString("share_visitor_id", generatedId.toString()) }
    }

    @Test
    fun `visitorId with malformed stored value generates new UUID and persists it`() = runTest {
        coEvery { mockDataStore.getString("share_visitor_id") } returns flowOf("not-a-valid-uuid")
        val storage = PreferencesValueStorage.create(mockDataStore, this)
        val generatedId = storage.visitorId
        coVerify { mockDataStore.putString("share_visitor_id", generatedId.toString()) }
    }

    @Test
    fun `deviceToken write calls store putString`() = runTest {
        val token = "device-token-firebase-123"
        val storage = PreferencesValueStorage.create(mockDataStore, this)
        storage.deviceToken = token
        advanceUntilIdle()
        coVerify { mockDataStore.putString("device_token", token) }
    }

    @Test
    fun `welcomeMessage write calls store putString`() = runTest {
        val message = "Welcome to our chat service!"
        val storage = PreferencesValueStorage.create(mockDataStore, this)
        storage.welcomeMessage = message
        advanceUntilIdle()
        coVerify { mockDataStore.putString("share_welcome_message", message) }
    }

    @Test
    fun `clearStorage resets backing fields and calls store clear asynchronously`() = runTest {
        val storage = PreferencesValueStorage.create(mockDataStore, this)
        storage.authToken = "some-token"
        storage.customerId = "some-customer"
        advanceUntilIdle()
        storage.clearStorage()
        storage.authToken shouldBe null
        storage.customerId shouldBe null
        storage.welcomeMessage shouldBe ""
        advanceUntilIdle()
        coVerify { mockDataStore.clear() }
    }

    @Test
    fun `clearStorage resets visitorId to a new UUID immediately`() = runTest {
        val storage = PreferencesValueStorage.create(mockDataStore, this)
        val originalId = storage.visitorId
        storage.clearStorage()
        storage.visitorId shouldNotBe originalId
    }

    @Test
    fun `clearStorage persists the new visitorId after store clear`() = runTest {
        val storage = PreferencesValueStorage.create(mockDataStore, this)
        storage.clearStorage()
        val freshId = storage.visitorId
        advanceUntilIdle()
        coVerify {
            mockDataStore.putString("share_visitor_id", freshId.toString())
        }
    }

    @Test
    fun `destinationId returns consistent value across calls`() = runTest {
        val storage = PreferencesValueStorage.create(mockDataStore, this)
        storage.destinationId shouldBe storage.destinationId
    }

    @Test
    fun `visitDetails write calls store putString`() = runTest {
        val visitDetails = VisitDetails(
            visitId = UUID.randomUUID(),
            validUntil = Clock.System.now()
        )
        val storage = PreferencesValueStorage.create(mockDataStore, this)
        storage.visitDetails = visitDetails
        advanceUntilIdle()
        coVerify(atLeast = 1) { mockDataStore.putString(key = "share_visit_details", value = any()) }
    }

    @Test
    fun `visitDetails write null calls store remove`() = runTest {
        val storage = PreferencesValueStorage.create(mockDataStore, this)
        storage.visitDetails = null
        advanceUntilIdle()
        coVerify { mockDataStore.remove("share_visit_details") }
    }

    @Test
    fun `concurrent visitId access when no visitDetails returns the same UUID for all callers`() = runTest {
        val storage = PreferencesValueStorage.create(mockDataStore, this)
        val ids = java.util.Collections.synchronizedList(mutableListOf<UUID>())

        val jobs = (1..10).map {
            launch(Dispatchers.Default) { ids.add(storage.visitId) }
        }
        jobs.forEach { it.join() }

        ids.distinct().size shouldBe 1
    }

    @Test
    fun `authToken write followed by clearStorage does not persist the written value to disk`() = runTest {
        val storage = PreferencesValueStorage.create(mockDataStore, backgroundScope)
        storage.authToken = "credentials"
        storage.clearStorage()
        advanceUntilIdle()
        coVerify(exactly = 0) { mockDataStore.putString("share_sdk_auth_token", "credentials") }
    }

    @Test
    fun `deserialization failure during create is silently discarded and does not crash`() = runTest {
        coEvery { mockDataStore.getString("share_sdk_refresh_token") } returns flowOf("not valid json {{{")
        val storage = PreferencesValueStorage.create(mockDataStore, this)
        storage.transactionTokenModel shouldBe null
    }

    /**
     * Regression test for DE-138226: clearStorage() must actually persist before it returns, so
     * that a rebuild's create() -- which every real caller (Chat.signOut(), fully awaited up
     * through ChatInstanceProvider.signOut() before it advances state) only runs after
     * clearStorage() has already completed -- reads the fresh visitorId, not stale pre-clear data.
     */
    @Test
    fun `create called after clearStorage completes reads the fresh visitorId, not stale data`() = runTest {
        val staleVisitorId = "11111111-1111-1111-1111-111111111111"
        var freshVisitorId: String? = null
        coEvery { mockDataStore.getString("share_visitor_id") } coAnswers {
            flowOf(freshVisitorId ?: staleVisitorId)
        }
        coEvery { mockDataStore.putString("share_visitor_id", any()) } coAnswers {
            freshVisitorId = secondArg()
        }

        val original = PreferencesValueStorage.create(mockDataStore, backgroundScope)
        original.visitorId.toString() shouldBe staleVisitorId

        original.clearStorage()

        val rebuilt = PreferencesValueStorage.create(mockDataStore, backgroundScope)
        rebuilt.visitorId shouldBe original.visitorId
        rebuilt.visitorId.toString() shouldNotBe staleVisitorId
    }

    /**
     * Regression test: an in-flight sibling-field write (from a property setter, launched before
     * clearStorage() was called) must land before store.clear() runs -- otherwise DataStore's own
     * write-actor decides the order between the two independent edit() calls, and the stale write
     * can resurrect data (e.g. an old auth token) that store.clear() just wiped.
     */
    @Test
    fun `clearStorage waits for an in-flight sibling write before clearing`() = runTest {
        val events = mutableListOf<String>()
        val writeGate = CompletableDeferred<Unit>()
        coEvery { mockDataStore.putString("share_sdk_auth_token", any()) } coAnswers {
            writeGate.await()
            events.add("write")
        }
        coEvery { mockDataStore.clear() } coAnswers { events.add("clear") }

        val storage = PreferencesValueStorage.create(mockDataStore, backgroundScope)
        storage.authToken = "stale-token" // launches the gated write on the same scope

        val clearJob = launch { storage.clearStorage() }
        advanceUntilIdle()
        assertTrue(events.isEmpty(), "clearStorage() must not proceed to store.clear() before the pending write lands")

        writeGate.complete(Unit)
        advanceUntilIdle()
        clearJob.join()

        assertEquals(listOf("write", "clear"), events, "the sibling write must land before store.clear(), never after")
    }
}
