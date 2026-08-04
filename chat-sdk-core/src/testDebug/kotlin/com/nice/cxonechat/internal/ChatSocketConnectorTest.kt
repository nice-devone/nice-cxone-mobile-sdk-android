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
import com.nice.cxonechat.OAuthToken
import com.nice.cxonechat.TokenDelegateListener
import com.nice.cxonechat.TokenRequestReason
import com.nice.cxonechat.TokenRequestReason.TOKEN_INVALID
import com.nice.cxonechat.api.AuthService
import com.nice.cxonechat.enums.AuthenticationType
import com.nice.cxonechat.exceptions.RuntimeChatException
import com.nice.cxonechat.exceptions.RuntimeChatException.FeatureUnavailableException
import com.nice.cxonechat.internal.model.ConfigurationInternal
import com.nice.cxonechat.internal.model.CustomerIdentityModel
import com.nice.cxonechat.internal.model.GrantType
import com.nice.cxonechat.internal.model.ThirdParty
import com.nice.cxonechat.internal.model.ThirdPartyOAuthBody
import com.nice.cxonechat.internal.model.TokenRequestBody
import com.nice.cxonechat.internal.model.TransactionTokenModel
import com.nice.cxonechat.internal.socket.ProxyWebSocketListener
import com.nice.cxonechat.internal.socket.SocketFactory
import com.nice.cxonechat.internal.socket.SocketState
import com.nice.cxonechat.internal.socket.WebsocketLogging
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.storage.ValueStorage
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.WebSocket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.UUID
import kotlin.time.Clock

internal class ChatSocketConnectorTest {
    private lateinit var chat: ChatWithParameters
    private lateinit var socketFactory: SocketFactory
    private lateinit var chatStateListener: ChatStateListener
    private lateinit var connector: ChatSocketConnector
    private lateinit var webSocket: WebSocket
    private lateinit var logger: Logger
    private lateinit var entrails: ChatEntrails
    private lateinit var storage: ValueStorage
    private lateinit var authService: AuthService
    private lateinit var configuration: ConfigurationInternal
    private lateinit var connection: Connection

    @Before
    fun setUp() {
        chat = mockk(relaxed = true)
        socketFactory = mockk(relaxed = true)
        chatStateListener = mockk(relaxed = true)
        webSocket = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        entrails = mockk(relaxed = true)
        storage = mockk(relaxed = true)
        authService = mockk(relaxed = true)
        configuration = mockk(relaxed = true)
        connection = mockk(relaxed = true)

        every { chat.configuration } returns configuration
        every { chat.connection } returns connection
        every { chat.entrails } returns entrails
        every { chat.socketListener } returns mockk(relaxed = true)
        every { entrails.storage } returns storage
        every { entrails.logger } returns logger
        every { entrails.authService } returns authService
        connector = ChatSocketConnector(chat, socketFactory, chatStateListener)
    }

    @Test
    fun `connect with no cached token fetches new transaction token`() {
        val visitorId = UUID.randomUUID()
        val tokenRequestBody = mockk<TokenRequestBody> {
            every { thirdParty } returns null
        }
        val responseBody = mockk<TransactionTokenModel> {
            every { this@mockk.transactionToken } returns "tx-token"
            every { this@mockk.customerIdentity } returns null
            every { this@mockk.isExpired } returns false
        }
        val response = mockk<retrofit2.Response<TransactionTokenModel>> {
            every { isSuccessful } returns true
            every { body() } returns responseBody
        }
        every { storage.visitorId } returns visitorId
        every { storage.transactionTokenModel } returns null
        every { authService.getTransactionToken(any(), any(), any(), any()) } returns mockk {
            every { execute() } returns response
        }
        every { socketFactory.create(any(), any()) } returns webSocket
        val result = connector.connect(tokenRequestBody)
        assertTrue(result.isSuccess)
        val payload = result.getOrThrow()
        // WebsocketLogging wraps the socket, but socket is private. Use isInstance and equality.
        assertTrue(payload.webSocket is WebsocketLogging)
        // Use reflection to access private socket for test assertion
        val socketField = WebsocketLogging::class.java.getDeclaredField("socket")
        socketField.isAccessible = true
        assertNotNull(payload.transactionTokenModel?.transactionToken)
        assertNull(payload.transactionTokenModel?.customerIdentity)
        assertTrue(payload.transactionTokenModel?.isExpired == false)
    }

