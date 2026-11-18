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

import com.nice.cxonechat.internal.model.TokenRequestBody
import com.nice.cxonechat.model.makeConnection
import com.nice.cxonechat.tool.MockInterceptor
import io.kotest.matchers.shouldBe
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test

/**
 * Response handling for `AuthService.getTransactionToken`: successful-body deserialization,
 * independence of consecutive calls, and error/failure responses (401, 500). Happy-path token
 * retrieval and request construction live in [AuthServiceTest].
 */
internal class AuthServiceResponseTest {
    private val recorder by lazy { MockInterceptor() }
    private val connection by lazy { makeConnection() }
    private val builder by lazy {
        AuthServiceBuilder()
            .setConnection(connection)
            .setInterceptor(recorder)
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
}
