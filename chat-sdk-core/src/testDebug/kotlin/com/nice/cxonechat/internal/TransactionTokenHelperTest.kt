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

import com.nice.cxonechat.api.AuthService
import com.nice.cxonechat.internal.TransactionTokenHelper.TransactionTokenResult
import com.nice.cxonechat.internal.model.CustomerIdentityModel
import com.nice.cxonechat.internal.model.TokenRequestBody
import com.nice.cxonechat.internal.model.TransactionTokenModel
import com.nice.cxonechat.storage.ValueStorage
import io.mockk.every
import io.mockk.mockk
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Tests for [TransactionTokenHelper.getTransactionToken], which is the shared
 * network utility called on every OAuth token exchange.
 */
internal class TransactionTokenHelperTest {

    private lateinit var entrails: ChatEntrails
    private lateinit var authService: AuthService
    private lateinit var storage: ValueStorage

    private val brandId = "1001"
    private val channelId = "chan-abc"
    private val visitorId = UUID.randomUUID().toString()
    private val tokenBody = TokenRequestBody(
        type = "customer-identity",
        customerIdentity = CustomerIdentityModel(
            idOnExternalPlatform = "user-1",
            firstName = "Alice",
            lastName = "Smith"
        )
    )

    @Before
    fun setUp() {
        storage = mockk(relaxed = true)
        authService = mockk()
        entrails = mockk(relaxed = true)
        every { entrails.authService } returns authService
        every { entrails.storage } returns storage
    }

    // ── success ──────────────────────────────────────────────────────────────

    @Test
    fun `returns Success when API call succeeds and body is non-null`() {
        val model = mockk<TransactionTokenModel>(relaxed = true)
        val response = Response.success(model)
        every { authService.getTransactionToken(brandId, channelId, visitorId, tokenBody) } returns mockk {
            every { execute() } returns response
        }

        val result = TransactionTokenHelper.getTransactionToken(entrails, brandId, channelId, visitorId, tokenBody)

        assertIs<TransactionTokenResult.Success>(result)
        assertEquals(model, result.transactionTokenModel)
    }

    // ── network failure ───────────────────────────────────────────────────────

    @Test
    fun `returns Error with httpStatusCode -1 when network throws IOException`() {
        every { authService.getTransactionToken(brandId, channelId, visitorId, tokenBody) } returns mockk {
            every { execute() } throws IOException("timeout")
        }

        val result = TransactionTokenHelper.getTransactionToken(entrails, brandId, channelId, visitorId, tokenBody)

        assertIs<TransactionTokenResult.Error>(result)
        assertEquals(-1, result.httpStatusCode)
        assertTrue(result.message.contains("IOException"))
        assertFalse(result.isTokenRejection)
    }

    // ── HTTP 401 → isTokenRejection ───────────────────────────────────────────

    @Test
    fun `returns Error with isTokenRejection true when server responds 401`() {
        val response = Response.error<TransactionTokenModel>(
            401,
            "".toResponseBody(null)
        )
        every { authService.getTransactionToken(brandId, channelId, visitorId, tokenBody) } returns mockk {
            every { execute() } returns response
        }

        val result = TransactionTokenHelper.getTransactionToken(entrails, brandId, channelId, visitorId, tokenBody)

        assertIs<TransactionTokenResult.Error>(result)
        assertEquals(401, result.httpStatusCode)
        assertTrue(result.isTokenRejection, "HTTP 401 must set isTokenRejection = true")
    }

    // ── HTTP 400 → not a token rejection ─────────────────────────────────────

    @Test
    fun `returns Error with isTokenRejection false when server responds 400`() {
        val response = Response.error<TransactionTokenModel>(
            400,
            "".toResponseBody(null)
        )
        every { authService.getTransactionToken(brandId, channelId, visitorId, tokenBody) } returns mockk {
            every { execute() } returns response
        }

        val result = TransactionTokenHelper.getTransactionToken(entrails, brandId, channelId, visitorId, tokenBody)

        assertIs<TransactionTokenResult.Error>(result)
        assertEquals(400, result.httpStatusCode)
        assertFalse(result.isTokenRejection, "HTTP 400 must NOT set isTokenRejection")
    }

    // ── HTTP 403 → not a token rejection ─────────────────────────────────────

    @Test
    fun `returns Error with isTokenRejection false when server responds 403`() {
        val response = Response.error<TransactionTokenModel>(403, "".toResponseBody(null))
        every { authService.getTransactionToken(brandId, channelId, visitorId, tokenBody) } returns mockk {
            every { execute() } returns response
        }

        val result = TransactionTokenHelper.getTransactionToken(entrails, brandId, channelId, visitorId, tokenBody)

        assertIs<TransactionTokenResult.Error>(result)
        assertEquals(403, result.httpStatusCode)
        assertFalse(result.isTokenRejection)
    }

    // ── HTTP 500 → not a token rejection ─────────────────────────────────────

    @Test
    fun `returns Error with isTokenRejection false when server responds 500`() {
        val response = Response.error<TransactionTokenModel>(500, "".toResponseBody(null))
        every { authService.getTransactionToken(brandId, channelId, visitorId, tokenBody) } returns mockk {
            every { execute() } returns response
        }

        val result = TransactionTokenHelper.getTransactionToken(entrails, brandId, channelId, visitorId, tokenBody)

        assertIs<TransactionTokenResult.Error>(result)
        assertEquals(500, result.httpStatusCode)
        assertFalse(result.isTokenRejection)
    }

