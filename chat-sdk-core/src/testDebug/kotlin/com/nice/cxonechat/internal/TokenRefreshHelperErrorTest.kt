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
import com.nice.cxonechat.exceptions.RuntimeChatException
import com.nice.cxonechat.internal.model.ThirdParty
import com.nice.cxonechat.internal.model.TransactionTokenModel
import com.nice.cxonechat.internal.socket.SocketState
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

/**
 * Missing-credential reporting and API-failure/exception handling of [TokenRefreshHelper].
 * Grant-type selection and successful persistence live in [TokenRefreshHelperTest].
 */
internal class TokenRefreshHelperErrorTest : AbstractTokenRefreshHelperTest() {

    @Test
    fun `refreshAccessToken reports TokenDelegationFailedException when delegate throws`() = runTest {
        val delegate = TokenDelegateListener { throw RuntimeChatException.TokenDelegationFailedException("no token") }
        every { chat.isImplicitOAuthFlow } returns true
        every { chat.tokenDelegateListener } returns delegate
        every { storage.transactionTokenModel } returns null
        TokenRefreshHelper.refreshAccessToken(chat)
        verify { chatStateListener.onChatRuntimeException(any<RuntimeChatException.TokenDelegationFailedException>()) }
        verify { socketListener.reportState(any()) }
    }

    @Test
    fun `refreshAccessToken reports AuthorizationError for explicit flow with no refresh_token`() = runTest {
        every { chat.isImplicitOAuthFlow } returns false
        every { storage.transactionTokenModel } returns null
        TokenRefreshHelper.refreshAccessToken(chat)
        verify { chatStateListener.onChatRuntimeException(any<RuntimeChatException.AuthorizationError>()) }
        verify { socketListener.reportState(any()) }
    }

    @Test
    fun `refreshAccessToken reports AuthorizationError when implicit flow but no delegate`() = runTest {
        every { chat.isImplicitOAuthFlow } returns true
        every { chat.tokenDelegateListener } returns null
        every { storage.transactionTokenModel } returns null
        TokenRefreshHelper.refreshAccessToken(chat)
        verify { chatStateListener.onChatRuntimeException(any<RuntimeChatException.AuthorizationError>()) }
        verify { socketListener.reportState(any()) }
    }

    @Test
    fun `refreshAccessToken reports InvalidRefreshTokenException when backend rejects refresh token`() = runTest {
        val testToken = "refresh-token"
        val existingModel = mockk<TransactionTokenModel> {
            every { thirdParty } returns mockk<ThirdParty> {
                every { refreshToken } returns testToken
            }
        }
        every { storage.transactionTokenModel } returns existingModel
        val response = mockk<Response<TransactionTokenModel>> {
            every { isSuccessful } returns false
            every { errorBody() } returns null
            every { code() } returns 400
            every { message() } returns "Bad Request"
        }
        every { authService.getTransactionToken(any(), any(), any(), any()) } returns mockk {
            every { execute() } returns response
        }
        TokenRefreshHelper.refreshAccessToken(chat)
        verify { chatStateListener.onChatRuntimeException(any<RuntimeChatException.InvalidRefreshTokenException>()) }
    }

    @Test
    fun `refreshAccessToken reports AuthorizationError when implicit flow JWT is rejected`() = runTest {
        // isRefreshTokenGrant is false here (GrantType.IMPLICIT) — a 400 must stay AuthorizationError,
        // not InvalidRefreshTokenException, since that type is reserved for rejected refresh_token grants.
        val delegate = TokenDelegateListener { OAuthToken("fresh-jwt") }
        every { chat.isImplicitOAuthFlow } returns true
        every { chat.tokenDelegateListener } returns delegate
        every { storage.transactionTokenModel } returns null
        val response = mockk<Response<TransactionTokenModel>> {
            every { isSuccessful } returns false
            every { errorBody() } returns null
            every { code() } returns 400
            every { message() } returns "Bad Request"
        }
        every { authService.getTransactionToken(any(), any(), any(), any()) } returns mockk {
            every { execute() } returns response
        }
        TokenRefreshHelper.refreshAccessToken(chat)
        verify { chatStateListener.onChatRuntimeException(any<RuntimeChatException.AuthorizationError>()) }
    }

