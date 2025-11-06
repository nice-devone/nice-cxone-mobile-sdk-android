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

package com.nice.cxonechat.ui.composable.generic

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.domain.model.TreeFieldItem
import com.nice.cxonechat.ui.util.toggle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
@LargeTest
internal class TreeFieldTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun whenErrorProvided_labelHasErrorColor_andErrorTextIsVisible() {
        val expectedArgb = AtomicInteger()

        composeTestRule.setContent {
            ChatTheme {
                // capture the theme error color in ARGB form inside composition
                val expectedColorArgbLocal = ChatTheme.colorScheme.error.toArgb()
                SideEffect { expectedArgb.set(expectedColorArgbLocal) }

                val nodes = listOf(
                    // simple node list
                    TreeFieldItem(label = "One", value = "1")
                )

                TreeField(
                    label = "Label",
                    items = nodes,
                    isExpanded = { false },
                    isSelected = { false },
                    onNodeClicked = {},
                    onExpandClicked = {},
                    error = "This field is required"
                )
            }
        }

        // Assert error text is displayed
        composeTestRule.onNodeWithTag("tree_field_error").assertIsDisplayed()

        // Read semantics property for label color and compare with expected
        val semanticsColor = composeTestRule
            .onNodeWithTag("tree_field_label")
            .fetchSemanticsNode()
            .config
            .getOrNull(TreeFieldLabelColorKey)

        assertEquals("Label semantics color should match theme error color", expectedArgb.get(), semanticsColor)
    }

    @Test
    fun whenLabelBlank_labelIsNotRendered() {
        composeTestRule.setContent {
            ChatTheme {
                val nodes = listOf(
                    TreeFieldItem(label = "One", value = "1")
                )
                TreeField(
                    label = "",
                    items = nodes,
                    isExpanded = { false },
                    isSelected = { false },
                    onNodeClicked = {},
                    onExpandClicked = {},
                    error = null
                )
            }
        }

        // Label shouldn't exist when blank
        composeTestRule.onNodeWithTag("tree_field_label").assertDoesNotExist()
    }

    @Test
    fun whenLabelPresentWithoutError_semanticsColorIsNotSet() {
        composeTestRule.setContent {
            ChatTheme {
                val nodes = listOf(
                    TreeFieldItem(label = "One", value = "1")
                )
                TreeField(
                    label = "Label",
                    items = nodes,
                    isExpanded = { false },
                    isSelected = { false },
                    onNodeClicked = {},
                    onExpandClicked = {},
                    error = null
                )
            }
        }

        // Label should exist
        composeTestRule.onNodeWithTag("tree_field_label").assertIsDisplayed()

        val semanticsColor = composeTestRule
            .onNodeWithTag("tree_field_label")
            .fetchSemanticsNode()
            .config
            .getOrNull(TreeFieldLabelColorKey)

        // When there's no error, we don't set the semantics color
        assertNull(semanticsColor)
    }

    @Test
    fun selectionAndExpansion_isReportedCorrectly() {
        // Use mutableStateOf outside setContent so test code can mutate/read it reliably
        val expandedState = mutableStateOf(setOf<TreeFieldItem<String>>())
        val selectedState = mutableStateOf<String?>(null)

        // Create nodes outside composition so test can reference them
        val child = TreeFieldItem(label = "Child", value = "child")
        val parent = TreeFieldItem(label = "Parent", value = "parent", children = listOf(child))
        val nodes = listOf(parent)

        composeTestRule.setContent {
            ChatTheme {
                TreeField(
                    label = "Label",
                    items = nodes,
                    isExpanded = { expandedState.value.contains(it) },
                    isSelected = { selectedState.value == it.value },
                    onNodeClicked = { node ->
                        if (node.isLeaf) {
                            selectedState.value = if (selectedState.value == node.value) null else node.value
                        } else {
                            expandedState.value = expandedState.value.toggle(node)
                        }
                    },
                    onExpandClicked = { node -> expandedState.value = expandedState.value.toggle(node) },
                    error = null
                )
            }
        }

        // Child shouldn't be visible until parent is expanded
        composeTestRule.onNodeWithText("Child").assertDoesNotExist()

        // Expand programmatically (simulates the effect of user expanding the node)
        composeTestRule.runOnUiThread {
            expandedState.value = setOf(parent)
        }

        // Give composition time to update
        composeTestRule.waitForIdle()

        // Now child should be visible
        composeTestRule.onNodeWithText("Child").assertIsDisplayed()

        // Click child to select
        composeTestRule.onNodeWithText("Child").performClick()

        // Wait for selection to propagate
        composeTestRule.waitForIdle()

        // Verify selected icon content description for child exists
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val selectedContentDesc = context.getString(string.content_description_icon_is_selected, "Child")
        composeTestRule.onNodeWithContentDescription(selectedContentDesc).assertIsDisplayed()
    }

    @Test
    fun itemsDisplayed_inCorrectHierarchy_usingTags() {
        val expandedState = mutableStateOf(setOf<TreeFieldItem<String>>())
        val selectedState = mutableStateOf<String?>(null)

        // Build a three-level tree: Parent -> Child -> Grandchild
        val grandchild = TreeFieldItem(label = "Grandchild", value = "gchild")
        val child = TreeFieldItem(label = "Child", value = "child", children = listOf(grandchild))
        val parent = TreeFieldItem(label = "Parent", value = "parent", children = listOf(child))
        val nodes = listOf(parent)

        composeTestRule.setContent {
            ChatTheme {
                TreeField(
                    label = "Label",
                    items = nodes,
                    isExpanded = { expandedState.value.contains(it) },
                    isSelected = { selectedState.value == it.value },
                    onNodeClicked = { node ->
                        if (node.isLeaf) {
                            selectedState.value = if (selectedState.value == node.value) null else node.value
                        } else {
                            expandedState.value = expandedState.value.toggle(node)
                        }
                    },
                    onExpandClicked = { node -> expandedState.value = expandedState.value.toggle(node) },
                    error = null
                )
            }
        }

        // Initially only parent should be present
        composeTestRule.onNodeWithTag("tree_node_Parent").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tree_node_Child").assertDoesNotExist()
        composeTestRule.onNodeWithTag("tree_node_Grandchild").assertDoesNotExist()

        // Expand parent
        composeTestRule.runOnUiThread { expandedState.value = setOf(parent) }
        composeTestRule.waitForIdle()

        // Now child should be visible, grandchild still hidden
        composeTestRule.onNodeWithTag("tree_node_Child").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tree_node_Grandchild").assertDoesNotExist()

        // Expand child
        composeTestRule.runOnUiThread { expandedState.value = setOf(parent, child) }
        composeTestRule.waitForIdle()

        // Now grandchild should be visible
        composeTestRule.onNodeWithTag("tree_node_Grandchild").assertIsDisplayed()

        // Collapse parent -> all descendants hidden
        composeTestRule.runOnUiThread { expandedState.value = setOf() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("tree_node_Child").assertDoesNotExist()
        composeTestRule.onNodeWithTag("tree_node_Grandchild").assertDoesNotExist()
    }

}
