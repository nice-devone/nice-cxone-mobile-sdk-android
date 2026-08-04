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

import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nice.cxonechat.ui.AbstractComponentActivityUiTest
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.colorScheme
import com.nice.cxonechat.ui.domain.model.TreeFieldItem
import com.nice.cxonechat.ui.util.toggle
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@Suppress(
    "LargeClass" // Test
)
class TreeFieldAccessibilityTest : AbstractComponentActivityUiTest() {

    private fun createTreeItems(): List<TreeFieldItem<String>> = listOf(
        TreeFieldItem(
            "Mobile Devices",
            "0",
            listOf(
                TreeFieldItem(
                    "Android",
                    "0-0",
                    listOf(
                        TreeFieldItem("Samsung", "0-0-0"),
                        TreeFieldItem("Google Pixel", "0-0-1")
                    )
                ),
                TreeFieldItem(
                    "iOS",
                    "0-1",
                    listOf(
                        TreeFieldItem("iPhone", "0-1-0"),
                        TreeFieldItem("iPad", "0-1-1")
                    )
                )
            )
        ),
        TreeFieldItem("Tablet", "1"),
        TreeFieldItem("Laptop", "2")
    )

    @Test
    fun treeField_labelIsAccessible() {
        val testItems = createTreeItems()

        composeTestRule.setContent {
            val selected = remember { mutableStateOf<TreeFieldItem<String>?>(null) }
            val expanded = remember { mutableStateOf<Set<TreeFieldItem<String>>>(setOf()) }

            ChatTheme {
                Surface(color = colorScheme.background) {
                    TreeField(
                        label = "Device Type",
                        items = testItems,
                        isExpanded = expanded.value::contains,
                        isSelected = { selected.value == it },
                        onNodeClicked = { node ->
                            if (node.isLeaf) {
                                selected.value = if (selected.value == node) null else node
                            }
                        },
                        onExpandClicked = { node ->
                            expanded.value = expanded.value.toggle(node)
                        }
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("tree_field_label")
            .assertExists()
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()
    }

    @Test
    fun treeField_leafNode_isAccessibleAndAnnouncesSelection() {
        val testItems = createTreeItems()

        composeTestRule.setContent {
            val selected = remember { mutableStateOf<TreeFieldItem<String>?>(null) }
            val expanded = remember { mutableStateOf<Set<TreeFieldItem<String>>>(setOf()) }

            ChatTheme {
                Surface(color = colorScheme.background) {
                    TreeField(
                        label = "Device Type",
                        items = testItems,
                        isExpanded = expanded.value::contains,
                        isSelected = { selected.value == it },
                        onNodeClicked = { node ->
                            if (node.isLeaf) {
                                selected.value = if (selected.value == node) null else node
                            }
                        },
                        onExpandClicked = { node ->
                            expanded.value = expanded.value.toggle(node)
                        }
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("tree_node_Tablet")
            .assertExists()
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()

        // Verify content description contains the leaf node label
        val contentDescription = composeTestRule.onNodeWithTag("tree_node_Tablet")
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.ContentDescription)

        assertTrue(
            "Content description should contain 'Tablet'",
            contentDescription?.any {
                it.contains("Tablet")
            } == true
        )
    }

    @Test
    fun treeField_parentNode_announcesExpandedState() {
        val testItems = createTreeItems()

        composeTestRule.setContent {
            val selected = remember { mutableStateOf<TreeFieldItem<String>?>(null) }
            val expanded = remember { mutableStateOf<Set<TreeFieldItem<String>>>(setOf()) }

            ChatTheme {
                Surface(color = colorScheme.background) {
                    TreeField(
                        label = "Device Type",
                        items = testItems,
                        isExpanded = expanded.value::contains,
                        isSelected = { selected.value == it },
                        onNodeClicked = { node ->
                            if (node.isLeaf) {
                                selected.value = if (selected.value == node) null else node
                            }
                        },
                        onExpandClicked = { node ->
                            expanded.value = expanded.value.toggle(node)
                        }
                    )
                }
            }
        }

        val parentNode = testItems[0]
        composeTestRule.onNodeWithTag("tree_node_${parentNode.label}")
            .assertExists()
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()

        // Verify content description contains both the label and collapsed state
        val contentDescription = composeTestRule.onNodeWithTag("tree_node_${parentNode.label}")
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.ContentDescription)

        assertTrue(
            "Content description should contain 'Mobile Devices, collapsed'",
            contentDescription?.any {
                it.contains("Mobile Devices") && it.contains("collapsed")
            } == true
        )
    }

    @Test
    fun treeField_errorMessage_isAccessible() {
        val testItems = createTreeItems()

        composeTestRule.setContent {
            ChatTheme {
                Surface(color = colorScheme.background) {
                    TreeField(
                        label = "Device Type",
                        items = testItems,
                        isExpanded = { false },
                        isSelected = { false },
                        onNodeClicked = {},
                        onExpandClicked = {},
                        error = "Please select a device type"
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("tree_field_error")
            .assertExists()
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()
    }

    @Test
    fun treeField_allNodes_passAccessibilityChecks() {
        val testItems = createTreeItems()

        composeTestRule.setContent {
            val selected = remember { mutableStateOf<TreeFieldItem<String>?>(null) }
            val expanded = remember { mutableStateOf<Set<TreeFieldItem<String>>>(setOf()) }

            ChatTheme {
                Surface(color = colorScheme.background) {
                    TreeField(
                        label = "Device Type",
                        items = testItems,
                        isExpanded = expanded.value::contains,
                        isSelected = { selected.value == it },
                        onNodeClicked = { node ->
                            if (node.isLeaf) {
                                selected.value = if (selected.value == node) null else node
                            }
                        },
                        onExpandClicked = { node ->
                            expanded.value = expanded.value.toggle(node)
                        }
                    )
                }
            }
        }

        testItems.forEach { item ->
            composeTestRule.onNodeWithTag("tree_node_${item.label}")
                .assertExists()
                .tryPerformAccessibilityChecks()
        }
    }

    @Test
    fun treeField_withoutLabel_isAccessible() {
        val testItems = listOf(
            TreeFieldItem("Option 1", "1"),
            TreeFieldItem("Option 2", "2")
        )

        composeTestRule.setContent {
            val selected = remember { mutableStateOf<TreeFieldItem<String>?>(null) }

            ChatTheme {
                Surface(color = colorScheme.background) {
                    TreeField(
                        label = "",
                        items = testItems,
                        isExpanded = { false },
                        isSelected = { selected.value == it },
                        onNodeClicked = { node ->
                            selected.value = if (selected.value == node) null else node
                        },
                        onExpandClicked = {}
                    )
                }
            }
        }

        composeTestRule.onNodeWithTag("tree_node_Option 1")
            .assertExists()
            .assertIsDisplayed()
            .tryPerformAccessibilityChecks()
    }

    @Test
    fun treeField_selectingLeafNode_announcesSelection() {
        val testItems = createTreeItems()

        composeTestRule.setContent {
            val selected = remember { mutableStateOf<TreeFieldItem<String>?>(null) }
            val expanded = remember { mutableStateOf<Set<TreeFieldItem<String>>>(setOf()) }

            ChatTheme {
                Surface(color = colorScheme.background) {
                    TreeField(
                        label = "Device Type",
                        items = testItems,
                        isExpanded = expanded.value::contains,
                        isSelected = { selected.value == it },
                        onNodeClicked = { node ->
                            if (node.isLeaf) {
                                selected.value = if (selected.value == node) null else node
                            } else {
                                expanded.value = expanded.value.toggle(node)
                            }
                        },
                        onExpandClicked = { node ->
                            expanded.value = expanded.value.toggle(node)
                        }
                    )
                }
            }
        }

        val leafNode = testItems[1]

        // Before selection, verify content description contains node label
        var contentDescription = composeTestRule.onNodeWithTag("tree_node_${leafNode.label}")
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.ContentDescription)
        assertTrue(
            "Content description should contain 'Tablet'",
            contentDescription?.any {
                it.contains("Tablet")
            } == true
        )

        composeTestRule.onNodeWithTag("tree_node_${leafNode.label}")
            .performClick()

        composeTestRule.waitForIdle()

        // After selection, verify content description includes "selected" state
        contentDescription = composeTestRule.onNodeWithTag("tree_node_${leafNode.label}")
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.ContentDescription)
        assertTrue(
            "Content description should contain 'Tablet' and 'selected'",
            contentDescription?.any {
                it.contains("Tablet") && it.contains("selected")
            } == true
        )
    }

    @Test
    fun treeField_expandingNode_updatesAccessibilityAnnouncement() {
        val testItems = createTreeItems()

        composeTestRule.setContent {
            val selected = remember { mutableStateOf<TreeFieldItem<String>?>(null) }
            val expanded = remember { mutableStateOf<Set<TreeFieldItem<String>>>(setOf()) }

            ChatTheme {
                Surface(color = colorScheme.background) {
                    TreeField(
                        label = "Device Type",
                        items = testItems,
                        isExpanded = expanded.value::contains,
                        isSelected = { selected.value == it },
                        onNodeClicked = { node ->
                            if (node.isLeaf) {
                                selected.value = if (selected.value == node) null else node
                            } else {
                                expanded.value = expanded.value.toggle(node)
                            }
                        },
                        onExpandClicked = { node ->
                            expanded.value = expanded.value.toggle(node)
                        }
                    )
                }
            }
        }

        val parentNode = testItems[0]

        // Before expansion, verify content description says "collapsed"
        var contentDescription = composeTestRule.onNodeWithTag("tree_node_${parentNode.label}")
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.ContentDescription)
        assertTrue(
            "Content description should contain 'collapsed'",
            contentDescription?.any {
                it.contains("collapsed")
            } == true
        )

        composeTestRule.onNodeWithTag("tree_node_${parentNode.label}")
            .performClick()

        composeTestRule.waitForIdle()

        // After expansion, verify content description says "expanded"
        contentDescription = composeTestRule.onNodeWithTag("tree_node_${parentNode.label}")
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.ContentDescription)
        assertTrue(
            "Content description should contain 'expanded'",
            contentDescription?.any {
                it.contains("expanded")
            } == true
        )
    }
}
