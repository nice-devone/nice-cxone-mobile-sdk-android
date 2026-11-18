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
import com.nice.cxonechat.internal.model.GrantType
import com.nice.cxonechat.internal.model.ThirdParty
import com.nice.cxonechat.internal.model.TransactionTokenModel
import com.nice.cxonechat.tool.nextString
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.Call
import retrofit2.Response
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.CoroutineContext
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Grant-type selection and successful-persistence behavior of [TokenRefreshHelper].
 * Missing-credential and API-failure behavior lives in [TokenRefreshHelperErrorTest].
 */
internal class TokenRefreshHelperTest : AbstractTokenRefreshHelperTest() {

    @Test
    fun `refreshAccessToken uses refresh_token when available`() = runTest {
        val instanceToken = "refresh-token-xyz"
        val existingModel = TransactionTokenModel(
            thirdParty = ThirdParty(refreshToken = instanceToken, expiresIn = 0L)
        )
        every { storage.transactionTokenModel } returns existingModel
        val newModel = TransactionTokenModel(
            transactionToken = "new-transaction-token",
            thirdParty = ThirdParty(accessToken = "new-access", refreshToken = "rotated-refresh-token", expiresIn = 3600L)
        )
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
        val expiryMillis = System.currentTimeMillis() + 3_600_000L
        val oauthToken = OAuthToken(jwt, expiryMillis)
        val delegate = TokenDelegateListener { oauthToken }
        every { chat.isImplicitOAuthFlow } returns true
        every { chat.tokenDelegateListener } returns delegate
        every { storage.transactionTokenModel } returns null
        val newModel = TransactionTokenModel(
            transactionToken = "new-transaction-token",
            thirdParty = ThirdParty(accessToken = "new-access", refreshToken = "rotated-refresh-token", expiresIn = 3600L)
        )
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
    fun `refreshAccessToken stores request JWT and expiry from delegate for implicit flow`() = runTest {
        val requestJwt = "integrator-jwt-for-storage"
        val expiryMillis = System.currentTimeMillis() + 3_600_000L
        val oauthToken = OAuthToken(requestJwt, expiryMillis)
        val delegate = TokenDelegateListener { oauthToken }
        every { chat.isImplicitOAuthFlow } returns true
        every { chat.tokenDelegateListener } returns delegate
        every { storage.transactionTokenModel } returns null
        val newModel = TransactionTokenModel(
            transactionToken = "new-transaction-token",
            thirdParty = ThirdParty(
                accessToken = "response-access-token-should-not-be-stored",
                refreshToken = "rotated-refresh-token",
                expiresIn = 0L,
                createdAt = Instant.fromEpochMilliseconds(0),
            )
        )
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
        val existingModel = TransactionTokenModel(
            thirdParty = ThirdParty(refreshToken = testRefreshToken, expiresIn = 0L)
        )
        every { chat.isImplicitOAuthFlow } returns false
        every { storage.transactionTokenModel } returns existingModel
        val responseExpiry = Clock.System.now() + 3600.seconds
        val newModel = TransactionTokenModel(
            transactionToken = "new-transaction-token",
            thirdParty = ThirdParty(
                accessToken = responseAccessToken,
                refreshToken = "rotated-refresh-token",
                expiresIn = 0L,
                createdAt = responseExpiry,
            )
        )
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
        val responseExpiry = Clock.System.now()
        val existingModel = TransactionTokenModel(
            thirdParty = ThirdParty(refreshToken = testToken, expiresIn = 0L)
        )
        every { storage.transactionTokenModel } returns existingModel
        val newModel = TransactionTokenModel(
            transactionToken = "new-transaction-token",
            thirdParty = ThirdParty(
                accessToken = newAccessToken,
                refreshToken = "rotated-refresh-token",
                expiresIn = 0L,
                createdAt = responseExpiry,
            )
        )
        val response = mockk<Response<TransactionTokenModel>> {
            every { isSuccessful } returns true
            every { body() } returns newModel
        }
        every { authService.getTransactionToken(any(), any(), any(), any()) } returns mockk {
            every { execute() } returns response
        }
        TokenRefreshHelper.refreshAccessToken(chat)
        verify { storage.authToken = newAccessToken }
        verify { storage.authTokenExpDate = responseExpiry }
        verify { storage.transactionTokenModel = newModel }
    }

    @Test
    fun `refreshAccessToken preserves cached transaction token when refresh_token response omits it`() = runTest {
        // storage.transactionTokenModel defaults to existingTokenModel (transactionToken = "old-token")
        val newThirdParty = mockk<ThirdParty> {
            every { accessToken } returns "fresh-access-token"
            every { expiresAt } returns mockk()
            // Rotating-refresh-token backend: a fresh refresh token is present, so the merge must not
            // touch it — see the fallback scenario below for when it's absent instead.
            every { refreshToken } returns "rotated-refresh-token"
        }
        val newModel = mockk<TransactionTokenModel> {
            every { transactionToken } returns null
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
        val storedModelSlot = slot<TransactionTokenModel>()
        verify { storage.transactionTokenModel = capture(storedModelSlot) }
        assertEquals("old-token", storedModelSlot.captured.transactionToken)
        assertEquals(newThirdParty, storedModelSlot.captured.thirdParty)
    }

    @Test
    fun `refreshAccessToken preserves cached refresh token when refresh_token response omits it`() = runTest {
        // storage.transactionTokenModel defaults to existingTokenModel (thirdParty.refreshToken = "valid-refresh-token")
        val newThirdParty = ThirdParty(accessToken = "fresh-access-token", refreshToken = null, expiresIn = 3600L)
        val newModel = mockk<TransactionTokenModel> {
            every { transactionToken } returns null
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
        val storedModelSlot = slot<TransactionTokenModel>()
        verify { storage.transactionTokenModel = capture(storedModelSlot) }
        assertEquals("fresh-access-token", storedModelSlot.captured.thirdParty?.accessToken)
        assertEquals("valid-refresh-token", storedModelSlot.captured.thirdParty?.refreshToken)
    }

    @Test
    fun `refreshAccessToken preserves cached refresh token when response includes transaction token`() = runTest {
        // storage.transactionTokenModel defaults to existingTokenModel (thirdParty.refreshToken = "valid-refresh-token").
        // Unlike the sibling scenario above, this response also carries a transactionToken — the merge must
        // still apply, not just for responses that omit transactionToken.
        val newThirdParty = ThirdParty(accessToken = "fresh-access-token", refreshToken = null, expiresIn = 3600L)
        val newModel = TransactionTokenModel(transactionToken = "new-transaction-token", thirdParty = newThirdParty)
        val response = mockk<Response<TransactionTokenModel>> {
            every { isSuccessful } returns true
            every { body() } returns newModel
        }
        every { authService.getTransactionToken(any(), any(), any(), any()) } returns mockk {
            every { execute() } returns response
        }
        TokenRefreshHelper.refreshAccessToken(chat)
        val storedModelSlot = slot<TransactionTokenModel>()
        verify { storage.transactionTokenModel = capture(storedModelSlot) }
        assertEquals("new-transaction-token", storedModelSlot.captured.transactionToken)
        assertEquals("fresh-access-token", storedModelSlot.captured.thirdParty?.accessToken)
        assertEquals("valid-refresh-token", storedModelSlot.captured.thirdParty?.refreshToken)
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

    /** signOut() clears storage, which rotates visitorId; simulate it happening while the request is in flight. */
    private fun clearStorageDuringRequest(call: Call<TransactionTokenModel>) {
        val visitorId = AtomicReference(UUID.fromString("00000000-0000-0000-0000-000000000001"))
        every { storage.visitorId } answers { visitorId.get() }
        every { authService.getTransactionToken(any(), any(), any(), any()) } answers {
            visitorId.set(UUID.randomUUID())
            call
        }
    }

    @Test
    fun `refreshAccessToken discards a successful result when storage was cleared while it was in flight`() = runTest {
        currentExpDate.set(Clock.System.now() - 60.seconds)
        clearStorageDuringRequest(successfulRefreshCall(freshAccessToken = nextString()))

        TokenRefreshHelper.refreshAccessToken(chat)

        verify(exactly = 0) { storage.authToken = any() }
        verify(exactly = 0) { storage.authTokenExpDate = any() }
        verify(exactly = 0) { storage.transactionTokenModel = any() }
    }

    @Test
    fun `refreshAccessToken does not report a failure when storage was cleared while it was in flight`() = runTest {
        currentExpDate.set(Clock.System.now() - 60.seconds)
        clearStorageDuringRequest(
            mockk { every { execute() } returns Response.error(401, "".toResponseBody()) }
        )

        TokenRefreshHelper.refreshAccessToken(chat)

        verify(exactly = 0) { chatStateListener.onChatRuntimeException(any()) }
        verify(exactly = 0) { socketListener.reportState(any()) }
    }

    @Test
    fun `refreshAccessToken does not report a delegate failure when storage was cleared while the delegate ran`() = runTest {
        val visitorId = AtomicReference(UUID.fromString("00000000-0000-0000-0000-000000000001"))
        every { storage.visitorId } answers { visitorId.get() }
        every { chat.isImplicitOAuthFlow } returns true
        every { chat.tokenDelegateListener } returns TokenDelegateListener {
            visitorId.set(UUID.randomUUID()) // the user signed out while the integrator was fetching a token
            error("No signed-in user")
        }

        TokenRefreshHelper.refreshAccessToken(chat)

        verify(exactly = 0) { chatStateListener.onChatRuntimeException(any()) }
        verify(exactly = 0) { socketListener.reportState(any()) }
        verify(exactly = 0) { authService.getTransactionToken(any(), any(), any(), any()) }
    }

    @Test
    fun `refreshAccessToken does not send the token request when storage was cleared while a successful delegate ran`() =
        runTest {
            val visitorId = AtomicReference(UUID.fromString("00000000-0000-0000-0000-000000000001"))
            every { storage.visitorId } answers { visitorId.get() }
            every { chat.isImplicitOAuthFlow } returns true
            every { chat.tokenDelegateListener } returns TokenDelegateListener {
                visitorId.set(UUID.randomUUID()) // the user signed out while the integrator was fetching a token
                OAuthToken("next-user-jwt", System.currentTimeMillis() + 3_600_000L)
            }

            TokenRefreshHelper.refreshAccessToken(chat)

            verify(exactly = 0) { authService.getTransactionToken(any(), any(), any(), any()) }
            verify(exactly = 0) { storage.authToken = any() }
            verify(exactly = 0) { chatStateListener.onChatRuntimeException(any()) }
            verify(exactly = 0) { socketListener.reportState(any()) }
        }

    @Test
    fun `refreshAccessToken does not send the token request when storage was cleared during the hop to IO`() = runTest {
        currentExpDate.set(Clock.System.now() - 60.seconds)
        val visitorId = AtomicReference(UUID.fromString("00000000-0000-0000-0000-000000000001"))
        every { storage.visitorId } answers { visitorId.get() }
        every { entrails.threading.ioDispatcher } returns object : CoroutineDispatcher() {
            override fun dispatch(context: CoroutineContext, block: Runnable) {
                visitorId.set(UUID.randomUUID()) // signOut() landing before the IO dispatcher runs the request
                block.run()
            }
        }

        TokenRefreshHelper.refreshAccessToken(chat)

        verify(exactly = 0) { authService.getTransactionToken(any(), any(), any(), any()) }
        verify(exactly = 0) { chatStateListener.onChatRuntimeException(any()) }
        verify(exactly = 0) { socketListener.reportState(any()) }
    }
}
