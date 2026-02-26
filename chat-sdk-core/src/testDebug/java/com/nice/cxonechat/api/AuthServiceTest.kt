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

package com.nice.cxonechat.api

import com.nice.cxonechat.internal.model.CustomerIdentityModel
import com.nice.cxonechat.internal.model.TokenRequestBody
import com.nice.cxonechat.model.makeConnection
import com.nice.cxonechat.tool.MockInterceptor
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import kotlin.test.assertEquals

internal class AuthServiceTest {
    private val recorder by lazy { MockInterceptor() }
    private val connection by lazy { makeConnection() }
    private val builder by lazy {
        AuthServiceBuilder()
            .setConnection(connection)
            .setInterceptor(recorder)
    }

    @Test
    fun getTransactionToken_successfullyFetchesToken() {
        val authService = builder.build()
        val brandId = connection.brandId.toString()
        val channelId = connection.channelId
        val visitorId = connection.visitorId.toString()
        val tokenBody = TokenRequestBody(
            type = "customer-identity",
            customerIdentity = CustomerIdentityModel(
                idOnExternalPlatform = "john-123",
                firstName = "John",
                lastName = "Doe"
            )
        )
        val token = "test-transaction-token-12345"
        val expiresIn = 3600L

        recorder.addResponse { _ ->
            code(200)
            message("")
            body(
                """{"accessToken":"$token","expiresIn":$expiresIn,"createdAt":"2026-01-30T12:00:00.000z"}""".toResponseBody()
            )
        }

        val response = authService.getTransactionToken(
            brandId = brandId,
            channelId = channelId,
            visitorId = visitorId,
            tokenRequestBody = tokenBody
        ).execute()

        response.isSuccessful shouldBe true
        response.body() shouldNotBe null
        response.body()?.transactionToken shouldBe token
        response.body()?.expiresIn shouldBe expiresIn
    }

    @Test
    fun getTransactionToken_withCustomerIdentity() {
        val authService = builder.build()
        val brandId = connection.brandId.toString()
        val channelId = connection.channelId
        val visitorId = connection.visitorId.toString()
        val firstName = "Jane"
        val lastName = "Smith"

        val tokenBody = TokenRequestBody(
            type = "customer-identity",
            customerIdentity = CustomerIdentityModel(
                idOnExternalPlatform = "jane-456",
                firstName = firstName,
                lastName = lastName
            )
        )

        recorder.addResponse { _ ->
            code(200)
            message("")
            body(
                """{"accessToken":"token-customer-123","expiresIn":7200,"createdAt":"2026-01-30T12:00:00.000z"}""".toResponseBody()
            )
        }

        val response = authService.getTransactionToken(
            brandId = brandId,
            channelId = channelId,
            visitorId = visitorId,
            tokenRequestBody = tokenBody
        ).execute()

        assertEquals(1, recorder.requests.count())
        response.isSuccessful shouldBe true

        with(recorder.requests.first()) {
            assertEquals("POST", method)
            url.toString().contains("/oauth/token") shouldBe true
            url.toString().contains("brandId=$brandId") shouldBe true
            url.toString().contains("channelId=$channelId") shouldBe true
            url.toString().contains("visitorId=$visitorId") shouldBe true
        }
    }

    @Test
    fun getTransactionToken_requestIncludesAllQueryParameters() {
        val authService = builder.build()
        val brandId = "brand-123"
        val channelId = "channel-456"
        val visitorId = "visitor-789"
        val tokenBody = TokenRequestBody()

        recorder.addResponse { _ ->
            code(200)
            message("")
            body("""{"accessToken":"token","expiresIn":1000,"createdAt":"2026-01-30T12:00:00.000z"}""".toResponseBody())
        }

        authService.getTransactionToken(
            brandId = brandId,
            channelId = channelId,
            visitorId = visitorId,
            tokenRequestBody = tokenBody
        ).execute()

        with(recorder.requests.first()) {
            val urlString = url.toString()
            urlString.contains("brandId=$brandId") shouldBe true
            urlString.contains("channelId=$channelId") shouldBe true
            urlString.contains("visitorId=$visitorId") shouldBe true
        }
    }

