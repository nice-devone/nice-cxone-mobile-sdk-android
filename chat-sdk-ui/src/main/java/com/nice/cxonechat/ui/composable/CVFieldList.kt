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

package com.nice.cxonechat.ui.composable

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.state.FieldDefinition.Selector
import com.nice.cxonechat.state.HierarchyNode
import com.nice.cxonechat.state.SelectorNode
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.generic.DropdownField
import com.nice.cxonechat.ui.composable.generic.DropdownItem
import com.nice.cxonechat.ui.composable.generic.Requirements.allOf
import com.nice.cxonechat.ui.composable.generic.Requirements.email
import com.nice.cxonechat.ui.composable.generic.Requirements.none
import com.nice.cxonechat.ui.composable.generic.Requirements.required
import com.nice.cxonechat.ui.composable.generic.TreeField
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.TextField
import com.nice.cxonechat.ui.domain.model.CustomValueItem
import com.nice.cxonechat.ui.domain.model.CustomValueItemList
import com.nice.cxonechat.ui.domain.model.TreeFieldItem
import com.nice.cxonechat.ui.util.pathToNode
import com.nice.cxonechat.ui.util.preview.FieldModelListProvider
import com.nice.cxonechat.ui.util.toggle

@Composable
internal fun CVFieldList(
    fields: CustomValueItemList,
    modifier: Modifier = Modifier,
    onUpdated: (CustomValueItemList) -> Unit = {},
) {
    LazyColumn(
        modifier = modifier,
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

    val validator = when (details.isRequired to details.isEMail) {
        true to true -> allOf(required, email)
        true to false -> required
        false to true -> email
        else -> none
    }

    ChatTheme.TextField(
        label = item.label(),
        minimizedLabelBackground = Color.Unspecified,
        value = text,
        modifier = Modifier
            .fillMaxWidth(1f)
            .testTag("custom_value_text_${item.definition.label}"),
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
        modifier = Modifier.testTag("custom_value_list_${item.definition.label}"),
        label = label,
        errorState = error,
        value = node,
        options = details.values.map { DropdownItem(it.label, it) },
        onSelect = { selected ->
            node = if (node == selected) {
                null
            } else {
                selected
            }
            item.response.value = selected
            validate(selected)
            onUpdated()
        },
    )
}

private fun <ValueType> Sequence<HierarchyNode<ValueType>>.toTreeFieldItemList(): List<TreeFieldItem<HierarchyNode<ValueType>>> {
    return map {
        TreeFieldItem(
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
            item.response.value = selected
            onUpdated()
        } else {
            expandClicked(node)
        }
    }

    LaunchedEffect(Unit) {
        expanded = nodes.pathToNode { it.value == selected }?.toSet() ?: setOf()
    }

    TreeField(
        modifier = Modifier.testTag("custom_value_hierarchy_${item.definition.label}"),
        label = label,
        items = nodes,
        isSelected = { it.value == selected },
        isExpanded = expanded::contains,
        onNodeClicked = ::selectClicked,
        onExpandClicked = ::expandClicked,
        error = error,
    )
}

@PreviewLightDark
@Composable
private fun CVFieldListPreview(
    @PreviewParameter(FieldModelListProvider::class) fields: CustomValueItemList,
) {
    ChatTheme {
        Surface(
            color = chatColors.token.background.surface.subtle,
        ) {
            Box(Modifier.padding(space.large)) {
                CVFieldList(fields = fields)
            }
        }
    }
}
