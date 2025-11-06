/*
 * Copyright (c) 2021-2025. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.ui.domain.model

internal interface TreeFieldItem<ValueType> {
    val label: String
    val value: ValueType
    val children: Iterable<TreeFieldItem<ValueType>>?

    val isLeaf: Boolean
        get() = children == null

    companion object {
        operator fun <ValueType> invoke(
            label: String,
            value: ValueType,
            children: List<TreeFieldItem<ValueType>>? = null,
        ): TreeFieldItem<ValueType> = SimpleTreeFieldItem(label, value, children)
    }
}
