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
import com.nice.cxonechat.exceptions.RuntimeChatException
import com.nice.cxonechat.internal.model.GrantType
import com.nice.cxonechat.internal.model.ThirdParty
import com.nice.cxonechat.internal.model.ThirdPartyOAuthBody
import com.nice.cxonechat.internal.model.TokenRequestBody
import com.nice.cxonechat.internal.model.TransactionTokenModel
import com.nice.cxonechat.internal.socket.WebsocketLogging
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * ThirdPartyOAuth **explicit** flow reconnect behavior of [ChatSocketConnector]: expired
 * transaction token with an also-expired access token, and the resulting refresh-token /
 * implicit-grant call sequencing and error reporting.
 * The locally-still-valid-access-token path lives in [ChatSocketConnectorValidAccessTokenTest];
 * implicit flow lives in [ChatSocketConnectorOAuthTest]; the cached/basic flow lives in
 * [ChatSocketConnectorTest].
 */
internal class ChatSocketConnectorExplicitFlowTest : AbstractChatSocketConnectorTest() {

    @Test
    fun `connect with ThirdPartyOAuth explicit flow and expired access token uses refresh token then implicit grant`() = runTest {
        every { configuration.authenticationType } returns AuthenticationType.ThirdPartyOAuth
        every { chat.isImplicitOAuthFlow } returns false
        // Real instances (not mocks) since ChatSocketConnector now persists the refreshed
        // credentials via .copy()/.expiresAt right after the refresh grant succeeds. expiresIn = 0
        // (expired) also drives the connect() decision to take the refresh-token path.
        val storedThirdParty = ThirdParty(refreshToken = "stored-refresh-token", expiresIn = 0L)
        val expiredModel = TransactionTokenModel(transactionToken = null, thirdParty = storedThirdParty)
        every { storage.transactionTokenModel } returns expiredModel
        val visitorId = UUID.randomUUID()
        every { storage.visitorId } returns visitorId

        // Step 1 response: refresh_token grant, only thirdParty (fresh access token) is populated.
        // Rotating-refresh-token backend: a fresh refresh token is present, so the merge in
        // ChatSocketConnector must not touch it — see the fallback-preserving test below for when
        // the response omits it instead.
        val refreshedThirdParty = ThirdParty(accessToken = "fresh-access-token", refreshToken = "rotated-refresh-token", expiresIn = 3600L)
        val refreshResponseModel = mockk<TransactionTokenModel> {
            every { this@mockk.transactionToken } returns null
            every { this@mockk.thirdParty } returns refreshedThirdParty
        }
        val refreshResponse = mockk<retrofit2.Response<TransactionTokenModel>> {
            every { isSuccessful } returns true
            every { body() } returns refreshResponseModel
        }
        // Step 2 response: implicit grant using the fresh access token, returns the real transaction token.
        // A real instance (not a mock) since ChatSocketConnector patches its thirdParty via .copy().
        val finalModel = TransactionTokenModel(transactionToken = "new-tx-token", expiresIn = 3600L, customerIdentity = null)
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
        assertTrue(result.getOrThrow().webSocket is WebsocketLogging)
        assertEquals(2, capturedBodies.size)
        assertEquals(GrantType.REFRESH_TOKEN, capturedBodies[0].thirdParty?.grantType)
        assertEquals("stored-refresh-token", capturedBodies[0].thirdParty?.refreshToken)
        assertEquals(GrantType.IMPLICIT, capturedBodies[1].thirdParty?.grantType)
        assertEquals("fresh-access-token", capturedBodies[1].thirdParty?.accessToken)
        // The rotated credentials must actually be persisted — not just used for the in-flight request.
        val storedModelSlot = slot<TransactionTokenModel>()
        verify { storage.transactionTokenModel = capture(storedModelSlot) }
        assertEquals("fresh-access-token", storedModelSlot.captured.thirdParty?.accessToken)
        assertEquals("rotated-refresh-token", storedModelSlot.captured.thirdParty?.refreshToken)
        verify { storage.authToken = "fresh-access-token" }
        verify { storage.authTokenExpDate = refreshedThirdParty.expiresAt }
    }

