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

import com.nice.cxonechat.internal.serializer.Default.serializer
import com.nice.cxonechat.state.Connection
import okhttp3.Interceptor
import okhttp3.Interceptor.Chain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import org.jetbrains.annotations.TestOnly
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

internal abstract class BaseRetrofitServiceBuilder<T> {
    protected var connection: Connection? = null
    protected var interceptor: Interceptor? = null
    protected var okHttpClientBuilder: OkHttpClient.Builder? = null

    protected abstract fun self(): BaseRetrofitServiceBuilder<T>

    abstract fun build(): T

    fun setSharedOkHttpClient(okHttpClient: OkHttpClient): BaseRetrofitServiceBuilder<T> {
        this.okHttpClientBuilder = okHttpClient.newBuilder()
        return self()
    }

    fun setConnection(connection: Connection): BaseRetrofitServiceBuilder<T> {
        this.connection = connection
        return self()
    }

    @TestOnly
    fun setInterceptor(interceptor: Interceptor): BaseRetrofitServiceBuilder<T> {
        this.interceptor = interceptor
        return self()
    }

    protected fun buildClient(): OkHttpClient = (okHttpClientBuilder ?: OkHttpClient.Builder())
        .connectTimeout(40L, TimeUnit.SECONDS)
        .readTimeout(40L, TimeUnit.SECONDS)
        .writeTimeout(40L, TimeUnit.SECONDS)
        .addInterceptor(ContentTypeInterceptor())
        .addInterceptorNullable(interceptor)
        .build()

    private fun OkHttpClient.Builder.addInterceptorNullable(interceptor: Interceptor?) =
        if (interceptor == null) this else addInterceptor(interceptor)

    private class ContentTypeInterceptor : Interceptor {
        override fun intercept(chain: Chain): Response {
            val original = chain.request()
            val request = original.newBuilder()
                .addHeader("Content-Type", "application/json; charset=UTF-8")
                .build()
            return chain.proceed(request)
        }
    }

    protected fun jsonConverterFactory() = serializer.asConverterFactory("application/json; charset=UTF-8".toMediaType())
}
