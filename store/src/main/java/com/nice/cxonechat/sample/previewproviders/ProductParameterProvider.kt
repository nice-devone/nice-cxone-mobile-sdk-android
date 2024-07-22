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

package com.nice.cxonechat.sample.previewproviders

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.nice.cxonechat.sample.data.models.Product

/**
 * PreviewParameterProvider that provides a list of Product to display.
 */
class ProductParameterProvider: PreviewParameterProvider<Product> {
    override val values: Sequence<Product>
        get() = ProductsParameterProvider().values.firstOrNull()?.asSequence() ?: sequenceOf()
}
