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

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType.Companion.PrimaryNotEditable
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.generic.TreeFieldDefaults.LeafNodeElevation
import com.nice.cxonechat.ui.composable.generic.TreeFieldDefaults.ParentNodeElevation
import com.nice.cxonechat.ui.composable.generic.TreeFieldDefaults.SelectedLeafNodeElevation
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.colorScheme
import com.nice.cxonechat.ui.composable.theme.LocalChatTypography
import com.nice.cxonechat.ui.composable.theme.LocalSpace
import com.nice.cxonechat.ui.model.SimpleTreeFieldItem
import com.nice.cxonechat.ui.model.TreeFieldItem
import com.nice.cxonechat.ui.util.findRecursive
import com.nice.cxonechat.ui.util.toggle

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
) {
    Box(
        contentAlignment = Alignment.TopStart,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = LocalSpace.current.clickableSize)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if (label.isNotBlank()) {
                Text(
                    text = label,
                    style = LocalChatTypography.current.surveyLabel,
                )
            }
            items.forEach { item ->
                TreeNode(
                    node = item,
                    modifier = modifier.fillMaxWidth(),
                    indent = LocalSpace.current.treeFieldIndent,
                    isExpanded = isExpanded,
                    isSelected = isSelected,
                    onNodeClicked = onNodeClicked,
                    onExpandClicked = onExpandClicked,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("CognitiveComplexMethod", "LongParameterList")
@Composable
private fun <ValueType> TreeNode(
    node: TreeFieldItem<ValueType>,
    modifier: Modifier,
    indent: Dp,
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
        modifier = modifier,
    ) {
        Column(modifier = Modifier.menuAnchor(PrimaryNotEditable)) {
            TreeNodeMenuItem(node, expanded, isSelected, onNodeClicked)
            val children = node.children
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

@Composable
private fun <ValueType> ColumnScope.SubTree(
    expanded: Boolean,
    children: Iterable<TreeFieldItem<ValueType>>,
    indent: Dp,
    isExpanded: (TreeFieldItem<ValueType>) -> Boolean,
    isSelected: (TreeFieldItem<ValueType>) -> Boolean,
    onNodeClicked: (TreeFieldItem<ValueType>) -> Unit,
    onExpandClicked: (TreeFieldItem<ValueType>) -> Unit,
) {
    HorizontalDivider()
    if (expanded) {
        children.forEach { childNode ->
            TreeNode(
                node = childNode,
                modifier = Modifier
                    .padding(start = if (childNode.isLeaf) 0.dp else indent),
                indent = indent,
                isExpanded = isExpanded,
                isSelected = isSelected,
                onNodeClicked = onNodeClicked,
                onExpandClicked = onExpandClicked,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun <ValueType> TreeNodeMenuItem(
    node: TreeFieldItem<ValueType>,
    expanded: Boolean,
    isSelected: (TreeFieldItem<ValueType>) -> Boolean,
    onNodeClicked: (TreeFieldItem<ValueType>) -> Unit,
) {
    val isLeaf = node.isLeaf
    val isNodeSelected = isSelected(node)
    Surface(
        color = nodeColor(isLeaf, isNodeSelected),
        tonalElevation = nodeElevation(isLeaf, isNodeSelected),
    ) {
        DropdownMenuItem(
            text = { Text(node.label) },
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

                    else -> ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            },
            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
        )
    }
}

@Composable
private fun nodeElevation(isLeaf: Boolean, isSelected: Boolean) = if (isLeaf) {
    if (isSelected) SelectedLeafNodeElevation else LeafNodeElevation
} else {
    ParentNodeElevation
}

@Composable
private fun nodeColor(isLeaf: Boolean, isSelected: Boolean) = if (isLeaf) {
    if (isSelected) colorScheme.surfaceContainerHighest else colorScheme.surfaceContainer
} else {
    colorScheme.surface
}

@Composable
private fun SelectedIcon(label: String) {
    Icon(
        imageVector = Icons.Default.Check,
        contentDescription = stringResource(
            id = string.content_description_icon_is_selected,
            formatArgs = arrayOf(
                label,
            )
        ),
        tint = colorScheme.primary,
    )
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO or Configuration.UI_MODE_TYPE_UNDEFINED)
@Composable
private fun TreeFieldPreviewLight() {
    TreeFieldPreview()
}

@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_UNDEFINED)
@Composable
private fun TreeFieldPreviewDark() {
    TreeFieldPreview()
}

@Suppress("CognitiveComplexMethod", "LongParameterList", "UnusedPrivateMember")
@Composable
private fun TreeFieldPreview() {
    val nodes = listOf(
        SimpleTreeFieldItem(
            "Node 0",
            "0",
            listOf(
                SimpleTreeFieldItem(
                    "Node 0-0",
                    "0-0",
                    listOf(
                        SimpleTreeFieldItem("Node 0-0-0", "0-0-0"),
                        SimpleTreeFieldItem("Node 0-0-1", "0-0-1"),
                        SimpleTreeFieldItem(
                            "Node 0-0-2",
                            "0-0-2",
                            listOf(
                                SimpleTreeFieldItem("Node 0-0-2-0", "0-0-2-0"),
                                SimpleTreeFieldItem("Node 0-0-2-1", "0-0-2-1"),
                            )
                        )
                    )
                ),
                SimpleTreeFieldItem("Node 0-1", "0-1"),
                SimpleTreeFieldItem("Node 0-2", "0-2"),
            ),
        ),
        SimpleTreeFieldItem("Node 1", "1")
    )
    var selected: TreeFieldItem<String>? by remember {
        mutableStateOf(nodes.findRecursive { it.value == "1" })
    }
    var expanded: Set<TreeFieldItem<String>> by remember { mutableStateOf(setOf()) }

    ChatTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column {
                Text("Current selected: ${selected?.label ?: ""}")
                HorizontalDivider()
                TreeField(
                    label = "Label",
                    items = nodes,
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

internal object TreeFieldDefaults {
    val ParentNodeElevation = 0.dp
    val LeafNodeElevation = 1.dp
    val SelectedLeafNodeElevation = 12.dp
}
