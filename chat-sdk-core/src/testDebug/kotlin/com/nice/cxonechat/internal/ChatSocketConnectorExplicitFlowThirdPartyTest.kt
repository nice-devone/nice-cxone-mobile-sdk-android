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

import com.nice.cxonechat.enums.AuthenticationType
import com.nice.cxonechat.internal.model.ThirdParty
import com.nice.cxonechat.internal.model.TransactionTokenModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

/**
 * Regression coverage for the third-party (access token/refresh token/expiry) state that
 * [ChatSocketConnector] persists after explicit flow's reconnect paths finish with an
 * internally-triggered implicit-grant call. That call's response isn't guaranteed to echo back
 * [ThirdParty] the same way an explicit `AUTHORIZATION_CODE`/`REFRESH_TOKEN` response does, so the
 * connector patches the known-correct third-party state onto the result rather than trusting it —
 * otherwise `ChatImpl` would persist a null/incomplete access token and later message sends would fail.
 * Call sequencing and error handling for these same paths live in [ChatSocketConnectorExplicitFlowTest].
 */
internal class ChatSocketConnectorExplicitFlowThirdPartyTest : AbstractChatSocketConnectorTest() {

    @Test
    fun `connect with ThirdPartyOAuth explicit flow preserves refreshed third party even when implicit response returns a different one`() =
        runTest {
        every { configuration.authenticationType } returns AuthenticationType.ThirdPartyOAuth
        every { chat.isImplicitOAuthFlow } returns false
        // A real instance (not a mock) since ChatSocketConnector now persists the refreshed
        // credentials via .copy()/.expiresAt right after the refresh grant succeeds. expiresIn = 0
        // (expired) also drives the connect() decision to take the refresh-token path.
        val storedThirdParty = ThirdParty(refreshToken = "stored-refresh-token", expiresIn = 0L)
        val expiredModel = TransactionTokenModel(transactionToken = null, thirdParty = storedThirdParty)
        every { storage.transactionTokenModel } returns expiredModel
        val visitorId = UUID.randomUUID()
        every { storage.visitorId } returns visitorId

        // Step 1 (refresh_token grant) response: the fresh, authoritative third-party state
        val freshThirdParty = ThirdParty(
            accessToken = "fresh-access-token",
            refreshToken = "rotated-refresh-token",
            expiresIn = 3600L,
        )
        val refreshResponseModel = TransactionTokenModel(transactionToken = null, expiresIn = null, thirdParty = freshThirdParty)
        val refreshResponse = mockk<retrofit2.Response<TransactionTokenModel>> {
            every { isSuccessful } returns true
            every { body() } returns refreshResponseModel
        }
        // Step 2 (implicit grant) response: a stale/different thirdParty that must NOT win — the backend
        // isn't guaranteed to echo the refreshed credential back on this internally-triggered call.
        val staleThirdParty = ThirdParty(accessToken = "stale-should-be-ignored", expiresIn = 60L)
        val finalModel = TransactionTokenModel(transactionToken = "new-tx-token", expiresIn = 3600L, thirdParty = staleThirdParty)
        val finalResponse = mockk<retrofit2.Response<TransactionTokenModel>> {
            every { isSuccessful } returns true
            every { body() } returns finalModel
        }
        every { authService.getTransactionToken(any(), any(), any(), any()) } returnsMany listOf(
            mockk { every { execute() } returns refreshResponse },
            mockk { every { execute() } returns finalResponse },
        )
        every { socketFactory.create(any(), any(), any()) } returns webSocket

        val result = connector.connect(mockk { every { thirdParty } returns null })

        val persistedThirdParty = result.getOrThrow().transactionTokenModel?.thirdParty
        assertEquals("fresh-access-token", persistedThirdParty?.accessToken)
        assertEquals("rotated-refresh-token", persistedThirdParty?.refreshToken)
    }

