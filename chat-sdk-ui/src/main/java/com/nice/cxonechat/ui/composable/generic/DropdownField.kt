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
import com.nice.cxonechat.ui.R.string

internal interface DropdownItem<KeyType> {
    val label: String
    val value: KeyType
}

internal data class SimpleDropdownItem<KeyType>(
    override val label: String,
    override val value: KeyType,
) : DropdownItem<KeyType> {
    constructor(entry: Map.Entry<String, KeyType>) : this(entry.key, entry.value)
    constructor(value: KeyType) : this(value.toString(), value)
}

@Suppress("LongParameterList")
@Composable
internal fun <KeyType> DropdownField(
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
                        text = { Text(placeholder) }
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
                            when(value) {
                                item.value -> SelectedIcon()
                                "" -> Unit
                                else -> Spacer(Modifier.width(16.dp))
                            }
                        },
                        text = {
                            Text(item.label)
                        }
                    )
                }
            }
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
        SimpleDropdownItem("One", "1"),
        SimpleDropdownItem("Two", "2"),
        SimpleDropdownItem("Three", "3")
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
