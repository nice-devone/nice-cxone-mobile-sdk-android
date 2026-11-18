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
import com.nice.cxonechat.internal.model.GrantType
import com.nice.cxonechat.internal.model.ThirdParty
import com.nice.cxonechat.internal.model.TokenRequestBody
import com.nice.cxonechat.internal.model.TransactionTokenModel
import com.nice.cxonechat.internal.socket.ProxyWebSocketListener
import com.nice.cxonechat.internal.socket.SocketState
import com.nice.cxonechat.internal.socket.WebsocketLogging
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * ThirdPartyOAuth **explicit** flow reconnect behavior of [ChatSocketConnector] specifically when
 * [com.nice.cxonechat.storage.ValueStorage.authTokenExpDate] says the cached access token is still
 * locally valid: [ChatSocketConnector.connectWithValidAccessToken]'s implicit-grant reuse, its
 * fallback to the refresh token on backend rejection, and its error handling.
 * The refresh-token / expired-access-token paths live in [ChatSocketConnectorExplicitFlowTest].
 */
internal class ChatSocketConnectorValidAccessTokenTest : AbstractChatSocketConnectorTest() {

    @Test
    fun `connect with ThirdPartyOAuth explicit flow and valid access token reuses it via implicit grant`() = runTest {
        every { configuration.authenticationType } returns AuthenticationType.ThirdPartyOAuth
        every { chat.isImplicitOAuthFlow } returns false
        // A real instance (not a mock) since the connect() decision now derives the access
        // token's expiry from thirdParty.expiresAt rather than storage.authTokenExpDate.
        val storedThirdParty = ThirdParty(accessToken = "valid-access-token", expiresIn = 3600L)
        val expiredModel = mockk<TransactionTokenModel> {
            every { isExpired } returns true
            every { this@mockk.thirdParty } returns storedThirdParty
        }
        every { storage.transactionTokenModel } returns expiredModel
        val visitorId = UUID.randomUUID()
        every { storage.visitorId } returns visitorId
        // A real instance (not a mock) since ChatSocketConnector patches its thirdParty via .copy().
        val newModel = TransactionTokenModel(transactionToken = "new-tx-token", expiresIn = 3600L, customerIdentity = null)
        val response = mockk<retrofit2.Response<TransactionTokenModel>> {
            every { isSuccessful } returns true
            every { body() } returns newModel
        }
        val capturedBodies = mutableListOf<TokenRequestBody>()
        every { authService.getTransactionToken(any(), any(), any(), capture(capturedBodies)) } returns mockk {
            every { execute() } returns response
        }
        every { socketFactory.create(any(), any(), any()) } returns webSocket

        val result = connector.connect(mockk { every { thirdParty } returns null })

        assertNull(result.exceptionOrNull())
        assertTrue(result.getOrThrow().webSocket is WebsocketLogging)
        assertEquals(1, capturedBodies.size)
        assertEquals(GrantType.IMPLICIT, capturedBodies[0].thirdParty?.grantType)
        assertEquals("valid-access-token", capturedBodies[0].thirdParty?.accessToken)
        verify(exactly = 1) { authService.getTransactionToken(any(), any(), any(), any()) }
    }

    @Test
    fun `connect with ThirdPartyOAuth explicit flow falls back to refresh token when backend rejects the locally-valid access token`() =
        runTest {
        every { configuration.authenticationType } returns AuthenticationType.ThirdPartyOAuth
        every { chat.isImplicitOAuthFlow } returns false
        // Real instances (not mocks) since ChatSocketConnector now persists the refreshed
        // credentials via .copy()/.expiresAt right after the refresh grant succeeds. expiresIn = 3600
        // (valid) also drives the connect() decision to take the valid-access-token path.
        val storedThirdParty = ThirdParty(accessToken = "stale-access-token", refreshToken = "stored-refresh-token", expiresIn = 3600L)
        val expiredModel = TransactionTokenModel(transactionToken = null, thirdParty = storedThirdParty)
        every { storage.transactionTokenModel } returns expiredModel
        val visitorId = UUID.randomUUID()
        every { storage.visitorId } returns visitorId

        // Step 1: implicit grant using the locally-unexpired access token — backend rejects it (400).
        val rejectedResponse = mockk<retrofit2.Response<TransactionTokenModel>> {
            every { isSuccessful } returns false
            every { errorBody() } returns null
            every { code() } returns 400
            every { message() } returns "Bad Request"
        }
        // Step 2: refresh_token grant, only thirdParty (fresh access token) is populated.
        // Rotating-refresh-token backend: a fresh refresh token is present, so the merge in
        // ChatSocketConnector must not touch it.
        val refreshedThirdParty = ThirdParty(accessToken = "fresh-access-token", refreshToken = "rotated-refresh-token", expiresIn = 3600L)
        val refreshResponseModel = mockk<TransactionTokenModel> {
            every { this@mockk.transactionToken } returns null
            every { this@mockk.thirdParty } returns refreshedThirdParty
        }
        val refreshResponse = mockk<retrofit2.Response<TransactionTokenModel>> {
            every { isSuccessful } returns true
            every { body() } returns refreshResponseModel
        }
        // Step 3: implicit grant using the fresh access token, returns the real transaction token.
        // A real instance (not a mock) since ChatSocketConnector patches its thirdParty via .copy().
        val finalModel = TransactionTokenModel(transactionToken = "new-tx-token", expiresIn = 3600L, customerIdentity = null)
        val finalResponse = mockk<retrofit2.Response<TransactionTokenModel>> {
            every { isSuccessful } returns true
            every { body() } returns finalModel
        }
        val capturedBodies = mutableListOf<TokenRequestBody>()
        every { authService.getTransactionToken(any(), any(), any(), capture(capturedBodies)) } returnsMany listOf(
            mockk { every { execute() } returns rejectedResponse },
            mockk { every { execute() } returns refreshResponse },
            mockk { every { execute() } returns finalResponse },
        )
        every { socketFactory.create(any(), any(), any()) } returns webSocket

        val result = connector.connect(mockk { every { thirdParty } returns null })

        assertNull(result.exceptionOrNull())
        assertTrue(result.getOrThrow().webSocket is WebsocketLogging)
        assertEquals(3, capturedBodies.size)
        assertEquals(GrantType.IMPLICIT, capturedBodies[0].thirdParty?.grantType)
        assertEquals("stale-access-token", capturedBodies[0].thirdParty?.accessToken)
        assertEquals(GrantType.REFRESH_TOKEN, capturedBodies[1].thirdParty?.grantType)
        assertEquals("stored-refresh-token", capturedBodies[1].thirdParty?.refreshToken)
        assertEquals(GrantType.IMPLICIT, capturedBodies[2].thirdParty?.grantType)
        assertEquals("fresh-access-token", capturedBodies[2].thirdParty?.accessToken)
        // The rejected access token attempt is a recoverable retry, not a reported failure.
        verify(exactly = 0) { chatStateListener.onChatRuntimeException(any()) }
    }

