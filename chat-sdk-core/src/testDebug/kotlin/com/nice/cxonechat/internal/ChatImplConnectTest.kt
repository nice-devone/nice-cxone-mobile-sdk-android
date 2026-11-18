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
import com.nice.cxonechat.exceptions.RuntimeChatException
import com.nice.cxonechat.internal.model.ChatImplDependencies
import com.nice.cxonechat.internal.model.ThirdParty
import com.nice.cxonechat.internal.model.TransactionTokenModel
import com.nice.cxonechat.internal.socket.SocketFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.WebSocket
import org.junit.Test
import retrofit2.Call
import retrofit2.Response
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Connection behavior of [ChatImpl.connect]: token fetching, implicit/explicit JWT storage, and
 * connect-failure propagation. Auth-body construction lives in [ChatImplTest]; state emission in
 * [ChatImplStateFlowTest]; close/signOut lifecycle in [ChatImplLifecycleTest].
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatImplConnectTest : AbstractChatImplTest() {

    @Test
    fun `connect fetches new transaction token when no valid token`() = runTest {
        val socket = mockk<WebSocket>()
        val mockSocketFactory = mockk<SocketFactory> {
            // Use 3-arg match: Kotlin compiles create(listener, visitorId) as create(listener, visitorId, null)
            every { create(any(), any(), isNull()) } returns socket
        }
        every { storage.transactionTokenModel } returns null
        val deps = mockk<ChatImplDependencies>(relaxed = true) {
            every { socketFactory } returns mockSocketFactory
        }
        chatImpl.dependencies = deps
        val connectJob = launch {
            assertFailsWith<TransientConnectFailureException> { chatImpl.connect() }
        }
        // Advance until connect() suspends at openDeferred.await(), then fire onOpen
        advanceUntilIdle()
        chatImpl.socketListener.onOpen(socket, mockk())
        connectJob.join()
    }

    @Test
    fun `connect reports token delegation failure when auth body creation throws`() = runTest {
        val cause = IllegalStateException("delegate failed")
        val tokenDelegationException = RuntimeChatException.TokenDelegationFailedException(
            cause.message ?: "Token delegation failed", cause
        )
        every { configuration.authenticationType } returns AuthenticationType.ThirdPartyOAuth
        every { chatImpl.createAuthRequestBody(AuthenticationType.ThirdPartyOAuth) } throws tokenDelegationException

        val connectJob = launch { chatImpl.connect() }
        // Advance until connect() suspends at openDeferred.await()
        advanceUntilIdle()
        connectJob.join()
        verify {
            chatStateListener.onChatRuntimeException(
                withArg {
                    assertTrue(it is RuntimeChatException.TokenDelegationFailedException)
                    assertSame(cause, it.cause)
                    assertEquals("delegate failed", it.message)
                }
            )
        }
    }

    @Test
    fun `connect stores request JWT as authToken for implicit flow`() = runTest {
        val requestJwt = "google-jwt-from-delegate"
        val responseAccessToken = "response-access-token-should-not-be-stored"

        val socket = mockk<WebSocket>()
        val socketFactory = mockk<SocketFactory> {
            every { create(any(), any(), any()) } returns socket
        }
        chatImpl.dependencies = ChatImplDependencies(
            socketFactory = socketFactory,
            callback = mockk(relaxed = true),
            authorization = Authorization.None,
        )
        chatImpl.tokenDelegateListener = { OAuthToken(requestJwt) }

        every { configuration.authenticationType } returns AuthenticationType.ThirdPartyOAuth
        every { storage.transactionTokenModel } returns null
        every { storage.visitorId } returns UUID.randomUUID()

        val transactionTokenModel = mockk<TransactionTokenModel>(relaxed = true) {
            every { thirdParty } returns mockk {
                every { accessToken } returns responseAccessToken
                every { expiresAt } returns mockk()
            }
            every { transactionToken } returns "tx-token"
            every { isExpired } returns false
            every { customerIdentity } returns null
        }
        val response = mockk<Response<TransactionTokenModel>> {
            every { isSuccessful } returns true
            every { body() } returns transactionTokenModel
        }
        every { authService.getTransactionToken(any(), any(), any(), any()) } returns mockk {
            every { execute() } returns response
        }

        val connectJob = launch { chatImpl.connect() }
        advanceUntilIdle()
        chatImpl.socketListener.onOpen(socket, mockk())
        connectJob.join()
        verify { storage.authToken = requestJwt }
    }

    @Test
    fun `connect stores refreshed JWT when two-step implicit reconnect occurs`() = runTest {
        val staleJwt = "stale-jwt-rejected-by-backend"
        val freshJwt = "fresh-jwt-from-second-delegate-call"
        val responseAccessToken = "response-access-token-should-not-be-stored"

        val socket = mockk<WebSocket>()
        val socketFactory = mockk<SocketFactory> {
            every { create(any(), any(), any()) } returns socket
        }
        val deps = ChatImplDependencies(
            socketFactory = socketFactory,
            callback = mockk(relaxed = true),
            authorization = Authorization.None,
        )
        chatImpl.dependencies = deps
        // Delegate returns stale JWT on first call (UNSPECIFIED), fresh JWT on second (TOKEN_INVALID)
        chatImpl.tokenDelegateListener = { reason ->
            if (reason == TokenRequestReason.UNSPECIFIED) {
                OAuthToken(staleJwt)
            } else {
                OAuthToken(freshJwt)
            }
        }

        every { configuration.authenticationType } returns AuthenticationType.ThirdPartyOAuth
        every { storage.visitorId } returns UUID.randomUUID()

        // Simulate: expired transaction token so connectWithImplicitJwt runs
        val expiredModel = mockk<TransactionTokenModel> { every { isExpired } returns true }
        every { storage.transactionTokenModel } returns expiredModel

        val transactionTokenModel = mockk<TransactionTokenModel>(relaxed = true) {
            every { thirdParty } returns mockk {
                every { accessToken } returns responseAccessToken
                every { expiresAt } returns mockk()
            }
            every { transactionToken } returns "tx-token"
            every { isExpired } returns false
            every { customerIdentity } returns null
        }
        // Step 1: 400 (stale JWT rejected), Step 2: success (fresh JWT accepted)
        val rejectionResponse = mockk<Response<TransactionTokenModel>> {
            every { isSuccessful } returns false
            every { errorBody() } returns null
            every { code() } returns 400
            every { message() } returns "Bad Request"
        }
        val successResponse = mockk<Response<TransactionTokenModel>> {
            every { isSuccessful } returns true
            every { body() } returns transactionTokenModel
        }
        every { authService.getTransactionToken(any(), any(), any(), any()) } returnsMany listOf(
            mockk { every { execute() } returns rejectionResponse },
            mockk { every { execute() } returns successResponse },
        )

        val connectJob = launch { chatImpl.connect() }
        // Advance until connect() suspends at openDeferred.await(), then fire onOpen
        advanceUntilIdle()
        chatImpl.socketListener.onOpen(socket, mockk())
        connectJob.join()

        // Must store the fresh JWT, not the stale one
        verify { storage.authToken = freshJwt }
    }

    @Test
    fun `connect stores delegate JWT as authToken for implicit flow when valid cached transaction token exists`() = runTest {
        val requestJwt = "delegate-jwt-for-cached-token-path"
        val responseAccessToken = "response-access-token-should-not-be-stored"

        val socket = mockk<WebSocket>()
        val socketFactory = mockk<SocketFactory> {
            every { create(any(), any(), any()) } returns socket
        }
        chatImpl.dependencies = ChatImplDependencies(
            socketFactory = socketFactory,
            callback = mockk(relaxed = true),
            authorization = Authorization.None,
        )

        chatImpl.tokenDelegateListener = { OAuthToken(requestJwt) }

        every { configuration.authenticationType } returns AuthenticationType.ThirdPartyOAuth
        every { storage.visitorId } returns UUID.randomUUID()

        // Valid cached transaction token — takes Case 1: connectWithTransactionToken (no API call)
        val cachedModel = mockk<TransactionTokenModel>(relaxed = true) {
            every { isExpired } returns false
            every { transactionToken } returns "cached-tx-token"
            every { thirdParty } returns mockk {
                every { accessToken } returns responseAccessToken
                every { expiresAt } returns mockk()
            }
            every { customerIdentity } returns null
        }
        every { storage.transactionTokenModel } returns cachedModel

        val connectJob = launch { chatImpl.connect() }
        // Advance until connect() suspends at openDeferred.await(), then fire onOpen
        advanceUntilIdle()
        chatImpl.socketListener.onOpen(socket, mockk())
        connectJob.join()

        // Delegate JWT (from UNSPECIFIED call in createAuthRequestBody) must be stored,
        // not the response transactionTokenModel.thirdParty.accessToken
        verify { storage.authToken = requestJwt }
    }

    @Test
    fun `connect stores response accessToken as authToken for explicit flow`() = runTest {
        val responseAccessToken = "explicit-response-access-token"

        val socket = mockk<WebSocket>()
        val socketFactory = mockk<SocketFactory> {
            every { create(any(), any(), any()) } returns socket
        }
        val authorization = mockk<Authorization> {
            every { code } returns "auth-code"
            every { verifier } returns "verifier"
        }
        chatImpl.dependencies = ChatImplDependencies(
            socketFactory = socketFactory,
            callback = mockk(relaxed = true),
            authorization = authorization,
        )

        every { configuration.authenticationType } returns AuthenticationType.ThirdPartyOAuth
        every { storage.transactionTokenModel } returns null
        every { storage.visitorId } returns UUID.randomUUID()

        val transactionTokenModel = mockk<TransactionTokenModel>(relaxed = true) {
            every { thirdParty } returns mockk {
                every { accessToken } returns responseAccessToken
                every { expiresAt } returns mockk()
            }
            every { transactionToken } returns "tx-token"
            every { isExpired } returns false
            every { customerIdentity } returns null
        }
        val response = mockk<Response<TransactionTokenModel>> {
            every { isSuccessful } returns true
            every { body() } returns transactionTokenModel
        }
        every { authService.getTransactionToken(any(), any(), any(), any()) } returns mockk {
            every { execute() } returns response
        }

        val connectJob = launch { chatImpl.connect() }
        // Advance until connect() suspends at openDeferred.await(), then fire onOpen
        advanceUntilIdle()
        chatImpl.socketListener.onOpen(socket, mockk())
        connectJob.join()

        verify { storage.authToken = responseAccessToken }
    }

    @Test
    fun `connect throws PermanentConnectionFailureException when connector reports ConnectionTokenFailed`() = runTest {
        every { configuration.authenticationType } returns AuthenticationType.ThirdPartyOAuth
        // A real instance (not a mock) since the connect() decision now derives the access
        // token's expiry from thirdParty.expiresAt rather than storage.authTokenExpDate.
        val transactionTokenModel = mockk<TransactionTokenModel>(relaxed = true) {
            every { isExpired } returns true
            every { thirdParty } returns ThirdParty(refreshToken = null, expiresIn = 0L)
        }
        every { storage.transactionTokenModel } returns transactionTokenModel
        // assertFailsWith inside launch consumes the exception so it doesn't propagate to the test scope.
        // PermanentConnectionFailureException extends CancellationException — assertFailsWith catches
        // it before the coroutine framework sees it, so connectJob completes normally (not cancelled).
        val connectJob = launch {
            assertFailsWith<PermanentConnectionFailureException> { chatImpl.connect() }
        }
        advanceUntilIdle()
        connectJob.join()
        assertTrue(connectJob.isCompleted)
        assertFalse(connectJob.isCancelled)
    }

    @Test
    fun `connect throws TransientConnectFailureException when connector reports non-ConnectionTokenFailed error`() = runTest {
        every { configuration.authenticationType } returns AuthenticationType.SecuredCookie
        every { storage.transactionTokenModel } returns null // forces Case 3: fetch new token
        val failingTokenCall = mockk<Call<TransactionTokenModel>> {
            every { execute() } returns Response.error(503, mockk(relaxed = true))
        }
        every { entrails.authService.getTransactionToken(any(), any(), any(), any()) } returns failingTokenCall
        val connectJob = launch {
            assertFailsWith<TransientConnectFailureException> { chatImpl.connect() }
        }
        advanceUntilIdle()
        connectJob.join()
        assertTrue(connectJob.isCompleted)
        assertFalse(connectJob.isCancelled)
    }

    @Test
    fun `connect throws PermanentConnectionFailureException when connector reports InvalidRefreshTokenException`() = runTest {
        every { configuration.authenticationType } returns AuthenticationType.ThirdPartyOAuth
        // A real instance (not a mock) since the connect() decision now derives the access
        // token's expiry from thirdParty.expiresAt rather than storage.authTokenExpDate.
        val transactionTokenModel = mockk<TransactionTokenModel>(relaxed = true) {
            every { isExpired } returns true
            every { thirdParty } returns ThirdParty(refreshToken = "stored-refresh-token", expiresIn = 0L)
        }
        every { storage.transactionTokenModel } returns transactionTokenModel
        val failingTokenCall = mockk<Call<TransactionTokenModel>> {
            every { execute() } returns Response.error(400, mockk(relaxed = true))
        }
        every { entrails.authService.getTransactionToken(any(), any(), any(), any()) } returns failingTokenCall
        val connectJob = launch {
            assertFailsWith<PermanentConnectionFailureException> { chatImpl.connect() }
        }
        advanceUntilIdle()
        connectJob.join()
        assertTrue(connectJob.isCompleted)
        assertFalse(connectJob.isCancelled)
    }

    @Test
    fun `connect throws TransientConnectFailureException when socket onFailure fires while awaiting open`() = runTest {
        val socket = mockk<WebSocket>()
        val socketFactory = mockk<SocketFactory> {
            every { create(any(), any(), isNull()) } returns socket
        }
        val deps = mockk<ChatImplDependencies>(relaxed = true) {
            every { this@mockk.socketFactory } returns socketFactory
        }
        chatImpl.dependencies = deps
        // assertFailsWith inside launch consumes the exception so it doesn't propagate to the test scope.
        // TransientConnectFailureException (extends Exception, not CancellationException) would otherwise fail the test.
        val connectJob = launch {
            assertFailsWith<TransientConnectFailureException> { chatImpl.connect() }
        }
        advanceUntilIdle()
        // Simulate socket failure; connect() should throw TransientConnectFailureException (retriable by ReconnectingListener)
        chatImpl.socketListener.onFailure(socket, RuntimeException("Connection refused"), null)
        connectJob.join()
        assertTrue(connectJob.isCompleted)
        assertFalse(connectJob.isCancelled)
    }
}
