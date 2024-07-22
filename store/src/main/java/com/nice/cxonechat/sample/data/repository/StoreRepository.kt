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

package com.nice.cxonechat.sample.data.repository

import android.content.Context
import androidx.compose.runtime.Stable
import com.nice.cxonechat.sample.data.models.Cart
import com.nice.cxonechat.sample.data.models.Cart.Item
import com.nice.cxonechat.sample.data.models.Product
import com.nice.cxonechat.sample.data.operations.add
import com.nice.cxonechat.sample.data.operations.update
import com.nice.cxonechat.sample.network.DummyJsonService.Companion.dummyJsonService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

/**
 * Repository for the Store.  Maintains the list of products, the shoppers cart, and user name information.
 *
 * @param context Application Context for preferences access.
 */
@Single
class StoreRepository(
    val context: Context
) {
    private val productsCache = MutableStateFlow<Pair<String, List<Product>>?>(null)
    private val cartStore = MutableStateFlow(Cart(listOf()))

    /** the list of products to display. */
    @Stable
    val products = productsCache.asStateFlow().map { it?.second }

    /** Current cart. */
    @Stable
    val cart = cartStore.asStateFlow()

    /**
     * Fetch the products in the given category.
     *
     * @param category dummyjson.com category to fetch.
     * @return a [Result] containing the requested product list or appropriate
     * error.
     */
    suspend fun getProducts(
        category: String,
    ): Result<List<Product>> {
        val cached = productsCache.value

        return if (cached?.first == category) {
            Result.success(cached.second)
        } else {
            runCatching {
                dummyJsonService.products(category)
            }.map {
                it.items.sortedBy(Product::title).also { products ->
                    productsCache.value = category to products
                }
            }
        }
    }

    /**
     * Fetch the details of a given product from the store.
     *
     * @param productId product to fetch
     * @return [Result] containing either the request [Product] or an appropriate
     * error.
     */
    suspend fun getProduct(
        productId: String,
    ): Result<Product> = productsCache.value?.second
        ?.firstOrNull { it.id == productId }
        ?.let(Result.Companion::success)
        ?: runCatching {
            dummyJsonService.product(productId)
        }

    /**
     * Add a product to the current cart.
     *
     * If the product is currently in the cart the count will be incremented. If
     * the product is not currently in the cart it will be added.
     *
     * @param product [Product] to add to the cart.
     * @param quantity quantity to add, defaults to 1.
     */
    fun addToCart(product: Product, quantity: Int = 1) {
        val original = cartStore.value
        val updated = original.add(product, quantity)

        cartStore.compareAndSet(original, updated)
    }

    /**
     * Update the cart with a given [Cart.Item].
     *
     * If `item.quantity` ≤ 0, any line item in the cart with a matching
     * productId will be removed.  If `item.quantity` > 0, any matching line
     * item will be replaced with [item].
     *
     * @param item Cart item to update or remove.
     */
    fun updateCartItem(item: Item) {
        val original = cartStore.value
        val updated = original.update(item.productId, item.quantity)

        cartStore.compareAndSet(original, updated)
    }

    /**
     * Reset the cart contents to empty.
     */
    fun resetCart() {
        cartStore.value = Cart(listOf())
    }

    /**
     * Clear the stored user and cart information.
     */
    fun clear() {
        resetCart()
    }
}
