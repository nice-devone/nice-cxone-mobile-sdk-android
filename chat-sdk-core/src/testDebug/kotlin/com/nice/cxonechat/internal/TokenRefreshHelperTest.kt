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
import com.nice.cxonechat.api.AuthService
import com.nice.cxonechat.exceptions.RuntimeChatException
import com.nice.cxonechat.internal.model.GrantType
import com.nice.cxonechat.internal.model.ThirdParty
import com.nice.cxonechat.internal.model.TransactionTokenModel
import com.nice.cxonechat.internal.socket.ProxyWebSocketListener
import com.nice.cxonechat.internal.socket.SocketState
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.storage.ValueStorage
import com.nice.cxonechat.tool.nextString
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import retrofit2.Call
import retrofit2.Response
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

internal class TokenRefreshHelperTest {
    private lateinit var chat: ChatWithParameters
    private lateinit var storage: ValueStorage
    private lateinit var entrails: ChatEntrails
    private lateinit var authService: AuthService
    private lateinit var chatStateListener: ChatStateListener
    private lateinit var socketListener: ProxyWebSocketListener
    private lateinit var connection: Connection

    // AtomicReference so the mock getter reflects updates the setter writes,
    // allowing the mutex re-check to observe a fresh token after the first caller refreshes.
    private val currentExpDate = AtomicReference<Instant?>(null)

    // Real Mutex so withLock actually serializes concurrent callers in the thundering-herd test.
    private val realTokenRefreshMutex = Mutex()

    private lateinit var existingTokenModel: TransactionTokenModel

    @Before
    fun setUp() {
        currentExpDate.set(Instant.DISTANT_FUTURE) // fresh by default; each test overrides as needed
        storage = mockk(relaxed = true)
        entrails = mockk(relaxed = true)
        authService = mockk(relaxed = true)
        chatStateListener = mockk(relaxed = true)
        socketListener = mockk(relaxed = true)
        connection = mockk(relaxed = true)
        chat = mockk(relaxed = true)
        existingTokenModel = TransactionTokenModel(
            transactionToken = "old-token",
            expiresIn = 0L,
            thirdParty = ThirdParty(
                accessToken = "old-access",
                refreshToken = "valid-refresh-token",
                expiresIn = 0L
            )
        )
        every { chat.storage } returns storage
        every { chat.entrails } returns entrails
        every { entrails.authService } returns authService
        every { entrails.storage } returns storage
        every { entrails.threading.ioDispatcher } returns Dispatchers.Unconfined
        every { chat.chatStateListener } returns chatStateListener
        every { chat.socketListener } returns socketListener
        every { chat.connection } returns connection
        every { connection.brandId } returns 1
        every { connection.channelId } returns "channel-1"
        every { storage.visitorId } returns UUID.randomUUID()
        every { storage.authTokenExpDate } answers { currentExpDate.get() }
        every { storage.authTokenExpDate = any() } answers { currentExpDate.set(firstArg()) }
        every { storage.transactionTokenModel } returns existingTokenModel
        every { storage.visitorId } returns UUID.fromString("00000000-0000-0000-0000-000000000001")
        every { chat.guards } returns Threading.Guards(tokenRefreshMutex = realTokenRefreshMutex)
    }

    @Test
    fun `refreshAccessToken uses refresh_token when available`() = runTest {
        val instanceToken = "refresh-token-xyz"
        val existingModel = mockk<TransactionTokenModel> {
            every { thirdParty } returns mockk { every { refreshToken } returns instanceToken }
        }
        every { storage.transactionTokenModel } returns existingModel
        val newModel = mockk<TransactionTokenModel> {
            every { thirdParty } returns mockk {
                every { accessToken } returns "new-access"
                every { expiresAt } returns mockk()
            }
        }
        val response = mockk<Response<TransactionTokenModel>> {
            every { isSuccessful } returns true
            every { body() } returns newModel
        }
        every { authService.getTransactionToken(any(), any(), any(), any()) } returns mockk {
            every { execute() } returns response
        }
        val bodySlot = slot<com.nice.cxonechat.internal.model.TokenRequestBody>()
        every { authService.getTransactionToken(any(), any(), any(), capture(bodySlot)) } returns mockk {
            every { execute() } returns response
        }
        TokenRefreshHelper.refreshAccessToken(chat)
        assertEquals(GrantType.REFRESH_TOKEN, bodySlot.captured.thirdParty?.grantType)
        assertEquals(instanceToken, bodySlot.captured.thirdParty?.refreshToken)
        verify { storage.transactionTokenModel = newModel }
    }

