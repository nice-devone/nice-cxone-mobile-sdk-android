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

package com.nice.cxonechat.sample.data.operations

import com.nice.cxonechat.sample.data.models.Cart
import com.nice.cxonechat.sample.data.models.Product

/** returns true if the cart is empty (i.e., has no items). */
val Cart.isEmpty get() = items.isEmpty()

/** total value of cart, sum of the extended price of all line items. */
val Cart.total get() = items.fold(0.0) { total, item -> total + item.extended }

/**
 * Find a product in the cart by productId.
 *
 * @param productId Product to locate.
 * @return Contained product or null if no matching product is in the cart.
 */
fun Cart.find(productId: String) = items.firstOrNull { it.productId == productId }

/**
 * Test if a product is contained in the cart.
 *
 * @param productId Product id to test.
 * @return true iff the cart contains an item matching product.
 */
fun Cart.contains(productId: String) = find(productId = productId) != null

/**
 * Return a new cart, updating the quantity of matching items in the cart.
 *
 * @param productId Product ID to update.
 * @param quantity new quantity of Product. If the quantity is updated to 0, the
 * product is removed from the cart.
 * @return a new cart with the updated products.
 */
fun Cart.update(productId: String, quantity: Int) = copy(
    items = items.mapNotNull {
        when {
            it.productId != productId -> it
            quantity > 0 -> it.copy(quantity = quantity)
            else -> null
        }
    }
)

/**
 * Return a new cart, adding the given product.
 *
 * @param product product to add.
 * @param quantity quantity of product to add.
 */
fun Cart.add(product: Product, quantity: Int = 1) =
    when (val current = find(product.id)) {
        null -> copy(items = items + product.toCartItem(quantity))
        else -> update(product.id, current.quantity + quantity)
    }
