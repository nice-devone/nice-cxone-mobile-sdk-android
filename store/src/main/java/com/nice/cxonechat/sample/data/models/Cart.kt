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

import javax.annotation.concurrent.Immutable

/**
 * A typical shopping cart containing a list of items.
 *
 * @param items Items contained in the shopping cart.
 */
@Immutable
data class Cart(
    val items: List<Item> = listOf()
) {
    /**
     * An item in the shopping cart.
     *
     * @param productId unique product identifier.
     * @param title text description of product.
     * @param price unit price of product.
     * @param quantity quantity of product in cart.
     */
    @Immutable
    data class Item(
        val productId: String,
        val title: String,
        val price: Double,
        val quantity: Int,
    ) {
        /** extended price of product line (price * quantity). */
        val extended get() = price * quantity.toDouble()
    }
}