    @Test
    fun `connect with successful token returns all data`() {
        val visitorId = UUID.randomUUID()
        val accessToken = "token-abc"
        val customerId = "customer-xyz"
        val expiresAt = Clock.System.now()
        val tokenRequestBody = mockk<TokenRequestBody>()
        val customerIdentity = mockk<CustomerIdentityModel> {
            every { idOnExternalPlatform } returns customerId
        }
        val responseBody = mockk<TransactionTokenModel> {
            every { this@mockk.transactionToken } returns accessToken
            every { this@mockk.customerIdentity } returns customerIdentity
            every { this@mockk.expiresAt } returns expiresAt
        }
        val response = mockk<retrofit2.Response<TransactionTokenModel>> {
            every { isSuccessful } returns true
            every { body() } returns responseBody
        }
        every { storage.visitorId } returns visitorId
        every { authService.getTransactionToken(any(), any(), any(), any()) } returns mockk {
            every { execute() } returns response
        }
        every { socketFactory.create(any(), any(), any()) } returns webSocket
        val result = connector.connect(tokenRequestBody)
        assertTrue(result.isSuccess)
        val payload = result.getOrThrow()
        assertTrue(payload.webSocket is WebsocketLogging)
        val wsLogging = payload.webSocket as WebsocketLogging
        val socketField = WebsocketLogging::class.java.getDeclaredField("socket")
        socketField.isAccessible = true
        assertEquals(webSocket, socketField.get(wsLogging))
    }

