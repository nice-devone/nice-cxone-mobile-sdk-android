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
import com.nice.cxonechat.exceptions.RuntimeChatException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

internal class RetryParamsTest {

    @Test
    fun `action returns Pair on success`() {
        val call = mockk<Call<Void>>()
        val response = Response.success<Void>(null)
        every { call.execute() } returns response
        val params = createVisitorRetryParams(call, mockk(), null)
        val result = params.action()
        assertEquals(call, result.first)
        assertEquals(response, result.second)
    }


    @Test
    fun `action returns Pair on 4xx error`() {
        val call = mockk<Call<Void>>()
        val response = Response.error<Void>(404, "".toResponseBody(null))
        every { call.execute() } returns response
        val params = createVisitorRetryParams(call, mockk(), null)
        val result = params.action()
        assertEquals(call, result.first)
        assertEquals(response, result.second)
    }

    @Test(expected = RuntimeChatException.ServerCommunicationError::class)
    fun `action throws on server error`() {
        val call = mockk<Call<Void>>()
        val response = Response.error<Void>(500, "".toResponseBody(null))
        every { call.execute() } returns response
        val params = createVisitorRetryParams(call, mockk(), null)
        params.action()
    }

    @Test
    fun `onSuccess calls callback onResponse`() {
        val call = mockk<Call<Void>>()
        val response = Response.success<Void>(null)
        val callback = mockk<Callback<Void>>(relaxed = true)
        val params = createVisitorRetryParams(call, callback, null)
        params.onSuccess(Pair(call, response))
        verify { callback.onResponse(call, response) }
    }

    @Test
    fun `onFailure calls callback onFailure and chatStateListener`() {
        val call = mockk<Call<Void>>()
        val callback = mockk<Callback<Void>>(relaxed = true)
        val chatStateListener = mockk<ChatStateListener>(relaxed = true)
        val params = createVisitorRetryParams(call, callback, chatStateListener)
        val throwable = Throwable("fail")
        params.onFailure(throwable)
        verify { callback.onFailure(call, throwable) }
        verify {
            chatStateListener.onChatRuntimeException(
                match { it is RuntimeChatException.ServerCommunicationError }
            )
        }
    }

    @Test
    fun `onFailure works when chatStateListener is null`() {
        val call = mockk<Call<Void>>()
        val callback = mockk<Callback<Void>>(relaxed = true)
        val params = createVisitorRetryParams(call, callback, null)
        val throwable = Throwable("fail")
        params.onFailure(throwable)
        verify { callback.onFailure(call, throwable) }
        // No exception should be thrown
    }
}