    @Test
    fun `connect with ThirdPartyOAuth explicit flow persists refreshed credentials when implicit-grant fails`() =
        runTest {
        every { configuration.authenticationType } returns AuthenticationType.ThirdPartyOAuth
        every { chat.isImplicitOAuthFlow } returns false
        val storedThirdParty = ThirdParty(refreshToken = "stored-refresh-token", expiresIn = 0L)
        val expiredModel = TransactionTokenModel(transactionToken = null, thirdParty = storedThirdParty)
        every { storage.transactionTokenModel } returns expiredModel
        val visitorId = UUID.randomUUID()
        every { storage.visitorId } returns visitorId

        // Step 1 (refresh_token grant) succeeds and rotates the refresh token.
        val refreshedThirdParty = ThirdParty(accessToken = "fresh-access-token", refreshToken = "rotated-refresh-token", expiresIn = 3600L)
        val refreshResponseModel = mockk<TransactionTokenModel> {
            every { this@mockk.transactionToken } returns null
            every { this@mockk.thirdParty } returns refreshedThirdParty
        }
        val refreshResponse = mockk<retrofit2.Response<TransactionTokenModel>> {
            every { isSuccessful } returns true
            every { body() } returns refreshResponseModel
        }
        // Step 2 (implicit grant using the freshly rotated access token) fails.
        val implicitFailureResponse = mockk<retrofit2.Response<TransactionTokenModel>> {
            every { isSuccessful } returns false
            every { errorBody() } returns null
            every { code() } returns 503
            every { message() } returns "Service Unavailable"
        }
        every { authService.getTransactionToken(any(), any(), any(), any()) } returnsMany listOf(
            mockk { every { execute() } returns refreshResponse },
            mockk { every { execute() } returns implicitFailureResponse },
        )

        val result = connector.connect(mockk { every { thirdParty } returns null })

        assertTrue(result.isFailure)
        // The refresh's rotated refresh token must survive in storage even though the second step
        // failed — otherwise the next reconnect attempt retries with an already-invalidated one and
        // fails permanently.
        val storedModelSlot = slot<TransactionTokenModel>()
        verify { storage.transactionTokenModel = capture(storedModelSlot) }
        assertEquals("fresh-access-token", storedModelSlot.captured.thirdParty?.accessToken)
        assertEquals("rotated-refresh-token", storedModelSlot.captured.thirdParty?.refreshToken)
        verify { storage.authToken = "fresh-access-token" }
        verify { storage.authTokenExpDate = refreshedThirdParty.expiresAt }
    }

    @Test
    fun `connect with ThirdPartyOAuth explicit flow reuses a concurrently refreshed access token directly`() =
        runTest {
        every { configuration.authenticationType } returns AuthenticationType.ThirdPartyOAuth
        every { chat.isImplicitOAuthFlow } returns false
        val staleThirdParty = ThirdParty(refreshToken = "stale-refresh-token", expiresIn = 0L)
        val staleModel = TransactionTokenModel(transactionToken = null, thirdParty = staleThirdParty)
        // A concurrent TokenRefreshCoordinator refresh completes while this call waits for the lock.
        // storage.transactionTokenModel is read once by connect() (returns the stale model) and again
        // inside connectWithRefreshToken (returns the rotated one), simulating that interleaving.
        val rotatedThirdParty = ThirdParty(
            accessToken = "concurrently-refreshed-access-token",
            refreshToken = "concurrently-refreshed-refresh-token",
            expiresIn = 3600L,
        )
        val rotatedModel = TransactionTokenModel(transactionToken = null, thirdParty = rotatedThirdParty)
        every { storage.transactionTokenModel } returnsMany listOf(staleModel, rotatedModel)
        val visitorId = UUID.randomUUID()
        every { storage.visitorId } returns visitorId

        val finalModel = TransactionTokenModel(transactionToken = "new-tx-token", expiresIn = 3600L, customerIdentity = null)
        val capturedBodies = mutableListOf<TokenRequestBody>()
        every { authService.getTransactionToken(any(), any(), any(), capture(capturedBodies)) } returns mockk {
            every { execute() } returns mockk<retrofit2.Response<TransactionTokenModel>> {
                every { isSuccessful } returns true
                every { body() } returns finalModel
            }
        }
        every { socketFactory.create(any(), any(), any()) } returns webSocket

        val result = connector.connect(mockk { every { thirdParty } returns null })

        assertNull(result.exceptionOrNull())
        // Only the implicit-grant call happens — the refresh_token grant is never spent a second time.
        assertEquals(1, capturedBodies.size)
        assertEquals(GrantType.IMPLICIT, capturedBodies[0].thirdParty?.grantType)
        assertEquals("concurrently-refreshed-access-token", capturedBodies[0].thirdParty?.accessToken)
    }