    @Test
    fun getTransactionToken_withNullValues() {
        val authService = builder.build()
        val brandId = connection.brandId.toString()
        val channelId = connection.channelId
        val visitorId = connection.visitorId.toString()
        val tokenBody = TokenRequestBody(
            type = null,
            customerIdentity = null,
            thirdParty = null
        )

        recorder.addResponse { _ ->
            code(200)
            message("")
            body("""{"accessToken":"token-null","expiresIn":5000,"createdAt":"2026-01-30T12:00:00.000z"}""".toResponseBody())
        }

        val response = authService.getTransactionToken(
            brandId = brandId,
            channelId = channelId,
            visitorId = visitorId,
            tokenRequestBody = tokenBody
        ).execute()

        response.isSuccessful shouldBe true
        response.body() shouldNotBe null
    }

    @Test
    fun getTransactionToken_failureResponse() {
        val authService = builder.build()
        val brandId = connection.brandId.toString()
        val channelId = connection.channelId
        val visitorId = connection.visitorId.toString()

        recorder.addResponse { _ ->
            code(401)
            message("Unauthorized")
            body("""{"error":"Invalid credentials"}""".toResponseBody())
        }

        val response = authService.getTransactionToken(
            brandId = brandId,
            channelId = channelId,
            visitorId = visitorId,
            tokenRequestBody = TokenRequestBody()
        ).execute()

        response.isSuccessful shouldBe false
        response.code() shouldBe 401
    }

    @Test
    fun getTransactionToken_serverErrorResponse() {
        val authService = builder.build()
        val brandId = connection.brandId.toString()
        val channelId = connection.channelId
        val visitorId = connection.visitorId.toString()

        recorder.addResponse { _ ->
            code(500)
            message("Internal Server Error")
            body("""{"error":"Server error"}""".toResponseBody())
        }

        val response = authService.getTransactionToken(
            brandId = brandId,
            channelId = channelId,
            visitorId = visitorId,
            tokenRequestBody = TokenRequestBody()
        ).execute()

        response.isSuccessful shouldBe false
        response.code() shouldBe 500
    }

    @Test
    fun getTransactionToken_tokenExpiration() {
        val authService = builder.build()
        val brandId = connection.brandId.toString()
        val channelId = connection.channelId
        val visitorId = connection.visitorId.toString()
        val expiresIn = 10L // 10 seconds

        recorder.addResponse { _ ->
            code(200)
            message("")
            body("""{"accessToken":"short-lived-token","expiresIn":$expiresIn,"createdAt":"2026-01-30T12:00:00.000z"}""".toResponseBody())
        }

        val response = authService.getTransactionToken(
            brandId = brandId,
            channelId = channelId,
            visitorId = visitorId,
            tokenRequestBody = TokenRequestBody()
        ).execute()

        response.isSuccessful shouldBe true
        val tokenModel = response.body()
        tokenModel shouldNotBe null
        tokenModel?.expiresIn shouldBe expiresIn
    }

    @Test
    fun getTransactionToken_postMethodIsUsed() {
        val authService = builder.build()
        val brandId = connection.brandId.toString()
        val channelId = connection.channelId
        val visitorId = connection.visitorId.toString()

        recorder.addResponse { _ ->
            code(200)
            message("")
            body("""{"accessToken":"token","expiresIn":1000,"createdAt":"2026-01-30T12:00:00.000z"}""".toResponseBody())
        }

        authService.getTransactionToken(
            brandId = brandId,
            channelId = channelId,
            visitorId = visitorId,
            tokenRequestBody = TokenRequestBody()
        ).execute()

        with(recorder.requests.first()) {
            method shouldBe "POST"
        }
    }

