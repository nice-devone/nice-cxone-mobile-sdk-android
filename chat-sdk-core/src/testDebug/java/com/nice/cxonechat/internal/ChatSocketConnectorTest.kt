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
import com.nice.cxonechat.exceptions.RuntimeChatException
import com.nice.cxonechat.internal.model.ConfigurationInternal
import com.nice.cxonechat.internal.model.TokenRequestBody
import com.nice.cxonechat.internal.model.TransactionTokenModel
import com.nice.cxonechat.internal.socket.SocketFactory
import com.nice.cxonechat.internal.socket.WebsocketLogging
import com.nice.cxonechat.state.Configuration
import com.nice.cxonechat.storage.ValueStorage
import io.mockk.every
import io.mockk.mockk
import okhttp3.WebSocket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date
import java.util.UUID

internal class ChatSocketConnectorTest {
    private lateinit var chat: ChatWithParameters
    private lateinit var socketFactory: SocketFactory
    private lateinit var chatStateListener: ChatStateListener
    private lateinit var connector: ChatSocketConnector
    private lateinit var webSocket: WebSocket
    private lateinit var logger: com.nice.cxonechat.log.Logger
    private lateinit var entrails: ChatEntrails
    private lateinit var storage: ValueStorage
    private lateinit var authService: AuthService
    private lateinit var configuration: ConfigurationInternal
    private lateinit var connection: com.nice.cxonechat.state.Connection

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
        every { configuration.hasFeature(Configuration.Feature.SecuredSessions) } returns false
        connector = ChatSocketConnector(chat, socketFactory, chatStateListener)
    }

    @Test
    fun `connect without secured sessions returns websocket only`() {
        val visitorId = UUID.randomUUID()
        every { storage.visitorId } returns visitorId
        every { socketFactory.create(any(), any()) } returns webSocket
        val result = connector.connect(null)
        // WebsocketLogging wraps the socket, but socket is private. Use isInstance and equality.
        assertTrue(result.webSocket is WebsocketLogging)
        val wsLogging = result.webSocket as WebsocketLogging
        // Use reflection to access private socket for test assertion
        val socketField = WebsocketLogging::class.java.getDeclaredField("socket")
        socketField.isAccessible = true
        assertEquals(webSocket, socketField.get(wsLogging))
        assertNull(result.transactionTokenModel?.thirdParty?.accessToken)
        assertNull(result.transactionTokenModel?.customerIdentity?.idOnExternalPlatform)
        assertNull(result.transactionTokenModel?.thirdParty?.expiresAt)
        assertNull(result.error)
    }

    @Test
    fun `connect with secured sessions and successful token returns all data`() {
        every { configuration.hasFeature(Configuration.Feature.SecuredSessions) } returns true
        val visitorId = UUID.randomUUID()
        val accessToken = "token-abc"
        val customerId = "customer-xyz"
        val expiresAt = Date()
        val tokenRequestBody = mockk<TokenRequestBody>()
        val customerIdentity = mockk<com.nice.cxonechat.internal.model.CustomerIdentityModel> {
            every { idOnExternalPlatform } returns customerId
        }
        val responseBody = mockk<TransactionTokenModel> {
            every { this@mockk.transactionToken } returns accessToken
            every { this@mockk.customerIdentity } returns customerIdentity
            every { this@mockk.expiresAt } returns expiresAt
        }
        val response = mockk<retrofit2.Response<TransactionTokenModel>>() {
            every { isSuccessful } returns true
            every { body() } returns responseBody
        }
        every { storage.visitorId } returns visitorId
        every { authService.getTransactionToken(any(), any(), any(), any()) } returns mockk {
            every { execute() } returns response
        }
        every { socketFactory.create(any(), any(), any()) } returns webSocket
        val result = connector.connect(tokenRequestBody)
        assertTrue(result.webSocket is WebsocketLogging)
        val wsLogging = result.webSocket as WebsocketLogging
        val socketField = WebsocketLogging::class.java.getDeclaredField("socket")
        socketField.isAccessible = true
        assertEquals(webSocket, socketField.get(wsLogging))
        assertNull(result.error)
    }

    @Test
    fun `connect with secured sessions and failed token returns error`() {
        every { configuration.hasFeature(Configuration.Feature.SecuredSessions) } returns true
        val visitorId = UUID.randomUUID()
        val tokenRequestBody = mockk<TokenRequestBody>()
        val response = mockk<retrofit2.Response<TransactionTokenModel>>() {
            every { isSuccessful } returns false
            every { errorBody() } returns mockk {
                every { string() } returns "{\"error\":{\"errorMessage\":\"Auth failed\"}}"
            }
            every { code() } returns 401
            every { message() } returns "Unauthorized"
        }
        every { storage.visitorId } returns visitorId
        every { storage.transactionTokenModel } returns null // Ensure error path is taken
        every { authService.getTransactionToken(any(), any(), any(), any()) } returns mockk {
            every { execute() } returns response
        }
        val result = connector.connect(tokenRequestBody)
        assertNull(result.webSocket)
        assertNull(result.transactionTokenModel)
        assertNotNull(result.error)
        assertTrue(result.error is RuntimeChatException.AuthorizationError)
    }

    @Test
    fun `connect with secured sessions and valid cached transaction token uses cached token`() {
        every { configuration.hasFeature(Configuration.Feature.SecuredSessions) } returns true
        val visitorId = UUID.randomUUID()
        val accessToken = "token-cached"
        val customerId = "customer-cached"
        val transactionTokenModel = mockk<TransactionTokenModel> {
            every { this@mockk.transactionToken } returns accessToken
            every { this@mockk.customerIdentity } returns mockk {
                every { idOnExternalPlatform } returns customerId
            }
            every { isExpired } returns false
        }
        every { storage.visitorId } returns visitorId
        every { storage.transactionTokenModel } returns transactionTokenModel
        every { socketFactory.create(any(), any(), any()) } returns webSocket
        val result = connector.connect(null)
        assertTrue(result.webSocket is WebsocketLogging)
        val wsLogging = result.webSocket as WebsocketLogging
        val socketField = WebsocketLogging::class.java.getDeclaredField("socket")
        socketField.isAccessible = true
        assertEquals(webSocket, socketField.get(wsLogging))
        assertEquals(transactionTokenModel, result.transactionTokenModel)
        assertNull(result.error)
    }
}
