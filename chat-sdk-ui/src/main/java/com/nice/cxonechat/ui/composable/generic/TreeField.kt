/*
 * Copyright (c) 2021-2023. NICE Ltd. All rights reserved.
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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.nice.cxonechat.ui.composable.theme.LocalSpace

internal interface TreeFieldItem<ValueType> {
    val label: String
    val value: ValueType
    val children: Iterable<TreeFieldItem<ValueType>>?

    val isLeaf: Boolean
        get() = children == null
}

internal data class SimpleTreeFieldItem<ValueType>(
    override val label: String,
    override val value: ValueType,
    override val children: List<TreeFieldItem<ValueType>>? = null,
) : TreeFieldItem<ValueType>

@Suppress("LongParameterList")
@Composable
internal fun <ValueType> TreeField(
    modifier: Modifier = Modifier,
    label: String = "",
    items: List<TreeFieldItem<ValueType>>,
    expanded: Array<TreeFieldItem<ValueType>> = arrayOf(),
    canSelect: (TreeFieldItem<ValueType>) -> Boolean,
    isSelected: (TreeFieldItem<ValueType>) -> Boolean,
    toggleSelected: (TreeFieldItem<ValueType>) -> Unit,
) {
    val expandedItems = remember { mutableStateListOf<TreeFieldItem<ValueType>>() }
    expandedItems.addAll(expanded)

    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = LocalSpace.current.clickableSize)
    ) {
        Column {
            if(label.isNotBlank()) {
                Text(text = label, modifier = Modifier.padding(LocalSpace.current.treeFieldIndent))
                Spacer(Modifier.size(16.dp))
            }
            items.forEach { item ->
                TreeNode(
                    node = item,
                    modifier = modifier,
                    indent = 8.dp,
                    isExpanded = { node ->
                        expandedItems.contains(node)
                    },
                    toggleExpanded = { node ->
                        if(expandedItems.contains(node)) {
                            expandedItems.remove(node)
                        } else {
                            expandedItems.add(node)
                        }
                    },
                    canSelect = canSelect,
                    isSelected = isSelected,
                    toggleSelected = toggleSelected
                )
            }
        }
    }
}

@Suppress("CognitiveComplexMethod", "LongParameterList")
@Composable
private fun <ValueType> TreeNode(
    node: TreeFieldItem<ValueType>,
    modifier: Modifier,
    indent: Dp,
    isExpanded: (TreeFieldItem<ValueType>) -> Boolean,
    toggleExpanded: (TreeFieldItem<ValueType>) -> Unit,
    canSelect: (TreeFieldItem<ValueType>) -> Boolean,
    isSelected: (TreeFieldItem<ValueType>) -> Boolean,
    toggleSelected: (TreeFieldItem<ValueType>) -> Unit,
) {
    Column {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when {
                node.isLeaf -> Spacer(Modifier.size(LocalSpace.current.clickableSize))
                else -> ExpandableIcon(expanded = isExpanded(node)) {
                    toggleExpanded(node)
                }
            }
            Text(
                node.label,
                Modifier.clickable {
                    if (canSelect(node)) {
                        toggleSelected(node)
                    } else {
                        toggleExpanded(node)
                    }
                }
            )
            if (isSelected(node)) {
                SelectedIcon(node.label)
            }
        }

        val children = node.children
        if (children != null && isExpanded(node)) {
            Column(Modifier.padding(start = LocalSpace.current.treeFieldIndent)) {
                children.forEach {
                    TreeNode(
                        node = it,
                        modifier = modifier,
                        indent = indent + LocalSpace.current.treeFieldIndent,
                        isExpanded = isExpanded,
                        toggleExpanded = toggleExpanded,
                        canSelect = canSelect,
                        isSelected = isSelected,
                        toggleSelected = toggleSelected,
                    )
                }
            }
        }
    }
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
        )
    )
}

@Suppress("CognitiveComplexMethod", "LongParameterList", "UnusedPrivateMember")
@Preview
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
                    listOf(SimpleTreeFieldItem("Node 0-0-0", "0-0-0"))
                ),
                SimpleTreeFieldItem("Node 0-1", "0-1")
            ),
        ),
        SimpleTreeFieldItem("Node 1", "1")
    )
    var value by remember { mutableStateOf("0-1") }

    Column {
        Text(value) // Serves as a double-check that the correct value is set.
        TreeField(
            label = "Label",
            items = nodes,
            expanded = arrayOf(nodes.first()),
            canSelect = { it.isLeaf },
            isSelected = { it.value == value },
            toggleSelected = { value = it.value },
        )
    }
}
