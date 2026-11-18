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
import com.nice.cxonechat.OAuthToken
import com.nice.cxonechat.TokenRequestReason
import com.nice.cxonechat.enums.AuthenticationType
import com.nice.cxonechat.internal.model.ChatImplDependencies
import com.nice.cxonechat.internal.model.GrantType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Auth-request-body construction (per [AuthenticationType]) and the [ChatImpl.isImplicitOAuthFlow]
 * property. Connection flow lives in [ChatImplConnectTest]; state emission in
 * [ChatImplStateFlowTest]; close/signOut lifecycle in [ChatImplLifecycleTest].
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatImplTest : AbstractChatImplTest() {

    @Test
    fun `setDeviceToken null sets storage and triggers sendVisitorInfo`() {
        every { entrails.storage.deviceToken } returns "oldToken" andThen null
        chatImpl.setDeviceToken(null)
        verify { entrails.storage.deviceToken = null }
    }

    @Test
    fun `getChannelAvailability returns isChatAvailable`() = runTest {
        chatImpl.isChatAvailable = false
        val result = chatImpl.getChannelAvailability()
        assertEquals(false, result)
    }

    @Test
    fun `createAuthRequestBody returns correct body for SecuredCookie`() {
        val body = chatImpl.createAuthRequestBody(AuthenticationType.SecuredCookie)
        assertNull(body.requestBody.type)
        assertNull(body.requestBody.customerIdentity)
        assertNull(body.requestBody.thirdParty)
    }

    @Test
    fun `createAuthRequestBody returns correct body for Anonymous`() {
        every { connection.customerId } returns "customerId"
        val body = chatImpl.createAuthRequestBody(AuthenticationType.Anonymous)
        assertEquals(AuthenticationType.Anonymous.name, body.requestBody.type)
    }

    @Test
    fun `createAuthRequestBody returns correct body for ThirdPartyOAuth explicit flow`() {
        val authorization = mockk<Authorization> {
            every { code } returns "code"
            every { verifier } returns "verifier"
        }
        val deps = ChatImplDependencies(
            socketFactory = mockk(relaxed = true),
            callback = mockk(relaxed = true),
            authorization = authorization,
        )
        chatImpl.dependencies = deps
        val body = chatImpl.createAuthRequestBody(AuthenticationType.ThirdPartyOAuth)
        assertEquals(GrantType.AUTHORIZATION_CODE, body.requestBody.thirdParty?.grantType)
        assertEquals("code", body.requestBody.thirdParty?.authorizationCode)
        assertEquals("verifier", body.requestBody.thirdParty?.codeVerifier)
    }

    @Test
    fun `createAuthRequestBody uses tokenDelegateListener for ThirdPartyOAuth implicit flow`() {
        val jwt = "integrator-jwt-token"
        chatImpl.dependencies = ChatImplDependencies(
            socketFactory = mockk(relaxed = true),
            callback = mockk(relaxed = true),
            authorization = Authorization.None,
        )
        // Set delegate → implicit flow
        chatImpl.tokenDelegateListener = { _: TokenRequestReason -> OAuthToken(jwt) }

        val bodyWithMeta = chatImpl.createAuthRequestBody(AuthenticationType.ThirdPartyOAuth)
        assertEquals(GrantType.IMPLICIT, bodyWithMeta.requestBody.thirdParty?.grantType)
        assertEquals(jwt, bodyWithMeta.requestBody.thirdParty?.accessToken)
        assertNull(bodyWithMeta.requestBody.thirdParty?.authorizationCode)
        assertNull(bodyWithMeta.requestBody.thirdParty?.codeVerifier)
        assertEquals(jwt, bodyWithMeta.oauthToken?.accessToken)
    }

    // ── isImplicitOAuthFlow property ────────────────────────────────────────

    @Test
    fun `isImplicitOAuthFlow is true when delegate set and authorization is None`() {
        val deps = ChatImplDependencies(
            socketFactory = mockk(relaxed = true),
            callback = mockk(relaxed = true),
            authorization = Authorization.None,
        )
        val chat = ChatImpl(
            connection = connection,
            entrails = entrails,
            dependencies = deps,
            configuration = configuration,
            rawChatStateListener = chatStateListener,
            tokenDelegateListener = { OAuthToken("token") },
        )
        assertTrue(chat.isImplicitOAuthFlow)
    }

    @Test
    fun `isImplicitOAuthFlow is false when delegate set but authorization is not None`() {
        val authorization = Authorization("code", "verifier")
        val deps = ChatImplDependencies(
            socketFactory = mockk(relaxed = true),
            callback = mockk(relaxed = true),
            authorization = authorization,
        )
        val chat = ChatImpl(
            connection = connection,
            entrails = entrails,
            dependencies = deps,
            configuration = configuration,
            rawChatStateListener = chatStateListener,
            tokenDelegateListener = { OAuthToken("token") },
        )
        assertFalse(chat.isImplicitOAuthFlow)
    }

    @Test
    fun `isImplicitOAuthFlow is false when no delegate regardless of authorization`() {
        val deps = ChatImplDependencies(
            socketFactory = mockk(relaxed = true),
            callback = mockk(relaxed = true),
            authorization = Authorization.None,
        )
        val chat = ChatImpl(
            connection = connection,
            entrails = entrails,
            dependencies = deps,
            configuration = configuration,
            rawChatStateListener = chatStateListener,
            tokenDelegateListener = null,
        )
        assertFalse(chat.isImplicitOAuthFlow)
    }

    // ── createAuthRequestBody — Anonymous ───────────────────────────────────

    @Test
    fun `createAuthRequestBody returns null customerIdentity for Anonymous when customerId is null`() {
        every { connection.customerId } returns null
        val body = chatImpl.createAuthRequestBody(AuthenticationType.Anonymous)
        assertEquals(AuthenticationType.Anonymous.name, body.requestBody.type)
        assertNull(body.requestBody.customerIdentity)
    }

    // ── createAuthRequestBody — implicit flow reason forwarding ─────────────

    @Test
    fun `createAuthRequestBody passes UNSPECIFIED reason to delegate for implicit flow`() {
        val capturedReasons = mutableListOf<TokenRequestReason>()
        chatImpl.dependencies = ChatImplDependencies(
            socketFactory = mockk(relaxed = true),
            callback = mockk(relaxed = true),
            authorization = Authorization.None,
        )
        chatImpl.tokenDelegateListener = { reason ->
            capturedReasons += reason
            OAuthToken("test-jwt")
        }

        chatImpl.createAuthRequestBody(AuthenticationType.ThirdPartyOAuth)

        assertEquals(1, capturedReasons.size)
        assertEquals(TokenRequestReason.UNSPECIFIED, capturedReasons[0])
    }

    @Test
    fun `createAuthRequestBody implicit flow body has null authorizationCode and codeVerifier`() {
        chatImpl.dependencies = ChatImplDependencies(
            socketFactory = mockk(relaxed = true),
            callback = mockk(relaxed = true),
            authorization = Authorization.None,
        )
        chatImpl.tokenDelegateListener = { OAuthToken("jwt") }

        val body = chatImpl.createAuthRequestBody(AuthenticationType.ThirdPartyOAuth)

        assertNull(body.requestBody.thirdParty?.authorizationCode)
        assertNull(body.requestBody.thirdParty?.codeVerifier)
        assertNull(body.requestBody.thirdParty?.refreshToken)
    }

    @Test
    fun `createAuthRequestBody explicit flow body has null accessToken and refreshToken`() {
        val authorization = mockk<Authorization> {
            every { code } returns "auth-code"
            every { verifier } returns "code-verifier"
        }
        chatImpl.dependencies = ChatImplDependencies(
            socketFactory = mockk(relaxed = true),
            callback = mockk(relaxed = true),
            authorization = authorization,
        )

        val body = chatImpl.createAuthRequestBody(AuthenticationType.ThirdPartyOAuth)

        assertNull(body.requestBody.thirdParty?.accessToken)
        assertNull(body.requestBody.thirdParty?.refreshToken)
        assertEquals(GrantType.AUTHORIZATION_CODE, body.requestBody.thirdParty?.grantType)
    }
}
