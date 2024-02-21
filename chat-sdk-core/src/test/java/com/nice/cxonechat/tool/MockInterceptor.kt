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
