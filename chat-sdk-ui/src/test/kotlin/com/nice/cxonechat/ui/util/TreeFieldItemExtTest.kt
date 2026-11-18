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

package com.nice.cxonechat.ui.util

import com.nice.cxonechat.ui.domain.model.TreeFieldItem
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Tests for [pathToNode] and [findRecursive] in TreeFieldItemExt.
 *
 * Neither helper decides the submitted value — that is written directly from `item.response.value`
 * in `CVFieldList`. [pathToNode]'s one production caller feeds the hierarchy field's `expanded`
 * set, so a traversal bug misrenders which nodes appear expanded; [findRecursive] is currently
 * referenced only from a `@Preview`. They are worth pinning anyway: shared recursive utilities
 * with one live caller.
 *
 * Both algorithms are pre-order and first-match, which is the load-bearing part: [pathToNode]
 * tests a node before recursing into its children, and [findRecursive]'s `found ?:` fold keeps the
 * earliest match. Trees with a single matching node cannot detect a swap to post-order or
 * last-match, so each algorithm gets an ambiguous-match test in both dimensions below — two
 * matching siblings for breadth, a matching ancestor and descendant for depth.
 */
internal class TreeFieldItemExtTest {

    private fun node(
        label: String,
        children: List<TreeFieldItem<String>>? = null,
    ): TreeFieldItem<String> = TreeFieldItem(label, label, children)

    // ── pathToNode ────────────────────────────────────────────────────────────

    @Test
    fun `pathToNode returns null for an empty list`() {
        assertNull(emptyList<TreeFieldItem<String>>().pathToNode { true })
    }

    @Test
    fun `pathToNode returns single-element list when root matches`() {
        val root = node("root")
        val result = listOf(root).pathToNode { it.label == "root" }
        assertEquals(listOf(root), result)
    }

    @Test
    fun `pathToNode returns null when no node in a flat list matches`() {
        val items = listOf(node("a"), node("b"), node("c"))
        assertNull(items.pathToNode { it.label == "missing" })
    }

    @Test
    fun `pathToNode returns path containing parent and child when match is one level deep`() {
        val child = node("child")
        val root = node("root", listOf(child))
        val result = listOf(root).pathToNode { it.label == "child" }
        assertEquals(listOf(root, child), result)
    }

    @Test
    fun `pathToNode returns full path through multiple levels of nesting`() {
        val leaf = node("leaf")
        val mid = node("mid", listOf(leaf))
        val root = node("root", listOf(mid))
        val result = listOf(root).pathToNode { it.label == "leaf" }
        assertEquals(listOf(root, mid, leaf), result)
    }

    @Test
    fun `pathToNode finds match in a sibling branch when earlier roots do not contain the target`() {
        val target = node("target")
        val rootA = node("rootA", listOf(node("childA")))
        val rootB = node("rootB", listOf(target))
        val result = listOf(rootA, rootB).pathToNode { it.label == "target" }
        assertEquals(listOf(rootB, target), result)
    }

    @Test
    fun `pathToNode returns null when no node at any depth matches`() {
        val root = node("root", listOf(node("child", listOf(node("leaf")))))
        assertNull(listOf(root).pathToNode { it.label == "ghost" })
    }

    @Test
    fun `pathToNode stops at the ancestor when both ancestor and descendant match`() {
        // Pins pre-order: recursing before testing would return the longer path [root, leaf].
        val leaf = node("match")
        val root = node("match", listOf(leaf))

        assertEquals(listOf(root), listOf(root).pathToNode { it.label == "match" })
    }

    @Test
    fun `pathToNode returns the first match when two sibling roots match`() {
        // Pins first-match across siblings: reversing the loop's iteration order returns `second`
        // and is caught by nothing else here. assertSame for the same reason as the findRecursive
        // twin — structurally identical SimpleTreeFieldItems are equal, so only identity shows
        // which root the loop stopped at.
        val first = node("match")
        val second = node("match")

        assertSame(first, listOf(first, second).pathToNode { it.label == "match" }?.single())
    }

    // ── findRecursive ─────────────────────────────────────────────────────────

    @Test
    fun `findRecursive returns null for an empty list`() {
        assertNull(emptyList<TreeFieldItem<String>>().findRecursive { true })
    }

    @Test
    fun `findRecursive returns the root when root matches`() {
        val root = node("root")
        assertEquals(root, listOf(root).findRecursive { it.label == "root" })
    }

    @Test
    fun `findRecursive returns null when no node matches in a flat list`() {
        val items = listOf(node("a"), node("b"))
        assertNull(items.findRecursive { it.label == "missing" })
    }

    @Test
    fun `findRecursive returns a nested child when it matches`() {
        val child = node("child")
        val root = node("root", listOf(child))
        assertEquals(child, listOf(root).findRecursive { it.label == "child" })
    }

    @Test
    fun `findRecursive reaches a deeply nested leaf node`() {
        val leaf = node("leaf")
        val mid = node("mid", listOf(leaf))
        val root = node("root", listOf(mid))
        assertEquals(leaf, listOf(root).findRecursive { it.label == "leaf" })
    }

    @Test
    fun `findRecursive finds match in second root when first root does not contain it`() {
        val target = node("target")
        val rootA = node("rootA", listOf(node("childA")))
        val rootB = node("rootB", listOf(target))
        assertEquals(target, listOf(rootA, rootB).findRecursive { it.label == "target" })
    }

    @Test
    fun `findRecursive returns null when no node at any depth matches`() {
        val root = node("root", listOf(node("child", listOf(node("leaf")))))
        assertNull(listOf(root).findRecursive { it.label == "ghost" })
    }

    @Test
    fun `findRecursive returns the first match when two sibling roots match`() {
        // assertSame, not assertEquals: SimpleTreeFieldItem is a data class, so two structurally
        // identical nodes are equal — only identity shows which one the fold actually returned.
        val first = node("match")
        val second = node("match")

        assertSame(first, listOf(first, second).findRecursive { it.label == "match" })
    }

    @Test
    fun `findRecursive prefers an ancestor over its matching descendant`() {
        // Pins pre-order for findRecursive, mirroring the pathToNode case.
        val leaf = node("match")
        val root = node("match", listOf(leaf))

        assertSame(root, listOf(root).findRecursive { it.label == "match" })
    }
}
