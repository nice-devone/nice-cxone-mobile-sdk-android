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

package com.nice.cxonechat.internal

import com.nice.cxonechat.api.RemoteService
import com.nice.cxonechat.api.RemoteServiceCaching
import com.nice.cxonechat.internal.serializer.Default
import com.nice.cxonechat.state.Connection
import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.OkHttpClient
import okhttp3.Response
import org.jetbrains.annotations.TestOnly
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import java.util.concurrent.TimeUnit

internal class RemoteServiceBuilder {

    @Suppress(
        "LateinitUsage" // Connection instance is required & checked during build()
    )
    private lateinit var connection: Connection
    private var interceptor: Interceptor? = null
    private var okHttpClientBuilder: OkHttpClient.Builder? = null

    fun setSharedOkHttpClient(okHttpClient: OkHttpClient) = apply {
        this.okHttpClientBuilder = okHttpClient.newBuilder()
    }

    fun setConnection(connection: Connection) = apply {
        this.connection = connection
    }

    @TestOnly
    fun setInterceptor(interceptor: Interceptor) = apply {
        this.interceptor = interceptor
    }

    fun build(): RemoteService {
        require(this::connection.isInitialized) { "Connection needs to be set, before build() is called." }
        var service: RemoteService = Retrofit.Builder()
            .client(buildClient())
            .baseUrl(connection.environment.chatUrl)
            .addConverterFactory(GsonConverterFactory.create(Default.serializer))
            .build()
            .create()
        service = RemoteServiceCaching(service)
        return service
    }

    // ---

    private fun buildClient() = (okHttpClientBuilder ?: OkHttpClient.Builder())
        .connectTimeout(40L, TimeUnit.SECONDS)
        .readTimeout(40L, TimeUnit.SECONDS)
        .writeTimeout(40L, TimeUnit.SECONDS)
        .addInterceptor(ContentTypeInterceptor())
        .addInterceptorNullable(interceptor)
        .build()

    private fun OkHttpClient.Builder.addInterceptorNullable(interceptor: Interceptor?) =
        if (interceptor == null) this else addInterceptor(interceptor)

    // ---

    private class ContentTypeInterceptor : Interceptor {

        override fun intercept(chain: Chain): Response {
            val original = chain.request()
            val request = original.newBuilder()
                .addHeader("Content-Type", "application/json; charset=UTF-8")
                .build()
            return chain.proceed(request)
        }
    }
}
