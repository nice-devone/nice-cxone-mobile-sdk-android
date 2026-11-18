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

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
internal class PreferencesValueStorageSurveyDeliveryTest {
    private lateinit var mockDataStore: EncryptedDataStoreContract

    @Before
    fun setUp() {
        mockDataStore = mockk(relaxed = true)
    }

    @Test
    fun `markSurveyDeliveredIfAbsent returns true once then false for the same thread`() = runTest {
        val storage = PreferencesValueStorage.create(mockDataStore, this)
        val threadId = UUID.randomUUID()

        storage.markSurveyDeliveredIfAbsent(threadId) shouldBe true
        storage.markSurveyDeliveredIfAbsent(threadId) shouldBe false
    }

    @Test
    fun `isSurveyDelivered reflects markSurveyDeliveredIfAbsent without consuming it itself`() = runTest {
        val storage = PreferencesValueStorage.create(mockDataStore, this)
        val threadId = UUID.randomUUID()

        storage.isSurveyDelivered(threadId) shouldBe false
        // A peek must not itself record a delivery - repeating it must not change the outcome.
        storage.isSurveyDelivered(threadId) shouldBe false

        storage.markSurveyDeliveredIfAbsent(threadId) shouldBe true

        storage.isSurveyDelivered(threadId) shouldBe true
    }

    @Test
    fun `markSurveyDeliveredIfAbsent persists and reloads through create`() = runTest {
        val threadId = UUID.randomUUID()
        var persisted: String? = null
        coEvery { mockDataStore.putString("share_delivered_satisfaction_survey_thread_ids", any()) } coAnswers {
            persisted = secondArg()
        }
        coEvery { mockDataStore.getString("share_delivered_satisfaction_survey_thread_ids") } coAnswers {
            flowOf(persisted)
        }

        val storage = PreferencesValueStorage.create(mockDataStore, this)
        storage.markSurveyDeliveredIfAbsent(threadId)
        advanceUntilIdle()

        val reloaded = PreferencesValueStorage.create(mockDataStore, this)
        reloaded.markSurveyDeliveredIfAbsent(threadId) shouldBe false
    }

    @Test
    fun `markSurveyDeliveredIfAbsent evicts the oldest id once the bound is exceeded`() = runTest {
        val storage = PreferencesValueStorage.create(mockDataStore, this)
        val firstId = UUID.randomUUID()
        storage.markSurveyDeliveredIfAbsent(firstId) shouldBe true

        // One more than the bound, so firstId is pushed out and becomes insertable again.
        repeat(50) { storage.markSurveyDeliveredIfAbsent(UUID.randomUUID()) }

        storage.markSurveyDeliveredIfAbsent(firstId) shouldBe true
    }

    @Test
    fun `malformed stored delivered-survey ids are discarded instead of crashing create`() = runTest {
        coEvery { mockDataStore.getString("share_delivered_satisfaction_survey_thread_ids") } returns
            flowOf("not valid json {{{")

        val storage = PreferencesValueStorage.create(mockDataStore, this)

        // Discarded, not just ignored - a thread id must still be freshly insertable.
        storage.markSurveyDeliveredIfAbsent(UUID.randomUUID()) shouldBe true
    }

    @Test
    fun `clearStorage resets delivered-survey ids so a cleared thread is insertable again`() = runTest {
        val storage = PreferencesValueStorage.create(mockDataStore, this)
        val threadId = UUID.randomUUID()
        storage.markSurveyDeliveredIfAbsent(threadId)

        storage.clearStorage()

        storage.markSurveyDeliveredIfAbsent(threadId) shouldBe true
    }

    @Test
    fun `concurrent markSurveyDeliveredIfAbsent for the same thread returns true exactly once`() = runTest {
        val storage = PreferencesValueStorage.create(mockDataStore, backgroundScope)
        val threadId = UUID.randomUUID()
        val results = java.util.Collections.synchronizedList(mutableListOf<Boolean>())

        val jobs = (1..20).map {
            launch(Dispatchers.Default) { results.add(storage.markSurveyDeliveredIfAbsent(threadId)) }
        }
        jobs.joinAll()

        // Regression guard for a retry bug in the CAS loop: a losing attempt's stale "inserted"
        // flag could survive a retry and report true for more than one caller.
        results.count { it } shouldBe 1
    }
}