    @Test
    fun `connect with failed token returns error`() {
        val visitorId = UUID.randomUUID()
        val tokenRequestBody = mockk<TokenRequestBody> {
            every { thirdParty } returns null // non-explicit flow — expects AuthorizationError
        }
        val response = mockk<retrofit2.Response<TransactionTokenModel>> {
            every { isSuccessful } returns false
            every { errorBody() } returns mockk {
                every { string() } returns "{\"error\":{\"errorMessage\":\"Auth failed\"}}"
            }
            every { code() } returns 401
            every { message() } returns "Unauthorized"
        }
        every { storage.visitorId } returns visitorId
        every { storage.transactionTokenModel } returns null
        every { authService.getTransactionToken(any(), any(), any(), any()) } returns mockk {
            every { execute() } returns response
        }
        val result = connector.connect(tokenRequestBody)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuntimeChatException.AuthorizationError)
    }

    @Test
    fun `connect with ThirdPartyOAuth implicit flow uses provided token body`() {
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
    fun `connect with ThirdPartyOAuth explicit flow and expired token uses refresh token`() {
        every { configuration.authenticationType } returns AuthenticationType.ThirdPartyOAuth
        every { chat.isImplicitOAuthFlow } returns false
        val storedThirdParty = mockk<ThirdParty> {
            every { refreshToken } returns "stored-refresh-token"
        }
        val expiredModel = mockk<TransactionTokenModel> {
            every { isExpired } returns true
            every { this@mockk.thirdParty } returns storedThirdParty
        }
        every { storage.transactionTokenModel } returns expiredModel
        val visitorId = UUID.randomUUID()
        every { storage.visitorId } returns visitorId
        val newModel = mockk<TransactionTokenModel> {
            every { this@mockk.transactionToken } returns "new-tx-token"
            every { this@mockk.customerIdentity } returns null
            every { this@mockk.isExpired } returns false
        }
        val response = mockk<retrofit2.Response<TransactionTokenModel>> {
            every { isSuccessful } returns true
            every { body() } returns newModel
        }
        every { authService.getTransactionToken(any(), any(), any(), any()) } returns mockk {
            every { execute() } returns response
        }
        every { socketFactory.create(any(), any(), any()) } returns webSocket
        val result = connector.connect(mockk { every { thirdParty } returns null })
        assertNull(result.exceptionOrNull())
        assertTrue(result.getOrThrow().webSocket is WebsocketLogging)
    }

    @Test
    fun `connect with ThirdPartyOAuth explicit flow and expired token with no refresh token reports ConnectionTokenFailed`() {
        every { configuration.authenticationType } returns AuthenticationType.ThirdPartyOAuth
        every { chat.isImplicitOAuthFlow } returns false
        val storedThirdParty = mockk<ThirdParty> {
            every { refreshToken } returns null
        }
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
    fun `connect with ThirdPartyOAuth implicit flow and expired token calls delegate with TOKEN_INVALID`() {
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
        }
        // Step 1 must return 401 to trigger the delegate path; step 2 returns success
        val rejectionResponse = mockk<retrofit2.Response<TransactionTokenModel>> {
            every { isSuccessful } returns false
            every { errorBody() } returns null
            every { code() } returns 401
            every { message() } returns "Unauthorized"
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
    fun `connect with ThirdPartyOAuth implicit flow and backend rejection reports InvalidAccessTokenException`() {
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
                every { string() } returns "{\"error\":{\"errorMessage\":\"Invalid JWT\"}}"
            }
            every { code() } returns 401
            every { message() } returns "Unauthorized"
        }
        every { authService.getTransactionToken(any(), any(), any(), any()) } returns mockk {
            every { execute() } returns response
        }
        val result = connector.connect(mockk { every { thirdParty } returns null })
        assertTrue(result.exceptionOrNull() is RuntimeChatException.InvalidAccessTokenException)
        verify { chatStateListener.onChatRuntimeException(any<RuntimeChatException.InvalidAccessTokenException>()) }
    }

    @Test
    fun `connect with ThirdPartyOAuth implicit flow and delegate failure reports TokenDelegationFailedException`() {
        every { configuration.authenticationType } returns AuthenticationType.ThirdPartyOAuth
        every { chat.isImplicitOAuthFlow } returns true
        val expiredModel = mockk<TransactionTokenModel> { every { isExpired } returns true }
        every { storage.transactionTokenModel } returns expiredModel
        val delegate = TokenDelegateListener { throw RuntimeChatException.TokenDelegationFailedException("user cancelled") }
        every { chat.tokenDelegateListener } returns delegate
        // Step 1 must return a 401 to trigger the delegate path
        val response = mockk<retrofit2.Response<TransactionTokenModel>> {
            every { isSuccessful } returns false
            every { errorBody() } returns null
            every { code() } returns 401
            every { message() } returns "Unauthorized"
        }
        every { authService.getTransactionToken(any(), any(), any(), any()) } returns mockk {
            every { execute() } returns response
        }
        val result = connector.connect(mockk { every { thirdParty } returns null })
        assertTrue(result.exceptionOrNull() is RuntimeChatException.TokenDelegationFailedException)
        verify { chatStateListener.onChatRuntimeException(any<RuntimeChatException.TokenDelegationFailedException>()) }
    }

    @Test
    fun `connect with ThirdPartyOAuth explicit flow notifies chatStateListener on ConnectionTokenFailed`() {
        every { configuration.authenticationType } returns AuthenticationType.ThirdPartyOAuth
        every { chat.isImplicitOAuthFlow } returns false
        val storedThirdParty = mockk<ThirdParty> {
            every { refreshToken } returns null
        }
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
    fun `connect with ThirdPartyOAuth explicit flow reports FeatureUnavailableException on API error`() {
        val visitorId = UUID.randomUUID()
        val tokenRequestBody = mockk<TokenRequestBody> {
            every { thirdParty } returns mockk<ThirdPartyOAuthBody> {
                every { grantType } returns GrantType.AUTHORIZATION_CODE
            }
        }
        val response = mockk<retrofit2.Response<TransactionTokenModel>> {
            every { isSuccessful } returns false
            every { errorBody() } returns mockk {
                every { string() } returns "{\"error\":{\"errorMessage\":\"Feature not available\"}}"
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
        assertTrue(result.exceptionOrNull() is FeatureUnavailableException)
        verify { chatStateListener.onChatRuntimeException(any<FeatureUnavailableException>()) }
    }

    @Test
    fun `connect with ThirdPartyOAuth implicit flow succeeds on first attempt without calling delegate`() {
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
    fun `connect with ThirdPartyOAuth implicit flow reports error immediately on non-401 failure`() {
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

    @Test
    fun `connect with valid cached transaction token uses cached token`() {
        val visitorId = UUID.randomUUID()
        val accessToken = "token-cached"
        val customerId = "customer-cached"
        val transactionTokenModel = mockk<TransactionTokenModel> {
            every { this@mockk.transactionToken } returns accessToken
            every { this@mockk.customerIdentity } returns mockk {
                every { idOnExternalPlatform } returns customerId
            }
            every { isExpired } returns false
            every { thirdParty } returns null
        }
        every { storage.visitorId } returns visitorId
        every { storage.transactionTokenModel } returns transactionTokenModel
        every { socketFactory.create(any(), any(), any()) } returns webSocket
        val result = connector.connect(TokenRequestBody())
        assertTrue(result.isSuccess)
        val payload = result.getOrThrow()
        assertTrue(payload.webSocket is WebsocketLogging)
        val wsLogging = payload.webSocket as WebsocketLogging
        val socketField = WebsocketLogging::class.java.getDeclaredField("socket")
        socketField.isAccessible = true
        assertEquals(webSocket, socketField.get(wsLogging))
        assertEquals(transactionTokenModel, payload.transactionTokenModel)
    }

    @Test
    fun `connect returns failure and reports socket closed when socketFactory throws`() {
        val visitorId = UUID.randomUUID()
        val transactionTokenModel = mockk<TransactionTokenModel> {
            every { this@mockk.transactionToken } returns "token-cached"
            every { this@mockk.customerIdentity } returns null
            every { isExpired } returns false
            every { thirdParty } returns null
        }
        val socketListener = mockk<ProxyWebSocketListener>(relaxed = true)
        every { chat.socketListener } returns socketListener
        every { storage.visitorId } returns visitorId
        every { storage.transactionTokenModel } returns transactionTokenModel
        every { socketFactory.create(any(), any(), any()) } throws IOException("connection refused")
        val result = connector.connect(TokenRequestBody())
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IOException)
        verify { socketListener.reportState(SocketState.CLOSED) }
    }
}
