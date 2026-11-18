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

import com.nice.cxonechat.OAuthToken
import com.nice.cxonechat.TokenDelegateListener
import com.nice.cxonechat.TokenRequestReason
import com.nice.cxonechat.TokenRequestReason.TOKEN_INVALID
import com.nice.cxonechat.enums.AuthenticationType
import com.nice.cxonechat.exceptions.RuntimeChatException
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
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

/**
 * ThirdPartyOAuth **implicit** connection flow of [ChatSocketConnector]: delegate invocation,
 * two-step reconnect, and error reporting. Explicit flow lives in
 * [ChatSocketConnectorExplicitFlowTest]; the cached/basic flow lives in [ChatSocketConnectorTest].
 */
internal class ChatSocketConnectorOAuthTest : AbstractChatSocketConnectorTest() {

    @Test
    fun `connect with ThirdPartyOAuth implicit flow uses provided token body`() = runTest {
        every { configuration.authenticationType } returns AuthenticationType.ThirdPartyOAuth
        val visitorId = UUID.randomUUID()
        val transactionToken = "transaction-token"
        val tokenRequestBody = mockk<TokenRequestBody> {
            every { thirdParty } returns null
        }
        every { storage.visitorId } returns visitorId
        every { storage.transactionTokenModel } returns null
        val responseBody = mockk<TransactionTokenModel> {
            every { this@mockk.transactionToken } returns transactionToken
            every { this@mockk.customerIdentity } returns null
            every { this@mockk.isExpired } returns false
            every { this@mockk.expiresAt } returns Clock.System.now() + 1.hours
        }
        val response = mockk<retrofit2.Response<TransactionTokenModel>> {
            every { isSuccessful } returns true
            every { body() } returns responseBody
        }
        every { authService.getTransactionToken(any(), any(), any(), any()) } returns mockk {
            every { execute() } returns response
        }
        every { socketFactory.create(any(), any(), any()) } returns webSocket
        val result = connector.connect(tokenRequestBody)
        assertNull(result.exceptionOrNull())
        assertTrue(result.getOrThrow().webSocket is WebsocketLogging)
    }

    @Test
    fun `connect with ThirdPartyOAuth implicit flow and expired token calls delegate with TOKEN_INVALID`() = runTest {
        every { configuration.authenticationType } returns AuthenticationType.ThirdPartyOAuth
        every { chat.isImplicitOAuthFlow } returns true
        val expiredModel = mockk<TransactionTokenModel> { every { isExpired } returns true }
        every { storage.transactionTokenModel } returns expiredModel
        val visitorId = UUID.randomUUID()
        every { storage.visitorId } returns visitorId
        val jwt = "refreshed-jwt"
        var capturedReason: TokenRequestReason? = null
        val delegate = TokenDelegateListener { reason ->
            capturedReason = reason
            OAuthToken(jwt)
        }
        every { chat.tokenDelegateListener } returns delegate
        val newModel = mockk<TransactionTokenModel> {
            every { this@mockk.transactionToken } returns "new-tx-token"
            every { this@mockk.customerIdentity } returns null
            every { this@mockk.isExpired } returns false
            every { this@mockk.expiresAt } returns Clock.System.now() + 1.hours
        }
        // Step 1 must return 400 to trigger the delegate path; step 2 returns success
        val rejectionResponse = mockk<retrofit2.Response<TransactionTokenModel>> {
            every { isSuccessful } returns false
            every { errorBody() } returns null
            every { code() } returns 400
            every { message() } returns "Bad Request"
        }
        val successResponse = mockk<retrofit2.Response<TransactionTokenModel>> {
            every { isSuccessful } returns true
            every { body() } returns newModel
        }
        every { authService.getTransactionToken(any(), any(), any(), any()) } returnsMany listOf(
            mockk { every { execute() } returns rejectionResponse },
            mockk { every { execute() } returns successResponse },
        )
        every { socketFactory.create(any(), any(), any()) } returns webSocket
        val result = connector.connect(mockk { every { thirdParty } returns null })
        val payload = result.getOrThrow()
        assertTrue(payload.webSocket is WebsocketLogging)
        assertEquals(TOKEN_INVALID, capturedReason)
        assertEquals(jwt, payload.refreshedOAuthToken?.accessToken)
    }

    @Test
    fun `connect with ThirdPartyOAuth implicit flow and backend rejection reports InvalidAccessTokenException`() = runTest {
        every { configuration.authenticationType } returns AuthenticationType.ThirdPartyOAuth
        every { chat.isImplicitOAuthFlow } returns true
        val expiredModel = mockk<TransactionTokenModel> { every { isExpired } returns true }
        every { storage.transactionTokenModel } returns expiredModel
        val visitorId = UUID.randomUUID()
        every { storage.visitorId } returns visitorId
        val delegate = TokenDelegateListener { OAuthToken("stale-jwt") }
        every { chat.tokenDelegateListener } returns delegate
        val response = mockk<retrofit2.Response<TransactionTokenModel>> {
            every { isSuccessful } returns false
            every { errorBody() } returns mockk {
                every { string() } returns """{"error":{"errorMessage":"Invalid JWT"}}"""
            }
            every { code() } returns 400
            every { message() } returns "Bad Request"
        }
        every { authService.getTransactionToken(any(), any(), any(), any()) } returns mockk {
            every { execute() } returns response
        }
        val result = connector.connect(mockk { every { thirdParty } returns null })
        assertTrue(result.exceptionOrNull() is RuntimeChatException.InvalidAccessTokenException)
        verify { chatStateListener.onChatRuntimeException(any<RuntimeChatException.InvalidAccessTokenException>()) }
    }

