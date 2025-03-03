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

package com.nice.cxonechat.ui.customvalues

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.state.FieldDefinition.Hierarchy
import com.nice.cxonechat.state.FieldDefinition.Selector
import com.nice.cxonechat.state.FieldDefinition.Text
import com.nice.cxonechat.state.HierarchyNode
import com.nice.cxonechat.state.SelectorNode
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.generic.DropdownField
import com.nice.cxonechat.ui.composable.generic.Requirements.allOf
import com.nice.cxonechat.ui.composable.generic.Requirements.email
import com.nice.cxonechat.ui.composable.generic.Requirements.none
import com.nice.cxonechat.ui.composable.generic.Requirements.required
import com.nice.cxonechat.ui.composable.generic.SimpleDropdownItem
import com.nice.cxonechat.ui.composable.generic.TreeField
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.colorScheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.TextField
import com.nice.cxonechat.ui.model.SimpleTreeFieldItem
import com.nice.cxonechat.ui.model.TreeFieldItem
import com.nice.cxonechat.ui.util.isEmpty
import com.nice.cxonechat.ui.util.pathToNode
import com.nice.cxonechat.ui.util.toggle

@Composable
internal fun CVFieldList(fields: CustomValueItemList, onUpdated: (CustomValueItemList) -> Unit = {}) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = fields) {
            CVField(it) { onUpdated(fields) }
        }
    }
}

@Composable
private fun CVField(field: CustomValueItem<*, *>, onUpdated: () -> Unit) {
    when (field) {
        is CustomValueItem.Text -> CVTextField(field, onUpdated)
        is CustomValueItem.Selector -> CVSelectorField(field, onUpdated)
        is CustomValueItem.Hierarchy -> CVHierarchyField(field, onUpdated)
    }
}

@Composable
private fun CVTextField(item: CustomValueItem.Text, onUpdated: () -> Unit) {
    val details = item.definition
    val text = rememberTextFieldState(item.response.value ?: "")
    val labelBackground = labelBackground(details.isRequired)

    val validator = when (details.isRequired to details.isEMail) {
        true to true -> allOf(required, email)
        true to false -> required
        false to true -> email
        else -> none
    }

    ChatTheme.TextField(
        label = item.label(),
        minimizedLabelBackground = labelBackground,
        value = text,
        modifier = Modifier.fillMaxWidth(1f),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (details.isEMail) {
                KeyboardType.Email
            } else {
                KeyboardType.Text
            },
        ),
        validate = validator,
        onValueChange = {
            item.response.value = it.ifEmpty { null }
            onUpdated()
        }
    )
}

@Composable
private fun CVSelectorField(item: CustomValueItem.Selector, onUpdated: () -> Unit) {
    val details: Selector = remember(item::definition)
    val requiredError = stringResource(string.error_required_field)
    val valueError = stringResource(string.error_value_validation)
    val error = remember { mutableStateOf<String?>(null) }
    var node by remember(item::response)
    val label: String = remember(item::label)

    fun validate(value: SelectorNode?) = when {
        value == null && details.isRequired -> error.value = requiredError
        value != null && !details.values.contains(value) -> error.value = valueError
        else -> error.value = null
    }

    DropdownField(
        label = label.orEmpty(),
        errorState = error,
        value = node,
        labelBackground = labelBackground(details.isRequired),
        options = details.values.map { SimpleDropdownItem(it.label, it) },
        onSelect = { selected ->
            node = if (node == selected) {
                null
            } else {
                selected
            }
            validate(selected)
            onUpdated()
        },
    )
}

private fun <ValueType> Sequence<HierarchyNode<ValueType>>.toTreeFieldItemList(): List<TreeFieldItem<HierarchyNode<ValueType>>> {
    return map {
        SimpleTreeFieldItem(
            it.label,
            it,
            it.children.toTreeFieldItemList().ifEmpty { null },
        )
    }.toList()
}

private fun CustomValueItem<*, *>.label(): String = StringBuilder(definition.label)
    .apply {
        if (definition.isRequired) append(" *")
    }
    .toString()

@Composable
private fun labelBackground(isRequired: Boolean) = if (isRequired) {
    colorScheme.surfaceContainerHigh
} else {
    colorScheme.surfaceVariant
}

