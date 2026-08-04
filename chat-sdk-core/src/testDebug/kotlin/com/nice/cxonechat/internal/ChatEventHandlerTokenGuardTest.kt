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

import com.nice.cxonechat.ChatEventHandler
import com.nice.cxonechat.event.ChatEvent
import com.nice.cxonechat.internal.model.ConfigurationInternal
import com.nice.cxonechat.storage.ValueStorage
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class ChatEventHandlerTokenGuardTest {
    private val mockOriginHandler = mockk<ChatEventHandler>(relaxed = true)
    private val mockStorage = mockk<ValueStorage>(relaxed = true)
    private val mockConfiguration = mockk<ConfigurationInternal>(relaxed = true)
    private val mockEvent = mockk<ChatEvent<*>>()
    private val mockChat = mockk<ChatWithParameters> {
        every { storage } returns mockStorage
        every { configuration } returns mockConfiguration
    }
    private val mockCoordinator = mockk<TokenRefreshCoordinator> {
        coJustRun { refreshIfExpired(any()) }
    }

    private val tokenGuard = ChatEventHandlerTokenGuard(mockOriginHandler, mockChat, mockCoordinator)

    @Test
    fun `trigger calls origin handler with event`() = runTest {
        every { mockStorage.authTokenExpDate } returns Instant.DISTANT_FUTURE

        tokenGuard.trigger(mockEvent)

        coVerify { mockOriginHandler.trigger(mockEvent) }
    }

    @Test
    fun `trigger delegates to coordinator before forwarding event`() = runTest {
        every { mockStorage.authTokenExpDate } returns
                Clock.System.now() + 5.seconds

        tokenGuard.trigger(mockEvent)

        coVerify { mockCoordinator.refreshIfExpired(mockChat) }
        coVerify { mockOriginHandler.trigger(mockEvent) }
    }

    @Test
    fun `trigger calls coordinator even when token not expiring`() = runTest {
        every { mockStorage.authTokenExpDate } returns
                Clock.System.now() + 20.seconds

        tokenGuard.trigger(mockEvent)

        coVerify { mockCoordinator.refreshIfExpired(mockChat) }
        coVerify { mockOriginHandler.trigger(mockEvent) }
    }

    @Test
    fun `trigger handles null token expiration date as no expiration`() = runTest {
        every { mockStorage.authTokenExpDate } returns null

        tokenGuard.trigger(mockEvent)

        coVerify { mockOriginHandler.trigger(mockEvent) }
    }
}
