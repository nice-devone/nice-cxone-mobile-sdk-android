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
import com.nice.cxonechat.ChatThreadEventHandler
import com.nice.cxonechat.event.RefreshToken
import com.nice.cxonechat.event.thread.ChatThreadEvent
import com.nice.cxonechat.internal.model.ConfigurationInternal
import com.nice.cxonechat.state.Configuration
import com.nice.cxonechat.storage.ValueStorage
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.util.Date
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

class ChatThreadEventHandlerTokenGuardTest {
    private val mockOriginHandler = mockk<ChatThreadEventHandler>(relaxed = true)
    private val mockStorage = mockk<ValueStorage>(relaxed = true)
    private val mockConfiguration = mockk<ConfigurationInternal>(relaxed = true)
    private val mockEvent = mockk<ChatThreadEvent>()
    private val mockEventHandler = mockk<ChatEventHandler>(relaxed = true)
    private val mockChat = mockk<ChatWithParameters> {
        every { storage } returns mockStorage
        every { configuration } returns mockConfiguration
        every { events() } returns mockEventHandler
    }

    private val tokenGuard = ChatThreadEventHandlerTokenGuard(mockOriginHandler, mockChat)

    @Test
    fun `trigger calls origin handler with event`() {
        every { mockStorage.authTokenExpDate } returns Date(Long.MAX_VALUE)

        tokenGuard.trigger(mockEvent, null, null, null)

        verify { mockOriginHandler.trigger(mockEvent, null, null, null) }
    }

    @Test
    fun `trigger refreshes token when expiring soon`() {
        // Token expires in 5 seconds (less than 10 seconds guard)
        every { mockStorage.authTokenExpDate } returns
                Date(System.currentTimeMillis() + 5.seconds.inWholeMilliseconds)
        every { mockConfiguration.hasFeature(Configuration.Feature.SecuredSessions) } returns false

        tokenGuard.trigger(mockEvent, null, null, null)

        verify { mockEventHandler.trigger(RefreshToken, null, null) }
        verify { mockOriginHandler.trigger(mockEvent, null, null, null) }
    }

    @Test
    fun `trigger does not refresh token when not expiring soon`() {
        // Token expires in 20 seconds (more than 10 seconds guard)
        every { mockStorage.authTokenExpDate } returns
                Date(System.currentTimeMillis() + 20.seconds.inWholeMilliseconds)

        tokenGuard.trigger(mockEvent, null, null, null)

        verify(exactly = 0) { mockEventHandler.trigger(RefreshToken, null, null) }
        verify { mockOriginHandler.trigger(mockEvent, null, null, null) }
    }

    @Test
    fun `trigger handles null token expiration date as no expiration`() {
        every { mockStorage.authTokenExpDate } returns null

        tokenGuard.trigger(mockEvent, null, null, null)

        // With no expiration date, token is not considered expired, so no refresh
        verify(exactly = 0) { mockEventHandler.trigger(RefreshToken, null, null) }
        verify { mockOriginHandler.trigger(mockEvent, null, null, null) }
    }

    @Test
    fun `trigger with listeners passes them through`() {
        val mockListener = mockk<ChatThreadEventHandler.OnEventSentListener>()
        val mockErrorListener = mockk<ChatThreadEventHandler.OnEventErrorListener>()
        val mockResponseListener = mockk<ChatThreadEventHandler.OnEventResponseListener>()
        every { mockStorage.authTokenExpDate } returns Date(Long.MAX_VALUE)

        tokenGuard.trigger(mockEvent, mockListener, mockErrorListener, mockResponseListener)

        verify { mockOriginHandler.trigger(mockEvent, mockListener, mockErrorListener, mockResponseListener) }
    }

    @Test
    fun `trigger with expired token - past expiration`() {
        // Token expired 10 seconds ago
        every { mockStorage.authTokenExpDate } returns
                Date(System.currentTimeMillis() - 10.seconds.inWholeMilliseconds)
        every { mockConfiguration.hasFeature(Configuration.Feature.SecuredSessions) } returns false

        tokenGuard.trigger(mockEvent, null, null, null)

        // Expired token should trigger refresh via events()
        verify { mockEventHandler.trigger(RefreshToken, null, null) }
        verify { mockOriginHandler.trigger(mockEvent, null, null, null) }
    }

    @Test
    fun `trigger calls events handler refresh token for non-SecuredSessions`() {
        every { mockStorage.authTokenExpDate } returns
                Date(System.currentTimeMillis() + 3.seconds.inWholeMilliseconds)
        every { mockConfiguration.hasFeature(Configuration.Feature.SecuredSessions) } returns false

        tokenGuard.trigger(mockEvent, null, null, null)

        // For non-SecuredSessions, should call events().trigger(RefreshToken)
        verify { mockEventHandler.trigger(RefreshToken, null, null) }
    }

    @Test
    fun `trigger with all null listeners`() {
        every { mockStorage.authTokenExpDate } returns Date(Long.MAX_VALUE)

        tokenGuard.trigger(mockEvent, null, null, null)

        verify { mockOriginHandler.trigger(mockEvent, null, null, null) }
    }

    @Test
    fun `trigger delegates delegation correctly to ChatThreadEventHandler`() {
        every { mockStorage.authTokenExpDate } returns Date(Long.MAX_VALUE)

        // Should be able to call delegated methods
        tokenGuard.trigger(mockEvent, null, null, null)

        verify { mockOriginHandler.trigger(mockEvent, null, null, null) }
    }
}
