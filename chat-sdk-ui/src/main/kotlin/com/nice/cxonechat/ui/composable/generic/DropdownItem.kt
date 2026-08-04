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

package com.nice.cxonechat.ui.composable.generic

/**
 * Represents a generic dropdown item with a label and a value.
 */
internal interface DropdownItem<KeyType> {
    /**
     * The label of the dropdown item, typically displayed in the UI.
     */
    val label: String

    /**
     * The value associated with the dropdown item, used for internal logic or selection.
     */
    val value: KeyType

    companion object {
        operator fun <KeyType> invoke(label: String, value: KeyType): DropdownItem<KeyType> = SimpleDropdownItem(label, value)

        private data class SimpleDropdownItem<KeyType>(
            override val label: String,
            override val value: KeyType,
        ) : DropdownItem<KeyType>
    }
}
