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

package com.nice.cxonechat.ui.util

import com.nice.cxonechat.ui.model.TreeFieldItem

/**
 * Recursive searches [TreeFieldItem] and builds a path to the node matching [test].
 *
 * @param Type type of value items in the included [TreeFieldItem].
 * @param test Test function to match the node being sought.
 * @return list of items to traverse to reach the matching node or null if no
 * match was found.
 */
@Suppress("ReturnCount")
internal fun <Type> Iterable<TreeFieldItem<Type>>.pathToNode(
    test: (TreeFieldItem<Type>) -> Boolean,
): List<TreeFieldItem<Type>>? {
    for (child in this) {
        if (test(child)) {
            return listOf(child)
        }

        child.children?.pathToNode(test)?.let {
            return listOf(child) + it
        }
    }
    return null
}

/**
 * Recursive searches [TreeFieldItem] and returns the matching item.
 *
 * @param Type type of value items in the included [TreeFieldItem].
 * @param test Test function to match the node being sought.
 * @return matching item or null if no match is found.
 */
internal fun <Type> Iterable<TreeFieldItem<Type>>.findRecursive(
    test: (TreeFieldItem<Type>) -> Boolean,
): TreeFieldItem<Type>? {
    return fold(null as TreeFieldItem<Type>?) { found, node ->
        found ?: if (test(node)) {
            node
        } else {
            node.children?.findRecursive(test)
        }
    }
}