    @Test
    fun `connect with valid access token reports socket closed without crashing when socketFactory throws a non-RuntimeChatException`() =
        runTest {
        every { configuration.authenticationType } returns AuthenticationType.ThirdPartyOAuth
        every { chat.isImplicitOAuthFlow } returns false
        // A real instance (not a mock) since the connect() decision now derives the access
        // token's expiry from thirdParty.expiresAt rather than storage.authTokenExpDate.
        val storedThirdParty = ThirdParty(accessToken = "valid-access-token", expiresIn = 3600L)
        val expiredModel = mockk<TransactionTokenModel> {
            every { isExpired } returns true
            every { this@mockk.thirdParty } returns storedThirdParty
        }
        every { storage.transactionTokenModel } returns expiredModel
        val visitorId = UUID.randomUUID()
        every { storage.visitorId } returns visitorId

        val finalModel = TransactionTokenModel(transactionToken = "new-tx-token", expiresIn = 3600L, customerIdentity = null)
        val response = mockk<retrofit2.Response<TransactionTokenModel>> {
            every { isSuccessful } returns true
            every { body() } returns finalModel
        }
        every { authService.getTransactionToken(any(), any(), any(), any()) } returns mockk {
            every { execute() } returns response
        }
        val socketListener = mockk<ProxyWebSocketListener>(relaxed = true)
        every { chat.socketListener } returns socketListener
        every { socketFactory.create(any(), any(), any()) } throws IllegalStateException("socket setup failed")

        val result = connector.connect(mockk { every { thirdParty } returns null })

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        verify { socketListener.reportState(SocketState.CLOSED) }
        verify(exactly = 0) { chatStateListener.onChatRuntimeException(any()) }
    }

    @Test
    fun `connect with ThirdPartyOAuth explicit flow and valid authTokenExpDate but no stored access token falls back to refresh token`() =
        runTest {
        every { configuration.authenticationType } returns AuthenticationType.ThirdPartyOAuth
        every { chat.isImplicitOAuthFlow } returns false
        // Real instances (not mocks) since ChatSocketConnector now persists the refreshed
        // credentials via .copy()/.expiresAt right after the refresh grant succeeds. expiresIn = 3600
        // (valid) also drives the connect() decision to take the valid-access-token path.
        val storedThirdParty = ThirdParty(accessToken = null, refreshToken = "stored-refresh-token", expiresIn = 3600L)
        val expiredModel = TransactionTokenModel(transactionToken = null, thirdParty = storedThirdParty)
        every { storage.transactionTokenModel } returns expiredModel
        val visitorId = UUID.randomUUID()
        every { storage.visitorId } returns visitorId

        val refreshedThirdParty = ThirdParty(accessToken = "fresh-access-token", refreshToken = "rotated-refresh-token", expiresIn = 3600L)
        val refreshResponseModel = mockk<TransactionTokenModel> {
            every { this@mockk.transactionToken } returns null
            every { this@mockk.thirdParty } returns refreshedThirdParty
        }
        // A real instance (not a mock) since ChatSocketConnector patches its thirdParty via .copy().
        val finalModel = TransactionTokenModel(transactionToken = "new-tx-token", expiresIn = 3600L, customerIdentity = null)
        val refreshResponse = mockk<retrofit2.Response<TransactionTokenModel>> {
            every { isSuccessful } returns true
            every { body() } returns refreshResponseModel
        }
        val finalResponse = mockk<retrofit2.Response<TransactionTokenModel>> {
            every { isSuccessful } returns true
            every { body() } returns finalModel
        }
        val capturedBodies = mutableListOf<TokenRequestBody>()
        every { authService.getTransactionToken(any(), any(), any(), capture(capturedBodies)) } returnsMany listOf(
            mockk { every { execute() } returns refreshResponse },
            mockk { every { execute() } returns finalResponse },
        )
        every { socketFactory.create(any(), any(), any()) } returns webSocket

        val result = connector.connect(mockk { every { thirdParty } returns null })

        assertNull(result.exceptionOrNull())
        assertEquals(GrantType.REFRESH_TOKEN, capturedBodies[0].thirdParty?.grantType)
    }
}
