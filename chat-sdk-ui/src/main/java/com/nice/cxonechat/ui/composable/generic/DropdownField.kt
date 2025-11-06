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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuBoxScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.LocalChatColors
import com.nice.cxonechat.ui.composable.theme.Typography

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
    val textFieldState = rememberTextFieldState(options.firstOrNull { it.value == value }?.label ?: "")
    val stateHolder = remember(options) { DropdownFieldState(options) }
    stateHolder.ObserveFiltering(textFieldState)

    val keyHandler = dropdownFieldKeyHandler(
        stateHolder.filteredList,
        onSelect,
        { expanded = it }
    )

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.onPreviewKeyEvent(keyHandler)
    ) {
        DropdownFieldTextField(
            isError = isError,
            textFieldState = textFieldState,
            label = label,
            labelBackground = labelBackground,
            placeholder = placeholder,
            errorState = errorState,
            onClear = {
                textFieldState.clearText()
                expanded = true
                stateHolder.filteredList = stateHolder.items
                value?.let { onSelect(it) }
            },
        )
        DropdownFieldMenu(
            expanded = expanded,
            onDismiss = {
                expanded = false
                onDismiss()
            },
            placeholder = placeholder,
            showPlaceholderOption = showPlaceholderOption,
            filteredList = stateHolder.filteredList,
            onItemSelected = { item ->
                expanded = false
                textFieldState.edit { replace(0, textFieldState.text.length, item.label) }
                onSelect(item.value)
            }
        )
    }
}

private class DropdownFieldState<KeyType>(
    options: Sequence<DropdownItem<KeyType>>,
) {
    val items = options.toList()
    var filteredList by mutableStateOf(items)
    var previousValidText by mutableStateOf("")

    @Composable
    fun ObserveFiltering(textFieldState: TextFieldState) {
        LaunchedEffect(textFieldState.text) {
            val current = textFieldState.text.toString()
            if (current.isEmpty() || items.any { it.label.startsWith(current, true) }) {
                previousValidText = current
                filteredList = items.filter { it.label.startsWith(current, true) }
            } else {
                textFieldState.edit { replace(0, textFieldState.text.length, previousValidText) }
                filteredList = items.filter { it.label.startsWith(previousValidText, true) }
            }
        }
    }
}

private fun <KeyType> dropdownFieldKeyHandler(
    filteredList: List<DropdownItem<KeyType>>,
    onSelect: (KeyType) -> Unit,
    setExpanded: (Boolean) -> Unit,
): (KeyEvent) -> Boolean = { event ->
    if (event.key == Key.Enter && filteredList.size == 1) {
        onSelect(filteredList.first().value)
        setExpanded(false)
        true
    } else {
        false
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExposedDropdownMenuBoxScope.DropdownFieldTextField(
    isError: Boolean,
    textFieldState: TextFieldState,
    label: String,
    labelBackground: Color,
    placeholder: String,
    errorState: State<String?>,
    onClear: () -> Unit,
) {
    OutlinedTextField(
        isError = isError,
        state = textFieldState,
        label = { if (label.isNotEmpty()) Label(label, labelBackground) },
        trailingIcon = {
            AnimatedVisibility(textFieldState.text.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(imageVector = Icons.Outlined.Cancel, contentDescription = stringResource(string.cancel))
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        labelPosition = TextFieldLabelPosition.Attached(alwaysMinimize = true),
        placeholder = { Text(placeholder) },
        supportingText = { errorState.value?.let { Text(it) } },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dropdown_textfield")
            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <KeyType> ExposedDropdownMenuBoxScope.DropdownFieldMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    placeholder: String,
    showPlaceholderOption: Boolean,
    filteredList: List<DropdownItem<KeyType>>,
    onItemSelected: (DropdownItem<KeyType>) -> Unit,
) {
    ExposedDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        containerColor = LocalChatColors.current.token.background.surface.subtle,
        border = BorderStroke(1.dp, LocalChatColors.current.token.border.default)
    ) {
        if (placeholder.isNotBlank() && showPlaceholderOption) {
            DropdownMenuItem(
                modifier = Modifier.testTag("dropdown_item_placeholder"),
                enabled = false,
                onClick = { /* no-op */ },
                text = { Text(text = placeholder, style = Typography.bodyLarge) }
            )
            HorizontalDivider()
        }
        filteredList.forEach { item ->
            DropdownMenuItem(
                modifier = Modifier.testTag("dropdown_item_${item.label}"),
                onClick = { onItemSelected(item) },
                text = { Text(item.label, style = Typography.bodyLarge) }
            )
        }
    }
}

@Preview
@Composable
private fun DropdownPreview() {
    val options = remember {
        sequenceOf(
            DropdownItem("aaa", 1),
            DropdownItem("ab", 2),
            DropdownItem("ba", 3),
        )
    }
    val value = remember { mutableStateOf<Int?>(null) }

    ChatTheme {
        Surface(
            modifier = Modifier.systemBarsPadding(),
            color = ChatTheme.chatColors.token.background.surface.subtle,
        ) {
            Column(Modifier.padding(space.large)) {
                DropdownField(
                    modifier = Modifier.testTag("custom_value_list"),
                    label = "label",
                    value = value.value,
                    options = options,
                    onSelect = { selected ->
                        value.value = if (value.value == selected) {
                            null
                        } else {
                            selected
                        }
                    },
                )
                Spacer(Modifier.height(200.dp))
                Text(
                    text = "Selected: ${value.value ?: "None"}",
                    style = Typography.bodyLarge
                )
            }
        }
    }
}
