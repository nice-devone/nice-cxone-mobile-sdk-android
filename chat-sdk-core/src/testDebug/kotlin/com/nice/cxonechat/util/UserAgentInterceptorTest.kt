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

package com.nice.cxonechat.util

import com.nice.cxonechat.util.UserAgent.NetworkUserAgent
import com.nice.cxonechat.util.UserAgent.USER_AGENT_HEADER_NAME
import com.nice.cxonechat.util.UserAgent.addUserAgentInterceptor
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotBeEmpty
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import okhttp3.Interceptor
import okhttp3.Protocol.HTTP_2
import okhttp3.Request
import okhttp3.Response
import org.junit.Before
import org.junit.Test
import io.github.ackeecz.useragent.UserAgent as AckeeUserAgent

internal class UserAgentInterceptorTest {

    private val testUserAgentPrefix = "TestApp/1.0.0 (com.test.app; build:42; Android 13; Model:67)"
    private lateinit var mockUserAgent: AckeeUserAgent

    @Before
    fun setUp() {
        mockUserAgent = mockk {
            every { getUserAgentString(any()) } answers {
                "$testUserAgentPrefix ${firstArg<String>()}"
            }
        }
    }

    private fun executeInterceptor(originalRequest: Request): Request {
        val capturedRequest = slot<Request>()
        val mockChain = mockk<Interceptor.Chain> {
            every { request() } returns originalRequest
            every { proceed(capture(capturedRequest)) } returns Response.Builder()
                .request(originalRequest)
                .protocol(HTTP_2)
                .code(200)
                .message("OK")
                .build()
        }

        val interceptors = okhttp3.OkHttpClient.Builder()
            .addUserAgentInterceptor(mockUserAgent)
            .interceptors()

        interceptors.first().intercept(mockChain)
        return capturedRequest.captured
    }

    @Test
    fun `interceptor adds User-Agent header`() {
        val request = executeInterceptor(Request.Builder().url("https://example.com").build())
        val userAgent = request.header(USER_AGENT_HEADER_NAME)
        userAgent.shouldNotBeNull()
        userAgent.shouldNotBeEmpty()
    }

    @Test
    fun `interceptor User-Agent contains SDK suffix`() {
        val request = executeInterceptor(Request.Builder().url("https://example.com").build())
        val userAgent = request.header(USER_AGENT_HEADER_NAME)
        userAgent.shouldNotBeNull()
        userAgent shouldContain NetworkUserAgent
    }

    @Test
    fun `interceptor User-Agent contains app context prefix`() {
        val request = executeInterceptor(Request.Builder().url("https://example.com").build())
        val userAgent = request.header(USER_AGENT_HEADER_NAME)
        userAgent.shouldNotBeNull()
        userAgent shouldContain testUserAgentPrefix
    }
}
