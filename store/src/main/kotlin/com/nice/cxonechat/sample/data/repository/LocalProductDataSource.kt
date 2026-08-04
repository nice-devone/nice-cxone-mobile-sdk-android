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

package com.nice.cxonechat.sample.data.repository

import android.content.Context
import com.nice.cxonechat.sample.data.models.Product
import com.nice.cxonechat.sample.data.models.ProductList
import org.koin.core.annotation.Single

/**
 * Local data source for store products backed by the pre-bundled asset [ASSET_PATH].
 *
 * @param context Application context used to open the asset.
 */
@Single
class LocalProductDataSource(
    private val context: Context,
) : AssetRepository<ProductList>(
    name = ASSET_PATH,
    type = ProductList::class,
) {

    /**
     * Load the full list of products from the bundled asset.
     *
     * @return List of [Product] items.
     * @throws IllegalStateException if the asset is missing or cannot be parsed.
     */
    suspend fun loadProducts(): List<Product> = load(context)?.items
        ?: error("Product catalog asset not found or failed to parse: $ASSET_PATH")

    @Suppress("UndocumentedPublicClass")
    companion object {
        internal const val ASSET_PATH = "store/products.json"
    }
}