    @Test
    fun getTransactionToken_requestBodySerialization() {
        val authService = builder.build()
        val brandId = connection.brandId.toString()
        val channelId = connection.channelId
        val visitorId = connection.visitorId.toString()
        val tokenBody = TokenRequestBody(
            type = "customer-identity",
            customerIdentity = CustomerIdentityModel(
                idOnExternalPlatform = "test-789",
                firstName = "Test",
                lastName = "User"
            )
        )

        recorder.addResponse { _ ->
            code(200)
            message("")
            body("""{"accessToken":"token","expiresIn":1000,"createdAt":"2026-01-30T12:00:00.000z"}""".toResponseBody())
        }

        authService.getTransactionToken(
            brandId = brandId,
            channelId = channelId,
            visitorId = visitorId,
            tokenRequestBody = tokenBody
        ).execute()

        assertEquals(1, recorder.requests.count())
        with(recorder.requests.first()) {
            body shouldNotBe null
        }
    }

    @Test
    fun getTransactionToken_responseBodyDeserialization() {
        val authService = builder.build()
        val brandId = connection.brandId.toString()
        val channelId = connection.channelId
        val visitorId = connection.visitorId.toString()
        val expectedToken = "deserialized-token-xyz"
        val expectedExpiry = 9999L

        recorder.addResponse { _ ->
            code(200)
            message("")
            body(
                """{"accessToken":"$expectedToken","expiresIn":$expectedExpiry,"createdAt":"2026-01-30T12:00:00.000z"}""".toResponseBody()
            )
        }

        val response = authService.getTransactionToken(
            brandId = brandId,
            channelId = channelId,
            visitorId = visitorId,
            tokenRequestBody = TokenRequestBody()
        ).execute()

        val body = response.body()
        body?.transactionToken shouldBe expectedToken
        body?.expiresIn shouldBe expectedExpiry
    }

    @Test
    fun getTransactionToken_multipleCallsAreIndependent() {
        val authService = builder.build()
        val brandId = connection.brandId.toString()
        val channelId = connection.channelId
        val visitorId = connection.visitorId.toString()

        // First call
        recorder.addResponse { _ ->
            code(200)
            message("")
            body("""{"accessToken":"token1","expiresIn":1000,"createdAt":"2026-01-30T12:00:00.000z"}""".toResponseBody())
        }

        // Second call
        recorder.addResponse { _ ->
            code(200)
            message("")
            body("""{"accessToken":"token2","expiresIn":2000,"createdAt":"2026-01-30T12:00:00.000z"}""".toResponseBody())
        }

        val response1 = authService.getTransactionToken(
            brandId = brandId,
            channelId = channelId,
            visitorId = visitorId,
            tokenRequestBody = TokenRequestBody()
        ).execute()

        val response2 = authService.getTransactionToken(
            brandId = brandId,
            channelId = channelId,
            visitorId = visitorId,
            tokenRequestBody = TokenRequestBody()
        ).execute()

        response1.body()?.transactionToken shouldBe "token1"
        response2.body()?.transactionToken shouldBe "token2"
        recorder.requests.size shouldBe 2
    }

    @Test
    fun getTransactionToken_requestContentTypeHeader() {
        val authService = builder.build()
        val brandId = connection.brandId.toString()
        val channelId = connection.channelId
        val visitorId = connection.visitorId.toString()

        recorder.addResponse { _ ->
            code(200)
            message("")
            body("""{"accessToken":"token","expiresIn":1000,"createdAt":"2026-01-30T12:00:00.000z"}""".toResponseBody())
        }

        authService.getTransactionToken(
            brandId = brandId,
            channelId = channelId,
            visitorId = visitorId,
            tokenRequestBody = TokenRequestBody()
        ).execute()

        with(recorder.requests.first()) {
            headers["Content-Type"] shouldBe "application/json; charset=UTF-8"
        }
    }
}

