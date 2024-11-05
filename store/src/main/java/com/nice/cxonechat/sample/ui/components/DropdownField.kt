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

package com.nice.cxonechat.sample.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.sample.R.string

/**
 * Simplified dropdown field.
    val label: String
 *
}

data class SimpleDropdownItem<KeyType>(
 * @param KeyType type of data selected from list.
 * @param modifier Compose Modifier to apply.
 * @param label Label for field.
 * @param placeholder Text to be displayed if no value is chosen.
 * @param value Current value of the field.
constructor(entry: Map.Entry<String, KeyType>) : this(entry.key, entry.value)
 * @param options List of options the field can take.
 * @param isError is the field in error.
 * @param onDismiss the dropdown menu was dismissed.
 * @param onSelect a selection was made from the dropdown menu.
 */
@Composable
fun <KeyType> DropdownField(
    modifier: Modifier = Modifier,
    label: String = "",
    placeholder: String = "",
    value: KeyType?,
    options: Sequence<DropdownItem<KeyType>>,
    isError: Boolean = false,
    onDismiss: () -> Unit = {},
    onSelect: (KeyType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable { expanded = true }
            .fillMaxWidth()
    ) {
        if (label.isNotBlank()) {
            ErrorLabel(label, isError)
            Spacer(Modifier.weight(1f))
        }
        Text(options.firstOrNull { it.value == value }?.label ?: placeholder)
        if (label.isBlank()) {
            Spacer(Modifier.weight(1f))
        }
        Box {
            ExpandIcon()
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                    onDismiss()
                },
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                if (placeholder.isNotBlank()) {
                    DropdownMenuItem(
                        onClick = { },
                        text = { Text(placeholder) },
                    )
                    HorizontalDivider()
                }
                options.forEach { item ->
                    DropdownMenuItem(
                        onClick = {
                            expanded = false
                            onSelect(item.value)
                        },
                        leadingIcon = {
                            when (value) {
                                item.value -> SelectedIcon()
                                "" -> Unit
                                else -> Spacer(Modifier.width(16.dp))
                            }
                        },
                        text = { Text(item.label) }
                    )
                }
            }
        }
    }
}

/**
 * Data type for drop down menu items.
 */
interface DropdownItem<KeyType> {
    /** Text label to be displayed for item. */
    val label: String

    /** Value associated with item when selected. */
    val value: KeyType

    companion object {
        /**
         * Construct a DropdownItem from a label and key/value.
         *
         * @param KeyType type of value.
         * @param label label to be displayed.
         * @param value associated value.
         * @return a simple DropdownItem with [label] and [value].
         */
        operator fun <KeyType> invoke(label: String, value: KeyType) = object : DropdownItem<KeyType> {
            override val label = label
            override val value = value
        }

        /**
         * Construct a DropdownItem from a Map.Entry where the key is the label to be
         * applied and the value is the value.
         *
         * @param KeyType type of value in Map.Entry.
         * @param entry Source Map.Entry.
         * @return a DropdownItem constructed from [entry]
         */
        operator fun <KeyType> invoke(entry: Map.Entry<String, KeyType>) = object : DropdownItem<KeyType> {
            override val label = entry.key
            override val value = entry.value
        }

        /**
         * Construct a DropdownItem where the label and value are the same.
         *
         * @param label Label/Value for item.
         * @return a DropdownItem<String> with label as label and value.
         */
        operator fun invoke(label: String) = object : DropdownItem<String> {
            override val label = label
            override val value get() = this.label
        }
    }
}

@Composable
private fun ErrorLabel(
    label: String,
    isError: Boolean,
) {
    Text(
        text = label,
        color = if (isError) {
            MaterialTheme.colorScheme.error
        } else {
            LocalContentColor.current.copy(alpha = 0.38f)
        }
    )
}

@Composable
private fun ExpandIcon() {
    Icon(
        imageVector = Icons.Default.ArrowDropDown,
        contentDescription = stringResource(id = string.content_description_dropdown_field_icon_expandable)
    )
}

@Composable
private fun SelectedIcon() {
    Icon(
        modifier = Modifier.size(16.dp, 16.dp),
        imageVector = Icons.Default.Check,
        contentDescription = stringResource(
            string.content_description_dropdown_field_icon_is_selected
        )
    )
}

@Suppress("UnusedPrivateMember")
@Preview
@Composable
private fun DropdownPreview() {
    val data = sequenceOf(
        DropdownItem("One", "1"),
        DropdownItem("Two", "2"),
        DropdownItem("Three", "3")
    )
    var value by remember { mutableStateOf(null as (String?)) }
    fun onSelect(selected: String) {
        value = if (selected == value) {
            null
        } else {
            selected
        }
    }
    MaterialTheme {
        Column {
            DropdownField(value = value, options = data, onSelect = ::onSelect)
            DropdownField(label = "Label", value = value, options = data, onSelect = ::onSelect, isError = true)
            DropdownField(placeholder = "Placeholder", value = value, options = data, onSelect = ::onSelect)
            DropdownField(
                label = "Label",
                placeholder = "Placeholder",
                value = value,
                options = data,
                onSelect = ::onSelect
            )
        }
    }
}
