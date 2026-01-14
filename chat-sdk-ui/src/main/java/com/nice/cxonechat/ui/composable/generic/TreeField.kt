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

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType.Companion.PrimaryNotEditable
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.colorScheme
import com.nice.cxonechat.ui.composable.theme.LocalChatTypography
import com.nice.cxonechat.ui.composable.theme.LocalSpace
import com.nice.cxonechat.ui.domain.model.TreeFieldItem
import com.nice.cxonechat.ui.util.findRecursive
import com.nice.cxonechat.ui.util.pathToNode
import com.nice.cxonechat.ui.util.toggle

/** Semantics key used in tests to record the ARGB color of the label. Tests can read this
 *  ARGB integer value from the semantics tree to assert that the label is tinted with the
 *  theme error color when validation fails.
 */
@VisibleForTesting
internal val TreeFieldLabelColorKey = SemanticsPropertyKey<Int>("TreeFieldLabel")

/**
 * Renders a selectable tree of `TreeFieldItem`s with an optional label and validation error.
 *
 * The composable displays an optional label above the nodes and the list of items. Each
 * entry is rendered by the internal `TreeNode` composable. When `error` is non-null the
 * label is tinted with the theme error color and the error text is shown below all rendered
 * nodes.
 *
 * This is a controlled composable: callers must provide the selection and expansion
 * predicates and update their state from the provided callbacks.
 */
@Suppress("LongParameterList")
@Composable
internal fun <ValueType> TreeField(
    modifier: Modifier = Modifier,
    label: String = "",
    items: List<TreeFieldItem<ValueType>>,
    isExpanded: (TreeFieldItem<ValueType>) -> Boolean,
    isSelected: (TreeFieldItem<ValueType>) -> Boolean,
    onNodeClicked: (TreeFieldItem<ValueType>) -> Unit,
    onExpandClicked: (TreeFieldItem<ValueType>) -> Unit,
    error: String? = null,
) {
    Box(
        contentAlignment = Alignment.TopStart,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = LocalSpace.current.clickableSize)
    ) {
        // Extracted content into a separate composable to lower cognitive complexity of TreeField.
        TreeFieldContent(
            label = label,
            items = items,
            isExpanded = isExpanded,
            isSelected = isSelected,
            onNodeClicked = onNodeClicked,
            onExpandClicked = onExpandClicked,
            error = error,
        )
    }
}

/**
 * Renders the content of the TreeField, extracted to reduce cognitive complexity of the
 * TreeField composable.
 */
@Composable
private fun <ValueType> TreeFieldContent(
    label: String,
    items: List<TreeFieldItem<ValueType>>,
    isExpanded: (TreeFieldItem<ValueType>) -> Boolean,
    isSelected: (TreeFieldItem<ValueType>) -> Boolean,
    onNodeClicked: (TreeFieldItem<ValueType>) -> Unit,
    onExpandClicked: (TreeFieldItem<ValueType>) -> Unit,
    error: String?,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (label.isNotBlank()) {
            TreeFieldLabel(label = label, error = error)
        }
        items.forEach { item ->
            TreeNode(
                node = item,
                isExpanded = isExpanded,
                isSelected = isSelected,
                onNodeClicked = onNodeClicked,
                onExpandClicked = onExpandClicked,
            )
        }

        // Render validation/error text below the tree nodes when provided.
        if (error != null) {
            Text(
                text = error,
                color = colorScheme.error,
                style = ChatTheme.typography.bodySmall,
                modifier = Modifier
                    .testTag("tree_field_error")
                    .padding(start = LocalSpace.current.treeFieldIndent, top = LocalSpace.current.small)
            )
        }
    }
}

/**
 * Renders a single tree node and its optional children. The node relies on external
 * expansion/selection predicates and will invoke callbacks to notify the host of user
 * actions (node click, expand click).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <ValueType> TreeNode(
    node: TreeFieldItem<ValueType>,
    indent: Dp = 0.dp,
    isExpanded: (TreeFieldItem<ValueType>) -> Boolean,
    isSelected: (TreeFieldItem<ValueType>) -> Boolean,
    onNodeClicked: (TreeFieldItem<ValueType>) -> Unit,
    onExpandClicked: (TreeFieldItem<ValueType>) -> Unit,
) {
    val expanded = isExpanded(node)
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expandedStateChanged ->
            if (expandedStateChanged) onExpandClicked(node)
        },
    ) {
        Column(modifier = Modifier.menuAnchor(PrimaryNotEditable)) {
            TreeNodeMenuItem(node, expanded, Modifier.padding(start = indent), isSelected = isSelected, onNodeClicked = onNodeClicked)
            val children = node.children
            HorizontalDivider()
            if (children != null) {
                SubTree(
                    expanded = expanded,
                    children = children,
                    indent = indent,
                    isExpanded = isExpanded,
                    isSelected = isSelected,
                    onNodeClicked = onNodeClicked,
                    onExpandClicked = onExpandClicked
                )
            }
        }
    }
}

/**
 * Renders a subtree (children) of a node when expanded.
 *
 * Important: children are only composed when [expanded] is true. This avoids unnecessary
 * composition and keeps rendering efficient for large trees. The indentation is applied to
 * non-leaf child nodes to visually represent hierarchy.
 */
