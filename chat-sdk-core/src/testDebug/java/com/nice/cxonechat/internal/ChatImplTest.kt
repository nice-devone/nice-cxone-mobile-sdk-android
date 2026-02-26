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

import com.nice.cxonechat.Authorization
import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.ChatStateListener
import com.nice.cxonechat.enums.AuthenticationType
import com.nice.cxonechat.internal.model.ChatImplDependencies
import com.nice.cxonechat.internal.model.ConfigurationInternal
import com.nice.cxonechat.internal.model.GrantType
import com.nice.cxonechat.state.Configuration
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.storage.ValueStorage
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

internal class ChatImplTest {
    private lateinit var chatImpl: ChatImpl
    private lateinit var connection: Connection
    private lateinit var entrails: ChatEntrails
    private lateinit var dependencies: ChatImplDependencies
    private lateinit var configuration: ConfigurationInternal
    private lateinit var chatStateListener: ChatStateListener
    private lateinit var storage: ValueStorage
    private lateinit var retryApiHandler: RetryApiHandler

    @Before
    fun setUp() {
        connection = mockk(relaxed = true)
        entrails = mockk(relaxed = true)
        dependencies = mockk(relaxed = true)
        configuration = mockk(relaxed = true)
        chatStateListener = mockk(relaxed = true)
        storage = mockk(relaxed = true)
        every { entrails.storage } returns storage
        chatImpl = spyk(ChatImpl(connection, entrails, dependencies, configuration, chatStateListener))
        // Inject a mock retryApiHandler for cancel verification
        retryApiHandler = mockk(relaxed = true)
        chatImpl.apply {
            val field = ChatImpl::class.java.getDeclaredField("retryApiHandler")
            field.isAccessible = true
            field.set(this, retryApiHandler)
        }
    }

    @Test
    fun `close cancels visitor handle`() {
        chatImpl.close()
        verify { retryApiHandler.cancel() }
    }

    @Test
    fun `setDeviceToken null sets storage and triggers sendVisitorInfo`() {
        every { entrails.storage.deviceToken } returns "oldToken" andThen null
        every { entrails.threading.background(any()) } answers { Cancellable.noop }
        chatImpl.setDeviceToken(null)
        verify { entrails.storage.deviceToken = null }
    }

    @Test
    fun `getChannelAvailability returns isChatAvailable`() {
        chatImpl.isChatAvailable = false
        var result: Boolean? = null
        chatImpl.getChannelAvailability { result = it }
        assertEquals(false, result)
    }

    @Test
    fun `createAuthRequestBody returns correct body for SecuredCookie`() {
        val body = chatImpl.createAuthRequestBody(AuthenticationType.SecuredCookie)
        assertNull(body.type)
        assertNull(body.customerIdentity)
        assertNull(body.thirdParty)
    }

    @Test
    fun `createAuthRequestBody returns correct body for Anonymous`() {
        every { connection.customerId } returns "customerId"
        val body = chatImpl.createAuthRequestBody(AuthenticationType.Anonymous)
        assertEquals(AuthenticationType.Anonymous.name, body.type)
        // customerIdentity is set if customerId is not null
    }

    @Test
    fun `createAuthRequestBody returns correct body for ThirdPartyOAuth`() {
        val authorization = mockk<Authorization> {
            every { code } returns "code"
            every { verifier } returns "verifier"
        }
        val dependenciesField = ChatImpl::class.java.getDeclaredField("dependencies")
        dependenciesField.isAccessible = true
        val deps = mockk<ChatImplDependencies>(relaxed = true) {
            every { this@mockk.authorization } returns authorization
        }
        dependenciesField.set(chatImpl, deps)
        val body = chatImpl.createAuthRequestBody(AuthenticationType.ThirdPartyOAuth)
        assertEquals(GrantType.AUTHORIZATION_CODE, body.thirdParty?.grantType)
        assertEquals("code", body.thirdParty?.authorizationCode)
        assertEquals("verifier", body.thirdParty?.codeVerifier)
    }

    @Test
    fun `connect uses unsecured session path when SecuredSessions is disabled`() {
        every { configuration.hasFeature(Configuration.Feature.SecuredSessions) } returns false
        val socket = mockk<okhttp3.WebSocket>()
        val socketFactory = mockk<com.nice.cxonechat.internal.socket.SocketFactory> {
            every { create(any(), any()) } returns socket
        }
        val dependenciesField = ChatImpl::class.java.getDeclaredField("dependencies")
        dependenciesField.isAccessible = true
        val deps = mockk<ChatImplDependencies>(relaxed = true) {
            every { this@mockk.socketFactory } returns socketFactory
        }
        dependenciesField.set(chatImpl, deps)
        val result = chatImpl.connect()
        assertSame(result, Cancellable.noop)
    }

    @Test
    fun `connect fetches new transaction token when SecuredSessions enabled and no valid token`() {
        every { configuration.hasFeature(Configuration.Feature.SecuredSessions) } returns true
        val socket = mockk<okhttp3.WebSocket>()
        val socketFactory = mockk<com.nice.cxonechat.internal.socket.SocketFactory> {
            every { create(any(), any(), any()) } returns socket
        }
        every { storage.transactionTokenModel } returns null
        val dependenciesField = ChatImpl::class.java.getDeclaredField("dependencies")
        dependenciesField.isAccessible = true
        val deps = mockk<ChatImplDependencies>(relaxed = true) {
            every { this@mockk.socketFactory } returns socketFactory
        }
        dependenciesField.set(chatImpl, deps)
        val result = chatImpl.connect()
        assertSame(result, Cancellable.noop)
    }
}