    @Test
    fun `refreshAccessToken uses delegate when implicit flow and no refresh_token`() = runTest {
        val jwt = "integrator-jwt"
        val expiryMillis = System.currentTimeMillis() + 3600_000L
        val oauthToken = OAuthToken(jwt, expiryMillis)
        val delegate = TokenDelegateListener { oauthToken }
        every { chat.isImplicitOAuthFlow } returns true
        every { chat.tokenDelegateListener } returns delegate
        every { storage.transactionTokenModel } returns null
        val newModel = mockk<TransactionTokenModel> {
            every { thirdParty } returns mockk {
                every { accessToken } returns "new-access"
                every { expiresAt } returns mockk()
            }
        }
        val response = mockk<Response<TransactionTokenModel>> {
            every { isSuccessful } returns true
            every { body() } returns newModel
        }
        val bodySlot = slot<com.nice.cxonechat.internal.model.TokenRequestBody>()
        every { authService.getTransactionToken(any(), any(), any(), capture(bodySlot)) } returns mockk {
            every { execute() } returns response
        }
        TokenRefreshHelper.refreshAccessToken(chat)
        assertEquals(GrantType.IMPLICIT, bodySlot.captured.thirdParty?.grantType)
        assertEquals(jwt, bodySlot.captured.thirdParty?.accessToken)
        verify { storage.transactionTokenModel = newModel }
    }

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
    fun `refreshAccessToken stores request JWT and expiry from delegate for implicit flow`() = runTest {
        val requestJwt = "integrator-jwt-for-storage"
        val expiryMillis = System.currentTimeMillis() + 3600_000L
        val oauthToken = OAuthToken(requestJwt, expiryMillis)
        val delegate = TokenDelegateListener { oauthToken }
        every { chat.isImplicitOAuthFlow } returns true
        every { chat.tokenDelegateListener } returns delegate
        every { storage.transactionTokenModel } returns null
        val newModel = mockk<TransactionTokenModel> {
            every { thirdParty } returns mockk {
                every { accessToken } returns "response-access-token-should-not-be-stored"
                every { expiresAt } returns Instant.fromEpochMilliseconds(0)
            }
        }
        val response = mockk<Response<TransactionTokenModel>> {
            every { isSuccessful } returns true
            every { body() } returns newModel
        }
        every { authService.getTransactionToken(any(), any(), any(), any()) } returns mockk {
            every { execute() } returns response
        }
        TokenRefreshHelper.refreshAccessToken(chat)
        // JWT from delegate must be stored, not the server's access token
        verify { storage.authToken = requestJwt }
        // Expiry from delegate must be stored
        verify { storage.authTokenExpDate = Instant.fromEpochMilliseconds(expiryMillis) }
    }

    @Test
    fun `refreshAccessToken stores response accessToken as authToken for explicit flow`() = runTest {
        val testRefreshToken = "explicit-refresh-token"
        val responseAccessToken = "explicit-response-access-token"
        val existingModel = mockk<TransactionTokenModel> {
            every { thirdParty } returns mockk<ThirdParty> { every { refreshToken } returns testRefreshToken }
        }
        every { chat.isImplicitOAuthFlow } returns false
        every { storage.transactionTokenModel } returns existingModel
        val responseExpiry = Clock.System.now() + 3600.seconds
        val newModel = mockk<TransactionTokenModel> {
            every { thirdParty } returns mockk<ThirdParty> {
                every { accessToken } returns responseAccessToken
                every { expiresAt } returns responseExpiry
            }
        }
        val response = mockk<Response<TransactionTokenModel>> {
            every { isSuccessful } returns true
            every { body() } returns newModel
        }
        every { authService.getTransactionToken(any(), any(), any(), any()) } returns mockk {
            every { execute() } returns response
        }
        TokenRefreshHelper.refreshAccessToken(chat)
        verify { storage.authToken = responseAccessToken }
        verify { storage.authTokenExpDate = responseExpiry }
    }

    @Test
    fun `refreshAccessToken updates authToken and authTokenExpDate on success`() = runTest {
        val testToken = "refresh-token-xyz"
        val newAccessToken = "new-access-token"
        val expiresAt = Clock.System.now()
        val existingModel = mockk<TransactionTokenModel> {
            every { thirdParty } returns mockk { every { refreshToken } returns testToken }
        }
        every { storage.transactionTokenModel } returns existingModel
        val newThirdParty = mockk<ThirdParty> {
            every { accessToken } returns newAccessToken
            every { this@mockk.expiresAt } returns expiresAt
        }
        val newModel = mockk<TransactionTokenModel> {
            every { thirdParty } returns newThirdParty
        }
        val response = mockk<Response<TransactionTokenModel>> {
            every { isSuccessful } returns true
            every { body() } returns newModel
        }
        every { authService.getTransactionToken(any(), any(), any(), any()) } returns mockk {
            every { execute() } returns response
        }
        TokenRefreshHelper.refreshAccessToken(chat)
        verify { storage.authToken = newAccessToken }
        verify { storage.authTokenExpDate = expiresAt }
        verify { storage.transactionTokenModel = newModel }
    }

    @Test
    fun `refreshAccessToken reports AuthorizationError on API failure`() = runTest {
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
            every { code() } returns 401
            every { message() } returns "Unauthorized"
        }
        every { authService.getTransactionToken(any(), any(), any(), any()) } returns mockk {
            every { execute() } returns response
        }
        TokenRefreshHelper.refreshAccessToken(chat)
        verify { chatStateListener.onChatRuntimeException(any<RuntimeChatException.AuthorizationError>()) }
    }

    @Test
    fun `refreshes token when expired and updates authToken and transactionTokenModel`() = runTest {
        currentExpDate.set(Clock.System.now() - 60.seconds)
        val freshAccessToken = nextString()
        every { authService.getTransactionToken(any(), any(), any(), any()) } returns
                successfulRefreshCall(freshAccessToken = freshAccessToken)

        TokenRefreshHelper.refreshAccessToken(chat)

        verify(exactly = 1) { authService.getTransactionToken(any(), any(), any(), any()) }
        currentExpDate.get() shouldNotBe null
        verify { storage.authToken = freshAccessToken }
        verify { storage.transactionTokenModel = any() }
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

    private fun successfulRefreshCall(freshAccessToken: String, expiresInSeconds: Long = 3600L): Call<TransactionTokenModel> {
        val model = buildFreshTokenModel(freshAccessToken, expiresInSeconds)
        return mockk { every { execute() } returns Response.success(model) }
    }

    private fun buildFreshTokenModel(
        freshAccessToken: String = "fresh-access",
        expiresInSeconds: Long,
    ) = TransactionTokenModel(
        transactionToken = "fresh-transaction-token",
        expiresIn = expiresInSeconds,
        thirdParty = ThirdParty(
            accessToken = freshAccessToken,
            refreshToken = "fresh-refresh-token",
            expiresIn = expiresInSeconds
        )
    )
}