@Composable
private fun <ValueType> SubTree(
    expanded: Boolean,
    children: Iterable<TreeFieldItem<ValueType>>,
    indent: Dp,
    isExpanded: (TreeFieldItem<ValueType>) -> Boolean,
    isSelected: (TreeFieldItem<ValueType>) -> Boolean,
    onNodeClicked: (TreeFieldItem<ValueType>) -> Unit,
    onExpandClicked: (TreeFieldItem<ValueType>) -> Unit,
) {
    val newIndent = indent + LocalSpace.current.treeFieldIndent
    if (expanded) {
        children.forEach { childNode ->
            TreeNode(
                node = childNode,
                indent = newIndent,
                isExpanded = isExpanded,
                isSelected = isSelected,
                onNodeClicked = onNodeClicked,
                onExpandClicked = onExpandClicked,
            )
        }
    }
}

/**
 * Renders the clickable menu item for a tree node including selection and trailing icon.
 *
 * The visual appearance differs for leaf and non-leaf nodes. Leaf nodes can be selected
 * and show a check icon when selected. Non-leaf nodes show an expand/collapse icon.
 */
@Composable
private fun menuItemColor(expanded: Boolean, isLeaf: Boolean): Color =
    if (expanded && !isLeaf) colorScheme.primary else LocalContentColor.current

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun <ValueType> TreeNodeMenuItem(
    node: TreeFieldItem<ValueType>,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    isSelected: (TreeFieldItem<ValueType>) -> Boolean,
    onNodeClicked: (TreeFieldItem<ValueType>) -> Unit,
) {
    val isLeaf = node.isLeaf
    val isNodeSelected = isSelected(node)
    val menuItemColor = menuItemColor(expanded, isLeaf)
    DropdownMenuItem(
        modifier = Modifier
            .background(color = nodeColor(isNodeSelected))
            .testTag("tree_node_${node.label}"),
        text = {
            Row(modifier) {
                Text(text = node.label, style = LocalChatTypography.current.treeNodeItemLabel)
            }
        },
        onClick = {
            if (isLeaf || expanded) onNodeClicked(node)
        },
        trailingIcon = {
            when {
                isLeaf -> Box(contentAlignment = Alignment.Center) {
                    if (isNodeSelected) {
                        SelectedIcon(node.label)
                    }
                }

                else -> TrailingIcon(expanded = expanded)
            }
        },
        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
        colors = MenuDefaults.itemColors(textColor = menuItemColor, trailingIconColor = menuItemColor)
    )
}

@Composable
private fun TreeFieldLabel(label: String, error: String?) {
    val labelModifier = error?.let {
        val argb = colorScheme.error.toArgb()
        Modifier
            .testTag("tree_field_label")
            .semantics { this[TreeFieldLabelColorKey] = argb }
    } ?: Modifier.testTag("tree_field_label")

    Text(
        text = label,
        style = LocalChatTypography.current.surveyLabel,
        color = if (error != null) colorScheme.error else Color.Unspecified,
        modifier = labelModifier,
    )
}

@PreviewLightDark
@Composable
private fun TreeFieldPreview() {
    val testItems = testItems()
    var selected: TreeFieldItem<String>? by remember {
        mutableStateOf(testItems.findRecursive { it.value == "level-5" })
    }
    var expanded: Set<TreeFieldItem<String>> by remember {
        mutableStateOf(testItems.pathToNode { it == selected }?.toSet().orEmpty())
    }

    ChatTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = colorScheme.background,
        ) {
            Column {
                Text("Current selected: ${selected?.label ?: ""}")
                HorizontalDivider()
                TreeField(
                    label = "Label",
                    items = testItems,
                    isExpanded = expanded::contains,
                    isSelected = { selected == it },
                    onNodeClicked = { node ->
                        if (node.isLeaf) {
                            selected = if (selected == node) null else node
                        } else {
                            expanded = expanded.toggle(node)
                        }
                    },
                    onExpandClicked = { node ->
                        expanded = expanded.toggle(node)
                    }
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun TreeFieldErrorPreview() {
    val nodes = listOf(
        TreeFieldItem(label = "One", value = "1"),
    )
    ChatTheme {
        Surface(color = colorScheme.background) {
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
}

@Stable
private fun testItems(): List<TreeFieldItem<String>> = listOf(
    TreeFieldItem(
        "1. Level",
        "0",
        listOf(
            TreeFieldItem(
                "2. Level",
                "0-0",
                listOf(
                    TreeFieldItem(
                        "3. Level",
                        "0-0-2",
                        listOf(
                            TreeFieldItem(
                                label = "4. Level",
                                value = "0-0-2-0",
                                children = listOf(
                                    TreeFieldItem(label = "5. Level", value = "level-5"),
                                    TreeFieldItem(label = "5. Level", value = "level-5-0")
                                )
                            )
                        )
                    )
                )
            ),
        ),
    ),
    TreeFieldItem(
        "1. Level",
        "level-1-0"
    )
)