    @Test
    fun `connect with ThirdPartyOAuth explicit flow falls back to refresh token when backend rejects the reused access token`() =
        runTest {
        every { configuration.authenticationType } returns AuthenticationType.ThirdPartyOAuth
        every { chat.isImplicitOAuthFlow } returns false
        val staleThirdParty = ThirdParty(refreshToken = "stale-refresh-token", expiresIn = 0L)
        val staleModel = TransactionTokenModel(transactionToken = null, thirdParty = staleThirdParty)
        // A concurrent TokenRefreshCoordinator refresh completes while this call waits for the lock,
        // same interleaving as the reuse-success test above — except the backend now rejects the
        // reused (locally-still-valid) access token, so the fallback must spend
        // concurrently-refreshed-refresh-token instead of dead-ending on AuthorizationError.
        val rotatedThirdParty = ThirdParty(
            accessToken = "concurrently-refreshed-access-token",
            refreshToken = "concurrently-refreshed-refresh-token",
            expiresIn = 3600L,
        )
        val rotatedModel = TransactionTokenModel(transactionToken = null, thirdParty = rotatedThirdParty)
        every { storage.transactionTokenModel } returnsMany listOf(staleModel, rotatedModel)
        val visitorId = UUID.randomUUID()
        every { storage.visitorId } returns visitorId

        // Step 1: implicit grant using the reused access token — backend rejects it (400).
        val rejectedResponse = mockk<retrofit2.Response<TransactionTokenModel>> {
            every { isSuccessful } returns false
            every { errorBody() } returns null
            every { code() } returns 400
            every { message() } returns "Bad Request"
        }
        // Step 2: refresh_token grant using the concurrently-rotated refresh token.
        val refreshedThirdParty =
            ThirdParty(accessToken = "fresh-access-token", refreshToken = "twice-rotated-refresh-token", expiresIn = 3600L)
        val refreshResponseModel = mockk<TransactionTokenModel> {
            every { this@mockk.transactionToken } returns null
            every { this@mockk.thirdParty } returns refreshedThirdParty
        }
        val refreshResponse = mockk<retrofit2.Response<TransactionTokenModel>> {
            every { isSuccessful } returns true
            every { body() } returns refreshResponseModel
        }
        // Step 3: implicit grant using the freshly refreshed access token, returns the real transaction token.
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
        assertEquals(3, capturedBodies.size)
        assertEquals(GrantType.IMPLICIT, capturedBodies[0].thirdParty?.grantType)
        assertEquals("concurrently-refreshed-access-token", capturedBodies[0].thirdParty?.accessToken)
        assertEquals(GrantType.REFRESH_TOKEN, capturedBodies[1].thirdParty?.grantType)
        assertEquals("concurrently-refreshed-refresh-token", capturedBodies[1].thirdParty?.refreshToken)
        assertEquals(GrantType.IMPLICIT, capturedBodies[2].thirdParty?.grantType)
        assertEquals("fresh-access-token", capturedBodies[2].thirdParty?.accessToken)
        // The rejected reused-token attempt is a recoverable retry, not a reported failure.
        verify(exactly = 0) { chatStateListener.onChatRuntimeException(any()) }
    }

