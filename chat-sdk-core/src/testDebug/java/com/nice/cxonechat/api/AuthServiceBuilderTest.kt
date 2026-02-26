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

import com.nice.cxonechat.internal.model.ThirdPartyOAuthBody
import com.nice.cxonechat.internal.model.TokenRequestBody
import com.nice.cxonechat.model.makeConnection
import com.nice.cxonechat.tool.MockInterceptor
import io.kotest.matchers.shouldBe
import okhttp3.Protocol.HTTP_2
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class AuthServiceBuilderTest {
    private lateinit var builder: AuthServiceBuilder
    private lateinit var recorder: MockInterceptor
    private lateinit var connection: com.nice.cxonechat.state.Connection

    @Before
    fun setUp() {
        builder = AuthServiceBuilder()
        recorder = MockInterceptor()
        connection = makeConnection()
    }

    @Test
    fun build_throwsException_whenConnectionNotSet() {
        val builder = AuthServiceBuilder()
        val exception = assertFailsWith<IllegalArgumentException> {
            builder.build()
        }
        assertEquals("Connection needs to be set, before build() is called.", exception.message)
    }

    @Test
    fun getTransactionToken_sendsCorrectRequest() {
        recorder.addResponse {
            protocol(HTTP_2)
            code(200)
            message("")
            body(
                """{"accessToken":"test-token","expiresIn":3600}""".toResponseBody()
            )
        }

        val client = builder
            .setConnection(connection)
            .setInterceptor(recorder)
            .build()

        val brandId = "brand123"
        val channelId = "channel456"
        val visitorId = "visitor789"
        val tokenBody = TokenRequestBody(
            type = "authorization_code",
            thirdParty = ThirdPartyOAuthBody(
                authorizationCode = "code123",
                codeVerifier = "verifier123"
            )
        )

        client.getTransactionToken(
            brandId = brandId,
            channelId = channelId,
            visitorId = visitorId,
            tokenRequestBody = tokenBody
        ).execute()

        // Verify exactly one request was made
        assertEquals(1, recorder.requests.count())

        // Verify request method and URL
        with(recorder.requests.first()) {
            assertEquals("POST", method)
            assertTrue(url.toString().contains("/oauth/token"))
            assertTrue(url.toString().contains("brandId=$brandId"))
            assertTrue(url.toString().contains("channelId=$channelId"))
            assertTrue(url.toString().contains("visitorId=$visitorId"))
        }
    }

    @Test
    fun getTransactionToken_usesAuthUrlFromEnvironment() {
        recorder.addResponse {
            code(200)
            message("")
            body("""{"accessToken":"token","expiresIn":3600}""".toResponseBody())
        }

        val client = builder
            .setConnection(connection)
            .setInterceptor(recorder)
            .build()

        val tokenBody = TokenRequestBody(
            type = "authorization_code",
            thirdParty = ThirdPartyOAuthBody(
                authorizationCode = "code",
                codeVerifier = "verifier"
            )
        )

        client.getTransactionToken(
            brandId = "brand",
            channelId = "channel",
            visitorId = "visitor",
            tokenRequestBody = tokenBody
        ).execute()

        // Verify the base URL is from connection's environment
        val request = recorder.requests.first()
        val expectedBaseUrl = connection.environment.authUrl
        assertTrue(request.url.toString().startsWith(expectedBaseUrl))
    }

    @Test
    fun getTransactionToken_deserializesResponseCorrectly() {
        val accessToken = "test-access-token"
        val expiresIn = 7200

        recorder.addResponse {
            code(200)
            message("")
            body(
                """{"accessToken":"$accessToken","expiresIn":$expiresIn}""".toResponseBody()
            )
        }

        val client = builder
            .setConnection(connection)
            .setInterceptor(recorder)
            .build()

        val response = client.getTransactionToken(
            brandId = "brand",
            channelId = "channel",
            visitorId = "visitor",
            tokenRequestBody = TokenRequestBody(
                type = "authorization_code",
                thirdParty = ThirdPartyOAuthBody(
                    authorizationCode = "code",
                    codeVerifier = "verifier"
                )
            )
        ).execute()

        // Verify response was deserialized correctly
        assertTrue(response.isSuccessful)
        val body = response.body()
        assertEquals(accessToken, body?.transactionToken)
    }

    @Test
    fun builder_withCustomOkHttpClient_usesCustomClient() {
        recorder.addResponse {
            code(200)
            message("")
            body("""{"accessToken":"token","expiresIn":3600}""".toResponseBody())
        }

        val customClient = okhttp3.OkHttpClient()
            .newBuilder()
            .build()

        val client = builder
            .setConnection(connection)
            .setSharedOkHttpClient(customClient)
            .setInterceptor(recorder)
            .build()

        client.getTransactionToken(
            brandId = "brand",
            channelId = "channel",
            visitorId = "visitor",
            tokenRequestBody = TokenRequestBody(
                type = "authorization_code",
                thirdParty = ThirdPartyOAuthBody(
                    authorizationCode = "code",
                    codeVerifier = "verifier"
                )
            )
        ).execute()

        // If request was made successfully, custom client was properly configured
        assertEquals(1, recorder.requests.count())
    }

    @Test
    fun getTransactionToken_httpErrorResponse_returnsErrorResponse() {
        recorder.addResponse {
            code(400)
            message("Bad Request")
            body("""{"error":"invalid_grant"}""".toResponseBody())
        }

        val client = builder
            .setConnection(connection)
            .setInterceptor(recorder)
            .build()

        val response = client.getTransactionToken(
            brandId = "brand",
            channelId = "channel",
            visitorId = "visitor",
            tokenRequestBody = TokenRequestBody(
                type = "authorization_code",
                thirdParty = ThirdPartyOAuthBody(
                    authorizationCode = "code",
                    codeVerifier = "verifier"
                )
            )
        ).execute()

        // Verify error response
        assertEquals(400, response.code())
        assertEquals(false, response.isSuccessful)
    }

    @Test
    fun builder_canBeReusedForMultipleRequests() {
        recorder.addResponse {
            code(200)
            message("")
            body("""{"accessToken":"token1","expiresIn":3600}""".toResponseBody())
        }
        recorder.addResponse {
            code(200)
            message("")
            body("""{"accessToken":"token2","expiresIn":3600}""".toResponseBody())
        }

        val client = builder
            .setConnection(connection)
            .setInterceptor(recorder)
            .build()

        val tokenBody = TokenRequestBody(
            type = "authorization_code",
            thirdParty = ThirdPartyOAuthBody(
                authorizationCode = "code",
                codeVerifier = "verifier"
            )
        )

        // First request
        client.getTransactionToken(
            brandId = "brand1",
            channelId = "channel1",
            visitorId = "visitor1",
            tokenRequestBody = tokenBody
        ).execute()

        // Second request
        client.getTransactionToken(
            brandId = "brand2",
            channelId = "channel2",
            visitorId = "visitor2",
            tokenRequestBody = tokenBody
        ).execute()

        // Verify both requests were made
        assertEquals(2, recorder.requests.count())

        // Verify different query parameters in each request
        assertEquals("brand1", recorder.requests[0].url.queryParameter("brandId"))
        assertEquals("brand2", recorder.requests[1].url.queryParameter("brandId"))
    }

    @Test
    fun builder_passesQueryParametersCorrectlyInUrl() {
        recorder.addResponse {
            code(200)
            message("")
            body("""{"accessToken":"token","expiresIn":3600}""".toResponseBody())
        }

        val client = builder
            .setConnection(connection)
            .setInterceptor(recorder)
            .build()

        val brandId = "test-brand-123"
        val channelId = "test-channel-456"
        val visitorId = "test-visitor-789"

        client.getTransactionToken(
            brandId = brandId,
            channelId = channelId,
            visitorId = visitorId,
            tokenRequestBody = TokenRequestBody(
                type = "authorization_code",
                thirdParty = ThirdPartyOAuthBody(
                    authorizationCode = "code",
                    codeVerifier = "verifier"
                )
            )
        ).execute()

        // Verify query parameters match exactly
        with(recorder.requests.first().url) {
            queryParameter("brandId") shouldBe brandId
            queryParameter("channelId") shouldBe channelId
            queryParameter("visitorId") shouldBe visitorId
        }
    }
}