    // ── network exception: isTokenRejection and message ───────────────────────

    @Test
    fun `network exception sets isTokenRejection false`() {
        every { authService.getTransactionToken(brandId, channelId, visitorId, tokenBody) } returns mockk {
            every { execute() } throws IOException("connection reset")
        }

        val result = TransactionTokenHelper.getTransactionToken(entrails, brandId, channelId, visitorId, tokenBody)

        assertIs<TransactionTokenResult.Error>(result)
        assertFalse(result.isTokenRejection)
    }

    @Test
    fun `network exception message contains exception details`() {
        every { authService.getTransactionToken(brandId, channelId, visitorId, tokenBody) } returns mockk {
            every { execute() } throws IOException("DNS lookup failed")
        }

        val result = TransactionTokenHelper.getTransactionToken(entrails, brandId, channelId, visitorId, tokenBody)

        assertIs<TransactionTokenResult.Error>(result)
        assertTrue(
            result.message.contains("DNS lookup failed"),
            "Error message should include exception message; got: ${result.message}"
        )
    }

    // ── error body JSON parsing ───────────────────────────────────────────────

    @Test
    fun `uses errorMessage from JSON error body when present`() {
        val errorJson = """{"error":{"errorCode":"INVALID_TOKEN","errorMessage":"Token has expired"}}"""
        val response = Response.error<TransactionTokenModel>(
            403,
            errorJson.toResponseBody(null)
        )
        every { authService.getTransactionToken(brandId, channelId, visitorId, tokenBody) } returns mockk {
            every { execute() } returns response
        }

        val result = TransactionTokenHelper.getTransactionToken(entrails, brandId, channelId, visitorId, tokenBody)

        assertIs<TransactionTokenResult.Error>(result)
        assertEquals("Token has expired", result.message)
        assertEquals(403, result.httpStatusCode)
        assertFalse(result.isTokenRejection)
    }

    @Test
    fun `falls back to generic message when error body has no errorMessage field`() {
        val errorJson = """{"error":{"errorCode":"SOME_CODE"}}"""
        val response = Response.error<TransactionTokenModel>(403, errorJson.toResponseBody(null))
        every { authService.getTransactionToken(brandId, channelId, visitorId, tokenBody) } returns mockk {
            every { execute() } returns response
        }

        val result = TransactionTokenHelper.getTransactionToken(entrails, brandId, channelId, visitorId, tokenBody)

        assertIs<TransactionTokenResult.Error>(result)
        assertTrue(result.message.isNotBlank())
    }

    @Test
    fun `falls back to generic message when error body is null`() {
        every { authService.getTransactionToken(brandId, channelId, visitorId, tokenBody) } returns mockk {
            every { execute() } returns mockk {
                every { isSuccessful } returns false
                every { code() } returns 503
                every { message() } returns "Service Unavailable"
                every { errorBody() } returns null
            }
        }

        val result = TransactionTokenHelper.getTransactionToken(entrails, brandId, channelId, visitorId, tokenBody)

        assertIs<TransactionTokenResult.Error>(result)
        assertTrue(result.message.isNotBlank())
    }

    @Test
    fun `falls back to generic message when error body JSON is malformed`() {
        val response = Response.error<TransactionTokenModel>(
            500,
            "not-json".toResponseBody(null)
        )
        every { authService.getTransactionToken(brandId, channelId, visitorId, tokenBody) } returns mockk {
            every { execute() } returns response
        }

        val result = TransactionTokenHelper.getTransactionToken(entrails, brandId, channelId, visitorId, tokenBody)

        assertIs<TransactionTokenResult.Error>(result)
        assertEquals(500, result.httpStatusCode)
        assertTrue(result.message.isNotBlank())
    }

    // ── httpStatusCode propagation ────────────────────────────────────────────

    @Test
    fun `httpStatusCode is propagated from HTTP response code`() {
        val response = Response.error<TransactionTokenModel>(429, "".toResponseBody(null))
        every { authService.getTransactionToken(brandId, channelId, visitorId, tokenBody) } returns mockk {
            every { execute() } returns response
        }

        val result = TransactionTokenHelper.getTransactionToken(entrails, brandId, channelId, visitorId, tokenBody)

        assertIs<TransactionTokenResult.Error>(result)
        assertEquals(429, result.httpStatusCode)
    }

    // ── empty body ────────────────────────────────────────────────────────────

    @Test
    fun `returns Error when HTTP 200 but body is null`() {
        val response = Response.success<TransactionTokenModel>(null)
        every { authService.getTransactionToken(brandId, channelId, visitorId, tokenBody) } returns mockk {
            every { execute() } returns response
        }

        val result = TransactionTokenHelper.getTransactionToken(entrails, brandId, channelId, visitorId, tokenBody)

        assertIs<TransactionTokenResult.Error>(result)
        assertTrue(result.message.contains("empty response body"), "Expected empty-body message, got: ${result.message}")
    }
}
