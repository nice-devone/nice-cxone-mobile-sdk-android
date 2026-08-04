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

package com.nice.cxonechat.sample.previewproviders

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.nice.cxonechat.sample.data.models.Product
import com.nice.cxonechat.sample.data.models.ProductList
import kotlinx.serialization.json.Json

/**
 * PreviewParameterProvider providing a list of products for the product list page.
 */
class ProductsParameterProvider : PreviewParameterProvider<List<Product>> {
    override val values: Sequence<List<Product>>
        get() = sequenceOf(items)

    private companion object {
        val items by lazy {
            Json.decodeFromString<ProductList>(JSON).items
        }

        private const val JSON = """
            {
    "products": [
        {
            "id": "121",
            "title": "iPhone 5s",
            "description": "The iPhone 5s is a smartphone that was designed and marketed by Apple Inc.",
            "price": 149,
            "discountPercentage": 7.17,
            "rating": 4.15,
            "stock": 164,
            "brand": "Apple",
            "category": "smartphones",
            "thumbnail": "file:///android_asset/store/images/121_thumbnail.webp",
            "images": [
                "file:///android_asset/store/images/121_0.webp",
                "file:///android_asset/store/images/121_1.webp",
                "file:///android_asset/store/images/121_2.webp"
            ]
        },
        {
            "id": "122",
            "title": "iPhone 6",
            "description": "The iPhone 6 is a smartphone designed and marketed by Apple Inc.",
            "price": 239,
            "discountPercentage": 10.27,
            "rating": 4.1,
            "stock": 47,
            "brand": "Apple",
            "category": "smartphones",
            "thumbnail": "file:///android_asset/store/images/122_thumbnail.webp",
            "images": [
                "file:///android_asset/store/images/122_0.webp",
                "file:///android_asset/store/images/122_1.webp",
                "file:///android_asset/store/images/122_2.webp"
            ]
        },
        {
            "id": "123",
            "title": "iPhone 13 Pro",
            "description": "The iPhone 13 Pro is a high-end smartphone released by Apple in 2021.",
            "price": 999,
            "discountPercentage": 8.62,
            "rating": 4.61,
            "stock": 90,
            "brand": "Apple",
            "category": "smartphones",
            "thumbnail": "file:///android_asset/store/images/123_thumbnail.webp",
            "images": [
                "file:///android_asset/store/images/123_0.webp",
                "file:///android_asset/store/images/123_1.webp",
                "file:///android_asset/store/images/123_2.webp"
            ]
        },
        {
            "id": "131",
            "title": "Samsung Galaxy S7",
            "description": "The Samsung Galaxy S7 is an Android smartphone produced by Samsung Electronics.",
            "price": 314,
            "discountPercentage": 10.23,
            "rating": 4.04,
            "stock": 149,
            "brand": "Samsung",
            "category": "smartphones",
            "thumbnail": "file:///android_asset/store/images/131_thumbnail.webp",
            "images": [
                "file:///android_asset/store/images/131_0.webp",
                "file:///android_asset/store/images/131_1.webp",
                "file:///android_asset/store/images/131_2.webp"
            ]
        },
        {
            "id": "125",
            "title": "Oppo A57",
            "description": "The Oppo A57 is an Android smartphone produced by Oppo.",
            "price": 149,
            "discountPercentage": 12.63,
            "rating": 4.07,
            "stock": 66,
            "brand": "Oppo",
            "category": "smartphones",
            "thumbnail": "file:///android_asset/store/images/125_thumbnail.webp",
            "images": [
                "file:///android_asset/store/images/125_0.webp",
                "file:///android_asset/store/images/125_1.webp",
                "file:///android_asset/store/images/125_2.webp"
            ]
        }
    ],
    "total": 5,
    "skip": 0,
    "limit": 5
}
        """
    }
}
