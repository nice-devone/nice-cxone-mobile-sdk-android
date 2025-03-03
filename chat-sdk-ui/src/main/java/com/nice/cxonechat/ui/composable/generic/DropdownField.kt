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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.Typography

internal interface DropdownItem<KeyType> {
    val label: String
    val value: KeyType
}

internal data class SimpleDropdownItem<KeyType>(
    override val label: String,
    override val value: KeyType,
) : DropdownItem<KeyType>

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongParameterList")
@Composable
internal fun <KeyType> DropdownField(
    modifier: Modifier = Modifier,
    label: String = "",
    labelBackground: Color = Color.Transparent,
    errorState: State<String?> = mutableStateOf(null),
    placeholder: String = "",
    showPlaceholderOption: Boolean = false,
    value: KeyType?,
    options: Sequence<DropdownItem<KeyType>>,
    isError: Boolean = false,
    onDismiss: () -> Unit = {},
    onSelect: (KeyType) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val textFieldState = TextFieldState(options.firstOrNull { it.value == value }?.label ?: placeholder)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            readOnly = true,
            isError = isError,
            state = textFieldState,
            label = {
                if (label.isNotEmpty()) Label(label, labelBackground)
            },
            labelPosition = TextFieldLabelPosition.Attached(alwaysMinimize = true),
            placeholder = { Text(placeholder) },
            supportingText = { errorState.value?.let { Text(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                onDismiss()
            },
            scrollState = scrollState,
            matchAnchorWidth = false,
            modifier = Modifier
                .defaultMinSize(minWidth = LocalConfiguration.current.screenWidthDp.dp * 0.5f)
        ) {
            if (placeholder.isNotBlank() && showPlaceholderOption) {
                DropdownMenuItem(
                    enabled = false,
                    onClick = { expanded = false },
                    text = { Text(text = placeholder, style = Typography.bodyLarge) }
                )
                HorizontalDivider()
            }
            options.forEach { item ->
                DropdownMenuItem(
                    onClick = {
                        expanded = false
                        onSelect(item.value)
                    },
                    text = {
                        Text(item.label, style = Typography.bodyLarge)
                    }
                )
            }
        }
    }
}

@Composable
private fun Label(label: String, labelBackground: Color) {
    Text(
        text = label,
        modifier = Modifier
            .background(labelBackground)
            .padding(horizontal = space.small)
    )
}

@Suppress("UnusedPrivateMember")
@Preview
@Composable
private fun DropdownPreview() {
    val data = sequenceOf(
        SimpleDropdownItem("One", "1"),
        SimpleDropdownItem("Two", "2"),
        SimpleDropdownItem("Three", "3"),
    )
    var value: String? by remember { mutableStateOf(null) }
    fun onSelect(selected: String) {
        value = if (selected == value) {
            null
        } else {
            selected
        }
    }

    val errorState = remember { mutableStateOf<String?>("Error") }
    MaterialTheme {
        Column(
            verticalArrangement = Arrangement.spacedBy(space.large),
        ) {
            DropdownField(value = value, options = data, onSelect = ::onSelect)
            DropdownField(
                label = "Label",
                value = value,
                options = data,
                onSelect = ::onSelect,
                isError = true,
                errorState = errorState
            )
            DropdownField(placeholder = "Placeholder", value = value, options = data, onSelect = ::onSelect)
            DropdownField(
                label = "Label",
                labelBackground = colorScheme.primaryContainer,
                placeholder = "Placeholder",
                value = value,
                options = data,
                onSelect = ::onSelect
            )
        }
    }
}