    @Test
    fun `connect with ThirdPartyOAuth implicit flow and delegate failure reports TokenDelegationFailedException`() = runTest {
        every { configuration.authenticationType } returns AuthenticationType.ThirdPartyOAuth
        every { chat.isImplicitOAuthFlow } returns true
        val expiredModel = mockk<TransactionTokenModel> { every { isExpired } returns true }
        every { storage.transactionTokenModel } returns expiredModel
        val delegate = TokenDelegateListener { throw RuntimeChatException.TokenDelegationFailedException("user cancelled") }
        every { chat.tokenDelegateListener } returns delegate
        // Step 1 must return a 400 to trigger the delegate path
        val response = mockk<retrofit2.Response<TransactionTokenModel>> {
            every { isSuccessful } returns false
            every { errorBody() } returns null
            every { code() } returns 400
            every { message() } returns "Bad Request"
        }
        every { authService.getTransactionToken(any(), any(), any(), any()) } returns mockk {
            every { execute() } returns response
        }
        val result = connector.connect(mockk { every { thirdParty } returns null })
        assertTrue(result.exceptionOrNull() is RuntimeChatException.TokenDelegationFailedException)
        verify { chatStateListener.onChatRuntimeException(any<RuntimeChatException.TokenDelegationFailedException>()) }
    }

    @Test
    fun `connect with ThirdPartyOAuth implicit flow succeeds on first attempt without calling delegate`() = runTest {
        every { configuration.authenticationType } returns AuthenticationType.ThirdPartyOAuth
        every { chat.isImplicitOAuthFlow } returns true
        val expiredModel = mockk<TransactionTokenModel> { every { isExpired } returns true }
        every { storage.transactionTokenModel } returns expiredModel
        val visitorId = UUID.randomUUID()
        every { storage.visitorId } returns visitorId
        val newModel = mockk<TransactionTokenModel> {
            every { this@mockk.transactionToken } returns "tx-token"
            every { this@mockk.customerIdentity } returns null
            every { this@mockk.isExpired } returns false
            every { this@mockk.expiresAt } returns Clock.System.now() + 1.hours
        }
        val successResponse = mockk<retrofit2.Response<TransactionTokenModel>> {
            every { isSuccessful } returns true
            every { body() } returns newModel
        }
        every { authService.getTransactionToken(any(), any(), any(), any()) } returns mockk {
            every { execute() } returns successResponse
        }
        every { socketFactory.create(any(), any(), any()) } returns webSocket
        // Delegate should NOT be invoked since first attempt succeeds
        every { chat.tokenDelegateListener } returns TokenDelegateListener { error("should not be called") }

        val result = connector.connect(mockk { every { thirdParty } returns null })
        val payload = result.getOrThrow()
        assertTrue(payload.webSocket is WebsocketLogging)
        assertNull(payload.refreshedOAuthToken)
    }

    @Test
    fun `connect with ThirdPartyOAuth implicit flow closes socket without crashing on a non-RuntimeChatException failure`() = runTest {
        every { configuration.authenticationType } returns AuthenticationType.ThirdPartyOAuth
        every { chat.isImplicitOAuthFlow } returns true
        val expiredModel = mockk<TransactionTokenModel> { every { isExpired } returns true }
        every { storage.transactionTokenModel } returns expiredModel
        val visitorId = UUID.randomUUID()
        every { storage.visitorId } returns visitorId
        val newModel = mockk<TransactionTokenModel> {
            every { this@mockk.transactionToken } returns "tx-token"
            every { this@mockk.customerIdentity } returns null
            every { this@mockk.isExpired } returns false
            every { this@mockk.expiresAt } returns Clock.System.now() + 1.hours
        }
        val successResponse = mockk<retrofit2.Response<TransactionTokenModel>> {
            every { isSuccessful } returns true
            every { body() } returns newModel
        }
        every { authService.getTransactionToken(any(), any(), any(), any()) } returns mockk {
            every { execute() } returns successResponse
        }
        val socketListener = mockk<ProxyWebSocketListener>(relaxed = true)
        every { chat.socketListener } returns socketListener
        every { socketFactory.create(any(), any(), any()) } throws IllegalStateException("socket setup failed")
        // Delegate should NOT be invoked — a non-400 (in fact, non-network) failure must not trigger it
        every { chat.tokenDelegateListener } returns TokenDelegateListener { error("should not be called") }

        val result = connector.connect(mockk { every { thirdParty } returns null })

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
        verify { socketListener.reportState(SocketState.CLOSED) }
        verify(exactly = 0) { chatStateListener.onChatRuntimeException(any()) }
    }

    @Test
    fun `connect with ThirdPartyOAuth implicit flow reports error immediately on non-400 failure`() = runTest {
        every { configuration.authenticationType } returns AuthenticationType.ThirdPartyOAuth
        every { chat.isImplicitOAuthFlow } returns true
        val expiredModel = mockk<TransactionTokenModel> { every { isExpired } returns true }
        every { storage.transactionTokenModel } returns expiredModel
        val visitorId = UUID.randomUUID()
        every { storage.visitorId } returns visitorId
        // 500 is not a token rejection — should not invoke delegate
        val errorResponse = mockk<retrofit2.Response<TransactionTokenModel>> {
            every { isSuccessful } returns false
            every { errorBody() } returns null
            every { code() } returns 500
            every { message() } returns "Internal Server Error"
        }
        every { authService.getTransactionToken(any(), any(), any(), any()) } returns mockk {
            every { execute() } returns errorResponse
        }
        every { chat.tokenDelegateListener } returns TokenDelegateListener { error("should not be called") }

        val result = connector.connect(mockk { every { thirdParty } returns null })
        assertNull(result.getOrNull())
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuntimeChatException.AuthorizationError)
        verify { chatStateListener.onChatRuntimeException(any<RuntimeChatException.AuthorizationError>()) }
    }
}
