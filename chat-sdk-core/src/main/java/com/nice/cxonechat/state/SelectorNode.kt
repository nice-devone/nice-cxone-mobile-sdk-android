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

package com.nice.cxonechat.state

import com.nice.cxonechat.Public

/** Details of each entry in the list. */
@Public
interface SelectorNode {
    /** Computer targeted value for the item. */
    val nodeId: String

    /** Human-readable label for the item. */
    val label: String
}

/**
 * Search a sequence of nodes for a matching node id.
 *
 * @receiver Sequence to search.
 * @param nodeId Node id to locate.
 * @return First matching node or [null] if no match is found.
 */
@Public
fun Sequence<SelectorNode>.lookup(nodeId: String): SelectorNode? =
    firstOrNull { it.nodeId == nodeId }

/**
 * Test if a sequence of [SelectorNode] contains a given node id.
 *
 * @receiver Sequence to search.
 * @param nodeId Node id to locate.
 * @return [true] iff receiver contains a node with a matching [nodeId].
 */
@Public
fun Sequence<SelectorNode>.contains(nodeId: String): Boolean =
    lookup(nodeId) != null
