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

import com.nice.cxonechat.ChatStateListener
import com.nice.cxonechat.api.AuthService
import com.nice.cxonechat.internal.model.ThirdParty
import com.nice.cxonechat.internal.model.TransactionTokenModel
import com.nice.cxonechat.internal.socket.ProxyWebSocketListener
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.storage.ValueStorage
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import org.junit.Before
import retrofit2.Call
import retrofit2.Response
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.Instant

/**
 * Shared fixtures for the [TokenRefreshHelper] test classes: the mocked chat/storage/auth graph,
 * the [Before] setup, and the fresh-token helpers used across the success and error scenarios.
 */
internal abstract class AbstractTokenRefreshHelperTest {
    protected lateinit var chat: ChatWithParameters
    protected lateinit var storage: ValueStorage
    protected lateinit var entrails: ChatEntrails
    protected lateinit var authService: AuthService
    protected lateinit var chatStateListener: ChatStateListener
    protected lateinit var socketListener: ProxyWebSocketListener
    protected lateinit var connection: Connection

    // AtomicReference so the mock getter reflects updates the setter writes,
    // allowing the mutex re-check to observe a fresh token after the first caller refreshes.
    protected val currentExpDate = AtomicReference<Instant?>(null)

    // Real Mutex so withLock actually serializes concurrent callers in the thundering-herd test.
    protected val realTokenRefreshMutex = Mutex()

    protected lateinit var existingTokenModel: TransactionTokenModel

    @Before
    fun setUp() {
        currentExpDate.set(Instant.DISTANT_FUTURE) // fresh by default; each test overrides as needed
        storage = mockk(relaxed = true)
        entrails = mockk(relaxed = true)
        authService = mockk(relaxed = true)
        chatStateListener = mockk(relaxed = true)
        socketListener = mockk(relaxed = true)
        connection = mockk(relaxed = true)
        chat = mockk(relaxed = true)
        existingTokenModel = TransactionTokenModel(
            transactionToken = "old-token",
            expiresIn = 0L,
            thirdParty = ThirdParty(
                accessToken = "old-access",
                refreshToken = "valid-refresh-token",
                expiresIn = 0L
            )
        )
        every { chat.storage } returns storage
        every { chat.entrails } returns entrails
        every { entrails.authService } returns authService
        every { entrails.storage } returns storage
        every { entrails.threading.ioDispatcher } returns Dispatchers.Unconfined
        every { chat.chatStateListener } returns chatStateListener
        every { chat.socketListener } returns socketListener
        every { chat.connection } returns connection
        every { connection.brandId } returns 1
        every { connection.channelId } returns "channel-1"
        every { storage.visitorId } returns UUID.randomUUID()
        every { storage.authTokenExpDate } answers { currentExpDate.get() }
        every { storage.authTokenExpDate = any() } answers { currentExpDate.set(firstArg()) }
        every { storage.transactionTokenModel } returns existingTokenModel
        every { storage.visitorId } returns UUID.fromString("00000000-0000-0000-0000-000000000001")
        every { chat.guards } returns Threading.Guards(tokenRefreshMutex = realTokenRefreshMutex)
    }

    protected fun successfulRefreshCall(freshAccessToken: String, expiresInSeconds: Long = 3600L): Call<TransactionTokenModel> {
        val model = buildFreshTokenModel(freshAccessToken, expiresInSeconds)
        return mockk { every { execute() } returns Response.success(model) }
    }

    protected fun buildFreshTokenModel(
        freshAccessToken: String = "fresh-access",
        expiresInSeconds: Long,
    ) = TransactionTokenModel(
        transactionToken = "fresh-transaction-token",
        expiresIn = expiresInSeconds,
        thirdParty = ThirdParty(
            accessToken = freshAccessToken,
            refreshToken = "fresh-refresh-token",
            expiresIn = expiresInSeconds
        )
    )
}
