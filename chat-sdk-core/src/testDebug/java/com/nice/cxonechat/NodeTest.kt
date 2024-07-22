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

package com.nice.cxonechat

import com.nice.cxonechat.model.nextNode
import com.nice.cxonechat.state.HierarchyNodeInternal
import com.nice.cxonechat.state.HierarchyNodeInternal.Companion.toNodeIterable
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Suppress("UNCHECKED_CAST")
internal class NodeTest {
    @Test
    fun create_from_ordered_list() {
        val root = nextNode()
        val child = nextNode(root.name)
        val list = listOf(
            root,
            child,
        )
        val nodes = list.toNodeIterable().toList()
        assertEquals(1, nodes.size)
        val node = nodes[0] as HierarchyNodeInternal<String>
        assertEquals(root.name, node.nodeId)
        assertEquals(root.value, node.label)
        assertEquals(1, node.nodes.size)
        assertEquals(child.name, node.nodes[0].nodeId)
        assertEquals(child.value, node.nodes[0].label)
        assertEquals(0, node.nodes[0].nodes.size)
    }

    @Test
    fun create_from_unordered_list() {
        val root = nextNode()
        val child = nextNode(root.name)
        val list = listOf(
            child,
            root,
        )
        val nodes = list.toNodeIterable().toList() as List<HierarchyNodeInternal<String>>
        assertEquals(1, nodes.size)
        assertEquals(root.name, nodes[0].nodeId)
        assertEquals(root.value, nodes[0].label)
        assertEquals(1, nodes[0].nodes.size)
        assertEquals(child.name, nodes[0].nodes[0].nodeId)
        assertEquals(child.value, nodes[0].nodes[0].label)
        assertEquals(0, nodes[0].nodes[0].nodes.size)
    }

    @Test
    @Suppress(
        "VariableNaming"
    )
    fun create_from_complex_list() {
        val root1 = nextNode()
        val child1_1 = nextNode(root1.name)
        val child1_1_1 = nextNode(child1_1.name)
        val child2_1 = nextNode(root1.name)
        val root2 = nextNode()
        val list = listOf(
            child2_1,
            root2,
            child1_1_1,
            root1,
            child1_1
        )
        val nodes = list.toNodeIterable().toList() as List<HierarchyNodeInternal<String>>
        assertEquals(2, nodes.size)
        val root1Index = nodes.indexOfFirst { it.nodeId == root1.name }
        assertTrue(root1Index >= 0, "Root1 is not in root nodes")
        assertEquals(root1.value, nodes[root1Index].label)
        assertEquals(2, nodes[root1Index].nodes.size)
        val child1_1Index = nodes[root1Index].nodes.indexOfFirst { it.nodeId == child1_1.name }
        assertTrue(child1_1Index >= 0, "Child1_1 is not in it's root node")
        assertEquals(child1_1.value, nodes[root1Index].nodes[child1_1Index].label)
        assertEquals(1, nodes[root1Index].nodes[child1_1Index].nodes.size)
        assertEquals(child1_1_1.name, nodes[root1Index].nodes[child1_1Index].nodes[0].nodeId)
        assertEquals(child1_1_1.value, nodes[root1Index].nodes[child1_1Index].nodes[0].label)
        assertEquals(0, nodes[root1Index].nodes[child1_1Index].nodes[0].nodes.size)
    }
}
