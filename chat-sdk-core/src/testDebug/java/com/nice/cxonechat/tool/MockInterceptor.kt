/*
 * Copyright (c) 2021-2024. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.tool

import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.Protocol.HTTP_2
import okhttp3.Request
import okhttp3.Response
import okhttp3.Response.Builder

internal class MockInterceptor : Interceptor {
    val requests = mutableListOf<Request>()
    val responders = mutableListOf<Builder.(Request) -> Builder>()
    val responseIterator by lazy { responders.iterator() }

    val nextResponse: Builder.(Request) -> Builder
        get() = if(responseIterator.hasNext()) {
            responseIterator.next()
        } else {
            {
                code(404)
                message("")
            }
        }

    override fun intercept(chain: Chain): Response {
        val request = chain.request()

        requests += request
        return Builder()
            .request(request)
            .protocol(HTTP_2)
            .nextResponse(request)
            .build()
    }

    fun addResponse(builder: Builder.(Request) -> Unit) {
        responders.add {
            builder(it)
            this
        }
    }
}
