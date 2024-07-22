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

/**
 * convert this product to a cart item with the given count.
 *
 * @param quantity Quantity of Product in cart.
 * @return a cart item containing a quantity of the receiver.
 */
fun Product.toCartItem(quantity: Int) = Cart.Item(
    productId = id,
    title = title,
    price = price,
    quantity = quantity
)
