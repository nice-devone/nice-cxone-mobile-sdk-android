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

package com.nice.cxonechat.ui.composable.generic

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
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
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.LocalSpace
import com.nice.cxonechat.ui.util.toggle

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
    isExpanded: (TreeFieldItem<ValueType>) -> Boolean,
    isSelected: (TreeFieldItem<ValueType>) -> Boolean,
    onNodeClicked: (TreeFieldItem<ValueType>) -> Unit,
    onExpandClicked: (TreeFieldItem<ValueType>) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = LocalSpace.current.clickableSize)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            if(label.isNotBlank()) {
                Text(text = label, modifier = Modifier.padding(LocalSpace.current.treeFieldIndent))
                Spacer(Modifier.size(16.dp))
            }
            items.forEach { item ->
                TreeNode(
                    node = item,
                    modifier = modifier.fillMaxWidth(),
                    indent = 8.dp,
                    isExpanded = isExpanded,
                    isSelected = isSelected,
                    onNodeClicked = onNodeClicked,
                    onExpandClicked = onExpandClicked,
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
    isSelected: (TreeFieldItem<ValueType>) -> Boolean,
    onNodeClicked: (TreeFieldItem<ValueType>) -> Unit,
    onExpandClicked: (TreeFieldItem<ValueType>) -> Unit,
) {
    val expanded = isExpanded(node)

    Column {
        Row(
            modifier = modifier.clickable {
                onNodeClicked(node)
            },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when {
                node.isLeaf -> Spacer(Modifier.size(LocalSpace.current.clickableSize))
                else -> ExpandableIcon(expanded = isExpanded(node)) {
                    onExpandClicked(node)
                }
            }
            Text(
                node.label,
            )
            if (isSelected(node)) {
                SelectedIcon(node.label, modifier = Modifier.padding(start = ChatTheme.space.small))
            }
        }

        val children = node.children
        if (children != null && expanded) {
            Column(Modifier.padding(start = LocalSpace.current.treeFieldIndent)) {
                children.forEach {
                    TreeNode(
                        node = it,
                        modifier = modifier,
                        indent = indent + LocalSpace.current.treeFieldIndent,
                        isExpanded = isExpanded,
                        isSelected = isSelected,
                        onNodeClicked = onNodeClicked,
                        onExpandClicked = onExpandClicked,
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedIcon(label: String, modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Default.Check,
        contentDescription = stringResource(
            id = string.content_description_icon_is_selected,
            formatArgs = arrayOf(
                label,
            )
        ),
        modifier = modifier,
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
    var selected: TreeFieldItem<String>? by remember {
        mutableStateOf(nodes.findRecursive { it.value == "0-0-0" })
    }
    var expanded: Set<TreeFieldItem<String>> by remember { mutableStateOf(setOf()) }

    Column {
        Text(selected?.label ?: "")

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
    test: (TreeFieldItem<Type>) -> Boolean
): List<TreeFieldItem<Type>>? {
    for(child in this) {
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
    test: (TreeFieldItem<Type>) -> Boolean
): TreeFieldItem<Type>? {
    return fold(null as TreeFieldItem<Type>?) { found, node ->
        found ?: if (test(node)) node else node.children?.findRecursive(test)
    }
}
