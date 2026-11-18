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

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** When [PreferencesValueStorage.clearStorage] makes the cleared session observable, relative to its pending-write drain. */
@OptIn(ExperimentalCoroutinesApi::class)
internal class PreferencesValueStorageClearOrderTest {
    private val mockDataStore: EncryptedDataStoreContract = mockk(relaxed = true)

    /**
     * A token refresh still in flight during signOut() treats a changed visitorId as "storage was cleared"
     * (TokenRefreshHelper), so the rotation must be visible while clearStorage() still waits for pending writes.
     */
    @Test
    fun `clearStorage rotates visitorId before waiting for an in-flight sibling write`() = runTest {
        val writeGate = CompletableDeferred<Unit>()
        coEvery { mockDataStore.putString("share_sdk_auth_token", any()) } coAnswers { writeGate.await() }

        val storage = PreferencesValueStorage.create(mockDataStore, backgroundScope)
        val originalVisitorId = storage.visitorId
        storage.authToken = "stale-token" // launches the gated write on the same scope

        val clearJob = launch { storage.clearStorage() }
        advanceUntilIdle()
        assertTrue(clearJob.isActive, "Precondition: clearStorage() is still waiting for the pending write")
        assertNotEquals(originalVisitorId, storage.visitorId, "visitorId must rotate before the drain, not after it")

        writeGate.complete(Unit)
        clearJob.join()
    }
}