    @Test
    fun `refreshAccessToken reports AuthorizationError on operational API failure`() = runTest {
        val testToken = "refresh-token"
        val existingModel = mockk<TransactionTokenModel> {
            every { thirdParty } returns mockk<ThirdParty> {
                every { refreshToken } returns testToken
            }
        }
        every { storage.transactionTokenModel } returns existingModel
        val response = mockk<Response<TransactionTokenModel>> {
            every { isSuccessful } returns false
            every { errorBody() } returns null
            every { code() } returns 500
            every { message() } returns "Internal Server Error"
        }
        every { authService.getTransactionToken(any(), any(), any(), any()) } returns mockk {
            every { execute() } returns response
        }
        TokenRefreshHelper.refreshAccessToken(chat)
        verify { chatStateListener.onChatRuntimeException(any<RuntimeChatException.AuthorizationError>()) }
    }

    @Test
    fun `refreshAccessToken reports AuthorizationError and preserves cached credentials when 200 response omits accessToken`() = runTest {
        // storage.transactionTokenModel defaults to existingTokenModel (transactionToken = "old-token",
        // thirdParty.accessToken = "old-access", thirdParty.refreshToken = "valid-refresh-token")
        every { chat.isImplicitOAuthFlow } returns false
        val malformedModel = mockk<TransactionTokenModel> {
            every { transactionToken } returns null
            every { thirdParty } returns null
        }
        val response = mockk<Response<TransactionTokenModel>> {
            every { isSuccessful } returns true
            every { body() } returns malformedModel
        }
        every { authService.getTransactionToken(any(), any(), any(), any()) } returns mockk {
            every { execute() } returns response
        }

        TokenRefreshHelper.refreshAccessToken(chat)

        verify { chatStateListener.onChatRuntimeException(any<RuntimeChatException.AuthorizationError>()) }
        verify { socketListener.reportState(SocketState.CLOSED) }
        verify(exactly = 0) { storage.authToken = any() }
        verify(exactly = 0) { storage.authTokenExpDate = any() }
        verify(exactly = 0) { storage.transactionTokenModel = any() }
    }

    @Test
    fun `does not call HTTP when refresh token is absent`() = runTest {
        currentExpDate.set(Clock.System.now() - 60.seconds)
        every { storage.transactionTokenModel } returns existingTokenModel.copy(thirdParty = null)

        TokenRefreshHelper.refreshAccessToken(chat)

        verify(exactly = 0) { authService.getTransactionToken(any(), any(), any(), any()) }
        verify { chatStateListener.onChatRuntimeException(any<RuntimeChatException.AuthorizationError>()) }
        verify { socketListener.reportState(SocketState.CLOSED) }
    }

    @Test
    fun `does not update storage when HTTP call throws`() = runTest {
        val expiredDate = Clock.System.now() - 60.seconds
        currentExpDate.set(expiredDate)
        val ioException = IOException("Network error")
        every { authService.getTransactionToken(any(), any(), any(), any()) } returns
                mockk { every { execute() } throws ioException }

        TokenRefreshHelper.refreshAccessToken(chat)

        currentExpDate.get() shouldBe expiredDate
        verify { chatStateListener.onChatRuntimeException(any<RuntimeChatException.AuthorizationError>()) }
        verify { socketListener.reportState(SocketState.CLOSED) }
    }

    @Test
    fun `RuntimeException during execute reports auth error closes socket`() = runTest {
        currentExpDate.set(Clock.System.now() - 60.seconds)
        val cause = NullPointerException("unexpected null inside doRefresh")
        every { authService.getTransactionToken(any(), any(), any(), any()) } returns
                mockk { every { execute() } throws cause }

        TokenRefreshHelper.refreshAccessToken(chat)

        verify { chatStateListener.onChatRuntimeException(any<RuntimeChatException.AuthorizationError>()) }
        verify { socketListener.reportState(SocketState.CLOSED) }
    }

    @Test
    fun `CancellationException during execute propagates and is not swallowed as RuntimeException`() = runTest {
        currentExpDate.set(Clock.System.now() - 60.seconds)
        every { authService.getTransactionToken(any(), any(), any(), any()) } returns
                mockk { every { execute() } throws CancellationException("scope cancelled") }

        var caughtCancellation = false
        try {
            TokenRefreshHelper.refreshAccessToken(chat)
        } catch (_: CancellationException) {
            caughtCancellation = true
        }

        caughtCancellation shouldBe true
        verify(exactly = 0) { chatStateListener.onChatRuntimeException(any()) }
    }
}
