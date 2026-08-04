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

package com.nice.cxonechat.internal

import com.nice.cxonechat.api.AuthService
import com.nice.cxonechat.storage.ValueStorage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class TokenRefreshCoordinatorTest {

    private lateinit var storage: ValueStorage
    private lateinit var chat: ChatWithParameters
    private lateinit var entrails: ChatEntrails
    private lateinit var authService: AuthService
    private val coordinator = TokenRefreshCoordinator()

    // Real Mutex so withLock actually serializes concurrent callers in the thundering-herd test.
    private val realTokenRefreshMutex = Mutex()

    // AtomicReference so the mock getter reflects updates the setter writes,
    // allowing the mutex re-check to observe a fresh token after the first caller refreshes.
    private val currentExpDate = AtomicReference<Instant?>(null)

    @Before
    fun setUp() {
        storage = mockk(relaxed = true)
        chat = mockk(relaxed = true)
        entrails = mockk(relaxed = true)
        authService = mockk(relaxed = true)
        every { entrails.authService } returns authService
        every { chat.storage } returns storage
        every { chat.entrails } returns entrails
        mockkObject(TokenRefreshHelper)
        every { chat.guards } returns Threading.Guards(tokenRefreshMutex = realTokenRefreshMutex)
        every { storage.authTokenExpDate } answers { currentExpDate.get() }
        every { storage.authTokenExpDate = any() } answers { currentExpDate.set(firstArg()) }
    }

    @After
    fun tearDown() {
        unmockkObject(TokenRefreshHelper)
    }

    @Test
    fun `concurrent callers with expired token make only one HTTP call`() = runTest {
        val expiredDate = Clock.System.now() - 1.seconds
        val freshDate = Clock.System.now() + 3600.seconds
        currentExpDate.set(expiredDate)
        coEvery { TokenRefreshHelper.refreshAccessToken(any()) } coAnswers {
            currentExpDate.set(freshDate) // Simulate token refresh updating storage with a fresh expiry
            delay(50.milliseconds) // Let other threads queue up on the mutex while the first thread is "refreshing"
        }
        val jobs = (1..5).map {
            launch(Dispatchers.IO) { coordinator.refreshIfExpired(chat) }
        }
        jobs.joinAll()
        coVerify(exactly = 1) { TokenRefreshHelper.refreshAccessToken(chat) }
    }

    @Test
    fun `refreshIfExpired skips refresh when token is fresh`() = runTest {
        every { storage.authTokenExpDate } returns Clock.System.now() + 60.seconds

        coordinator.refreshIfExpired(chat)

        coVerify(exactly = 0) { TokenRefreshHelper.refreshAccessToken(any()) }
    }

    @Test
    fun `refreshIfExpired triggers refresh when token is expired`() = runTest {
        every { storage.authTokenExpDate } returns Clock.System.now() - 1.seconds

        coordinator.refreshIfExpired(chat)

        coVerify(exactly = 1) { TokenRefreshHelper.refreshAccessToken(chat) }
    }

    @Test
    fun `refreshIfExpired skips refresh when token is null`() = runTest {
        every { storage.authTokenExpDate } returns null

        coordinator.refreshIfExpired(chat)

        // null means no expiry known — treated as Instant.DISTANT_FUTURE (no refresh needed)
        coVerify(exactly = 0) { TokenRefreshHelper.refreshAccessToken(any()) }
    }

    @Test
    fun `does not refresh when token expires in 15 seconds`() = runTest {
        every { storage.authTokenExpDate } returns Clock.System.now() + 15.seconds

        coordinator.refreshIfExpired(chat)

        coVerify(exactly = 0) { TokenRefreshHelper.refreshAccessToken(any()) }
    }
}
