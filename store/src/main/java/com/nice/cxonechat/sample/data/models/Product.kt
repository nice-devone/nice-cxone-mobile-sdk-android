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

package com.nice.cxonechat.sample.data.models

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * A Product as represented on DummyJson.com.
 *
 * @param id Product id.
 * @param title Short product title.
 * @param description Longer product description.
 * @param price Unit price of item.
 * @param discountPercentage Percentage discount of product (ignored).
 * @param rating Rating of product 1-5.
 * @param stock Quantity on hand (ignored).
 * @param brand Brand of product.
 * @param category Category product falls in.
 * @param thumbnail Smaller thumbnail image of product.
 * @param images Array of image urls to be displayed with product.
 */
@Immutable
@Serializable
data class Product(
    val id: String,
    val title: String,
    val description: String,
    val price: Double,
    val discountPercentage: Double,
    val rating: Double,
    val stock: Int,
    val brand: String,
    val category: String,
    val thumbnail: String,
    val images: List<String>,
)
