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

import com.nice.cxonechat.exceptions.RuntimeChatException
import com.nice.cxonechat.internal.model.CustomerIdentityModel
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.UUID
import kotlin.time.Clock

/**
 * Cached/basic transaction-token connection flow of [ChatSocketConnector] plus socket-creation
 * failure. The ThirdPartyOAuth flows live in [ChatSocketConnectorOAuthTest].
 */
internal class ChatSocketConnectorTest : AbstractChatSocketConnectorTest() {

    @Test
    fun `connect with no cached token fetches new transaction token`() = runTest {
        val visitorId = UUID.randomUUID()
        val tokenRequestBody = mockk<TokenRequestBody> {
            every { thirdParty } returns null
        }
        val responseBody = mockk<TransactionTokenModel> {
            every { this@mockk.transactionToken } returns "tx-token"
            every { this@mockk.customerIdentity } returns null
            every { this@mockk.isExpired } returns false
            every { this@mockk.expiresAt } returns Clock.System.now()
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
    fun `connect with successful token returns all data`() = runTest {
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
    fun `connect with failed token returns error`() = runTest {
        val visitorId = UUID.randomUUID()
        val tokenRequestBody = mockk<TokenRequestBody> {
            every { thirdParty } returns null // non-explicit flow — expects AuthorizationError
        }
        val response = mockk<retrofit2.Response<TransactionTokenModel>> {
            every { isSuccessful } returns false
            every { errorBody() } returns mockk {
                every { string() } returns """{"error":{"errorMessage":"Auth failed"}}"""
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
    fun `connect with successful response missing a transaction token returns failure instead of an unauthenticated socket`() = runTest {
        val visitorId = UUID.randomUUID()
        val tokenRequestBody = mockk<TokenRequestBody> {
            every { thirdParty } returns null
        }
        val responseBody = mockk<TransactionTokenModel> {
            every { this@mockk.transactionToken } returns null
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
        val result = connector.connect(tokenRequestBody)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuntimeChatException.AuthorizationError)
        verify(exactly = 0) { socketFactory.create(any(), any(), any()) }
    }

    @Test
    fun `connect with successful response missing an expiry returns failure instead of an immediately-expired socket`() = runTest {
        // expiresIn is nullable so refresh-grant responses (only thirdParty populated) can
        // deserialize, but a transaction-token-issuing response with no expiry would otherwise open
        // the socket and store a model whose isExpired is immediately true, forcing an unnecessary
        // reconnect right after a successful connect.
        val visitorId = UUID.randomUUID()
        val tokenRequestBody = mockk<TokenRequestBody> {
            every { thirdParty } returns null
        }
        val responseBody = mockk<TransactionTokenModel> {
            every { this@mockk.transactionToken } returns "tx-token"
            every { this@mockk.expiresAt } returns null
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
        val result = connector.connect(tokenRequestBody)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuntimeChatException.AuthorizationError)
        verify(exactly = 0) { socketFactory.create(any(), any(), any()) }
    }

    @Test
    fun `connect with valid cached transaction token uses cached token`() = runTest {
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
    fun `connect returns failure and reports socket closed when socketFactory throws`() = runTest {
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