    @Test
    fun `connect with ThirdPartyOAuth explicit flow preserves cached third party when implicit response omits it`() = runTest {
        every { configuration.authenticationType } returns AuthenticationType.ThirdPartyOAuth
        every { chat.isImplicitOAuthFlow } returns false
        // expiresIn = 3600 (valid) drives the connect() decision to take the valid-access-token path.
        val cachedThirdParty = ThirdParty(
            accessToken = "valid-access-token",
            refreshToken = "cached-refresh-token",
            expiresIn = 3600L,
        )
        val expiredModel = mockk<TransactionTokenModel> {
            every { isExpired } returns true
            every { this@mockk.thirdParty } returns cachedThirdParty
        }
        every { storage.transactionTokenModel } returns expiredModel
        val visitorId = UUID.randomUUID()
        every { storage.visitorId } returns visitorId

        // Backend's implicit-grant response omits thirdParty entirely — the real-world case this guards
        // against: implicit-flow consumers already hold their own token, so the backend has no reason
        // to return it on this internally-triggered call.
        val finalModel = TransactionTokenModel(transactionToken = "new-tx-token", expiresIn = 3600L)
        val response = mockk<retrofit2.Response<TransactionTokenModel>> {
            every { isSuccessful } returns true
            every { body() } returns finalModel
        }
        every { authService.getTransactionToken(any(), any(), any(), any()) } returns mockk {
            every { execute() } returns response
        }
        every { socketFactory.create(any(), any(), any()) } returns webSocket

        val result = connector.connect(mockk { every { thirdParty } returns null })

        val persistedThirdParty = result.getOrThrow().transactionTokenModel?.thirdParty
        assertEquals("valid-access-token", persistedThirdParty?.accessToken)
        assertEquals("cached-refresh-token", persistedThirdParty?.refreshToken)
    }

    @Test
    fun `connect with ThirdPartyOAuth explicit flow preserves cached refresh token when refresh response omits it`() = runTest {
        every { configuration.authenticationType } returns AuthenticationType.ThirdPartyOAuth
        every { chat.isImplicitOAuthFlow } returns false
        // A real instance (not a mock) since ChatSocketConnector now persists the refreshed
        // credentials via .copy()/.expiresAt right after the refresh grant succeeds. expiresIn = 0
        // (expired) also drives the connect() decision to take the refresh-token path.
        val storedThirdParty = ThirdParty(refreshToken = "stored-refresh-token", expiresIn = 0L)
        val expiredModel = TransactionTokenModel(transactionToken = null, thirdParty = storedThirdParty)
        every { storage.transactionTokenModel } returns expiredModel
        val visitorId = UUID.randomUUID()
        every { storage.visitorId } returns visitorId

        // Step 1 (refresh_token grant) response omits refresh_token entirely — a non-rotating backend
        // (RFC 6749 §6 makes it optional) expects the client to keep the one it already has.
        val refreshedNoRefreshToken = ThirdParty(accessToken = "fresh-access-token", refreshToken = null, expiresIn = 3600L)
        val refreshResponseModel = mockk<TransactionTokenModel> {
            every { this@mockk.transactionToken } returns null
            every { this@mockk.thirdParty } returns refreshedNoRefreshToken
        }
        val refreshResponse = mockk<retrofit2.Response<TransactionTokenModel>> {
            every { isSuccessful } returns true
            every { body() } returns refreshResponseModel
        }
        // Step 2 (implicit grant) response: the real transaction token.
        val finalModel = TransactionTokenModel(transactionToken = "new-tx-token", expiresIn = 3600L)
        val finalResponse = mockk<retrofit2.Response<TransactionTokenModel>> {
            every { isSuccessful } returns true
            every { body() } returns finalModel
        }
        every { authService.getTransactionToken(any(), any(), any(), any()) } returnsMany listOf(
            mockk { every { execute() } returns refreshResponse },
            mockk { every { execute() } returns finalResponse },
        )
        every { socketFactory.create(any(), any(), any()) } returns webSocket

        val result = connector.connect(mockk { every { thirdParty } returns null })

        val persistedThirdParty = result.getOrThrow().transactionTokenModel?.thirdParty
        assertEquals("fresh-access-token", persistedThirdParty?.accessToken)
        assertEquals("stored-refresh-token", persistedThirdParty?.refreshToken)
    }
}