private typealias CVHFItem = TreeFieldItem<HierarchyNode<String>>

@Composable
private fun CVHierarchyField(item: CustomValueItem.Hierarchy, onUpdated: () -> Unit) {
    val details = item.definition
    val requiredError = stringResource(id = string.error_required_field)
    val valueError = stringResource(id = string.error_value_validation)
    val nodes by remember { mutableStateOf(details.values.toTreeFieldItemList()) }
    var selected by remember { item.response }
    var expanded: Set<CVHFItem> by remember { mutableStateOf(setOf()) }
    var error: String? by remember { mutableStateOf(null) }
    val label = item.label()

    fun validate(value: HierarchyNode<String>?) {
        error = when {
            value == null && details.isRequired -> requiredError
            value?.isLeaf == false -> valueError
            else -> null
        }
    }

    fun expandClicked(node: CVHFItem) {
        expanded = expanded.toggle(node)
    }

    fun selectClicked(node: CVHFItem) {
        if (node.isLeaf) {
            selected = if (selected == node.value) null else node.value
            validate(selected)
            onUpdated()
        } else {
            expandClicked(node)
        }
    }

    LaunchedEffect(Unit) {
        expanded = nodes.pathToNode { it.value == selected }?.toSet() ?: setOf()
    }

    TreeField(
        label = label.orEmpty(),
        items = nodes,
        isSelected = { it.value == selected },
        isExpanded = expanded::contains,
        onNodeClicked = ::selectClicked,
        onExpandClicked = ::expandClicked
    )
}

/*
 * Preview support
 */

private data class HierarchyNodeImpl(
    override val label: String,
    override val nodeId: String,
    override val children: Sequence<HierarchyNode<String>> = sequenceOf(),
) : HierarchyNode<String> {
    override val isLeaf = children.isEmpty()
}

private class FieldModelListProvider : PreviewParameterProvider<CustomValueItemList> {
    override val values = sequenceOf(
        listOf(
            CustomValueItem.Text(
                object : Text {
                    override val fieldId = "name"
                    override val label = "Name"
                    override val isEMail = false
                    override val isRequired = true

                    override fun validate(value: String) = Unit
                },
                "Some Name"
            ),
            CustomValueItem.Text(
                object : Text {
                    override val fieldId = "email"
                    override val label = "EMail"
                    override val isEMail = true
                    override val isRequired = false

                    override fun validate(value: String) = Unit
                },
                "some.one@some.where"
            ),
            CustomValueItem.Selector(
                object : Selector {
                    override val fieldId = "selector"
                    override val label = "Selector"
                    override val isRequired = true
                    override val values = listOf("zero", "one", "two").mapIndexed { index, label ->
                        object : SelectorNode {
                            override val nodeId = "$index"
                            override val label = label
                        }
                    }.asSequence()

                    override fun validate(value: String) = Unit
                },
                "zero"
            ),
            CustomValueItem.Hierarchy(
                object : Hierarchy {
                    override val fieldId = "hierarchy"
                    override val label = "Hierarchy"
                    override val isRequired = true
                    override val values = listOf<HierarchyNode<String>>(
                        HierarchyNodeImpl(
                            "Node 0",
                            "0",
                            listOf(
                                HierarchyNodeImpl(
                                    "Node 0-0",
                                    "0-0",
                                    listOf(HierarchyNodeImpl("Node 0-0-0", "0-0-0"))
                                        .asSequence()
                                )
                            ).asSequence()
                        ),
                        HierarchyNodeImpl("Node 1", "1")
                    ).asSequence()

                    override fun validate(value: String) = Unit
                },
                "0-0-0"
            )
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun CVFieldListPreview(
    @PreviewParameter(FieldModelListProvider::class) fields: CustomValueItemList,
) {
    ChatTheme {
        Surface(modifier = Modifier.padding(space.large)) {
            CVFieldList(fields = fields)
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CVFieldListPreviewDark(
    @PreviewParameter(FieldModelListProvider::class) fields: CustomValueItemList,
) {
    ChatTheme {
        Surface(modifier = Modifier.padding(space.large)) {
            CVFieldList(fields = fields)
        }
    }
}