    @Test
    fun `connect with ThirdPartyOAuth explicit flow reports failure when refresh call fails without attempting implicit grant`() = runTest {
        every { configuration.authenticationType } returns AuthenticationType.ThirdPartyOAuth
        every { chat.isImplicitOAuthFlow } returns false
        // A real instance (not a mock) since the connect() decision now derives the access
        // token's expiry from thirdParty.expiresAt rather than storage.authTokenExpDate.
        val storedThirdParty = ThirdParty(refreshToken = "stored-refresh-token", expiresIn = 0L)
        val expiredModel = mockk<TransactionTokenModel> {
            every { isExpired } returns true
            every { this@mockk.thirdParty } returns storedThirdParty
        }
        every { storage.transactionTokenModel } returns expiredModel

        val errorResponse = mockk<retrofit2.Response<TransactionTokenModel>> {
            every { isSuccessful } returns false
            every { errorBody() } returns null
            every { code() } returns 400
            every { message() } returns "Bad Request"
        }
        every { authService.getTransactionToken(any(), any(), any(), any()) } returns mockk {
            every { execute() } returns errorResponse
        }

        val result = connector.connect(mockk { every { thirdParty } returns null })

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuntimeChatException.InvalidRefreshTokenException)
        verify(exactly = 1) { authService.getTransactionToken(any(), any(), any(), any()) }
        verify { chatStateListener.onChatRuntimeException(any<RuntimeChatException.InvalidRefreshTokenException>()) }
    }

    @Test
    fun `connect with ThirdPartyOAuth explicit flow reports AuthorizationError when refresh call fails operationally`() = runTest {
        every { configuration.authenticationType } returns AuthenticationType.ThirdPartyOAuth
        every { chat.isImplicitOAuthFlow } returns false
        // A real instance (not a mock) since the connect() decision now derives the access
        // token's expiry from thirdParty.expiresAt rather than storage.authTokenExpDate.
        val storedThirdParty = ThirdParty(refreshToken = "stored-refresh-token", expiresIn = 0L)
        val expiredModel = mockk<TransactionTokenModel> {
            every { isExpired } returns true
            every { this@mockk.thirdParty } returns storedThirdParty
        }
        every { storage.transactionTokenModel } returns expiredModel

        val errorResponse = mockk<retrofit2.Response<TransactionTokenModel>> {
            every { isSuccessful } returns false
            every { errorBody() } returns null
            every { code() } returns 500
            every { message() } returns "Internal Server Error"
        }
        every { authService.getTransactionToken(any(), any(), any(), any()) } returns mockk {
            every { execute() } returns errorResponse
        }

        val result = connector.connect(mockk { every { thirdParty } returns null })

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuntimeChatException.AuthorizationError)
        verify(exactly = 1) { authService.getTransactionToken(any(), any(), any(), any()) }
        verify { chatStateListener.onChatRuntimeException(any<RuntimeChatException.AuthorizationError>()) }
    }

    @Test
    fun `connect with ThirdPartyOAuth explicit flow reports ConnectionTokenFailed when refresh response has no access token`() = runTest {
        every { configuration.authenticationType } returns AuthenticationType.ThirdPartyOAuth
        every { chat.isImplicitOAuthFlow } returns false
        // A real instance (not a mock) since the connect() decision now derives the access
        // token's expiry from thirdParty.expiresAt rather than storage.authTokenExpDate.
        val storedThirdParty = ThirdParty(refreshToken = "stored-refresh-token", expiresIn = 0L)
        val expiredModel = mockk<TransactionTokenModel> {
            every { isExpired } returns true
            every { this@mockk.thirdParty } returns storedThirdParty
        }
        every { storage.transactionTokenModel } returns expiredModel

        // A real instance (not a mock) since ChatSocketConnector's mergeThirdParty calls .copy() on it
        // when refreshToken is absent.
        val refreshedThirdPartyNoAccessToken = ThirdParty(accessToken = null, refreshToken = null, expiresIn = 3600L)
        val refreshResponseModel = mockk<TransactionTokenModel> {
            every { this@mockk.transactionToken } returns null
            every { this@mockk.thirdParty } returns refreshedThirdPartyNoAccessToken
        }
        val refreshResponse = mockk<retrofit2.Response<TransactionTokenModel>> {
            every { isSuccessful } returns true
            every { body() } returns refreshResponseModel
        }
        every { authService.getTransactionToken(any(), any(), any(), any()) } returns mockk {
            every { execute() } returns refreshResponse
        }

        val result = connector.connect(mockk { every { thirdParty } returns null })

        assertTrue(result.exceptionOrNull() is RuntimeChatException.ConnectionTokenFailed)
        verify { chatStateListener.onChatRuntimeException(any<RuntimeChatException.ConnectionTokenFailed>()) }
    }

    @Test
    fun `connect with ThirdPartyOAuth explicit flow and expired token with no refresh token reports ConnectionTokenFailed`() = runTest {
        every { configuration.authenticationType } returns AuthenticationType.ThirdPartyOAuth
        every { chat.isImplicitOAuthFlow } returns false
        // A real instance (not a mock) since the connect() decision now derives the access
        // token's expiry from thirdParty.expiresAt rather than storage.authTokenExpDate.
        val storedThirdParty = ThirdParty(refreshToken = null, expiresIn = 0L)
        val expiredModel = mockk<TransactionTokenModel> {
            every { isExpired } returns true
            every { this@mockk.thirdParty } returns storedThirdParty
        }
        every { storage.transactionTokenModel } returns expiredModel
        val result = connector.connect(mockk { every { thirdParty } returns null })
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuntimeChatException.ConnectionTokenFailed)
    }

    @Test
    fun `connect with ThirdPartyOAuth explicit flow notifies chatStateListener on ConnectionTokenFailed`() = runTest {
        every { configuration.authenticationType } returns AuthenticationType.ThirdPartyOAuth
        every { chat.isImplicitOAuthFlow } returns false
        // A real instance (not a mock) since the connect() decision now derives the access
        // token's expiry from thirdParty.expiresAt rather than storage.authTokenExpDate.
        val storedThirdParty = ThirdParty(refreshToken = null, expiresIn = 0L)
        val expiredModel = mockk<TransactionTokenModel> {
            every { isExpired } returns true
            every { this@mockk.thirdParty } returns storedThirdParty
        }
        every { storage.transactionTokenModel } returns expiredModel
        val result = connector.connect(mockk { every { thirdParty } returns null })
        assertTrue(result.exceptionOrNull() is RuntimeChatException.ConnectionTokenFailed)
        verify { chatStateListener.onChatRuntimeException(any<RuntimeChatException.ConnectionTokenFailed>()) }
    }

    @Test
    fun `connect with ThirdPartyOAuth explicit flow reports AuthorizationError on API error`() = runTest {
        val visitorId = UUID.randomUUID()
        val tokenRequestBody = mockk<TokenRequestBody> {
            every { thirdParty } returns mockk<ThirdPartyOAuthBody> {
                every { grantType } returns GrantType.AUTHORIZATION_CODE
            }
        }
        val response = mockk<retrofit2.Response<TransactionTokenModel>> {
            every { isSuccessful } returns false
            every { errorBody() } returns mockk {
                every { string() } returns """{"error":{"errorMessage":"Feature not available"}}"""
            }
            every { code() } returns 400
            every { message() } returns "Bad Request"
        }
        every { storage.visitorId } returns visitorId
        every { storage.transactionTokenModel } returns null
        every { authService.getTransactionToken(any(), any(), any(), any()) } returns mockk {
            every { execute() } returns response
        }
        val result = connector.connect(tokenRequestBody)
        assertTrue(result.exceptionOrNull() is RuntimeChatException.AuthorizationError)
        verify { chatStateListener.onChatRuntimeException(any<RuntimeChatException.AuthorizationError>()) }
    }
}
