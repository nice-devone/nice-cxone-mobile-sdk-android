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

package com.nice.cxonechat.sample.network

import com.nice.cxonechat.sample.data.models.Product
import com.nice.cxonechat.sample.data.models.ProductList
import com.nice.cxonechat.utilities.TaggingSocketFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Service specification for communication fetching products from https://dummyjson.com.
 */
interface DummyJsonService {
    /**
     * fetch the product list for the given category.
     *
     * @param category Category to fetch.
     * @return list of products in [category]
     */
    @GET("/products/category/{category}")
    suspend fun products(@Path("category") category: String): ProductList

    /**
     * fetch details of a specific product.
     *
     * @param productId Product ID of product to fetch.
     * @return Requested product details.
     */
    @GET("/product/{productId}")
    suspend fun product(@Path("productId") productId: String): Product

    @Suppress("UndocumentedPublicClass")
    companion object {
        private val client by lazy {
            OkHttpClient.Builder()
                .socketFactory(TaggingSocketFactory)
                .addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = BASIC
                    }
                )
                .build()
        }

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        /** singleton instance of DummyJsonService provided by retrofit. */
        val dummyJsonService: DummyJsonService by lazy {
            Retrofit.Builder()
                .baseUrl("https://dummyjson.com")
                .addConverterFactory(json.asConverterFactory("application/json; charset=UTF-8".toMediaType()))
                .client(client)
                .build()
                .create(DummyJsonService::class.java)
        }
    }
}
