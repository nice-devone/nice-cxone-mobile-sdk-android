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

package com.nice.cxonechat.sample.ui.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.nice.cxonechat.sample.utilities.Requirement
import com.nice.cxonechat.sample.utilities.Requirements.allOf
import com.nice.cxonechat.sample.utilities.Requirements.email
import com.nice.cxonechat.sample.utilities.Requirements.floating
import com.nice.cxonechat.sample.utilities.Requirements.none
import com.nice.cxonechat.sample.utilities.Requirements.required

/**
 * An App-themed OutlinedTextField with support for automatic field validation.
 *
 * Fields are validated when focus is lost.
 *
 * @param label Label for field.
 * @param value Current value for field.
 * @param modifier Compose Modifier for field.
 * @param enabled true iff the field should be enabled.
 * @param singleLine true iff newlines are not allowed in field.
 * @param keyboardOptions software keyboard options that contains configuration such as KeyboardType and ImeAction.
 * @param keyboardActions when the input service emits an IME action, the corresponding callback is called. Note
 * that this IME action may be different from what you specified in KeyboardOptions.imeAction
 * @param requirement [Requirement] to be applied to the field.
 * @param onValueChanged Callback made when value is changed.
 */
@Composable
internal fun AppTheme.TextField(
    label: String?,
    value: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    requirement: Requirement = none,
    onValueChanged: (String) -> Unit = { _ -> },
) {
    var error by remember { mutableStateOf(null as String?) }
    var focused by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (!enabled) {
        error = null
    }

    OutlinedTextField(
        value = value,
        onValueChange = { onValueChanged(it) },
        modifier = modifier
            .onFocusChanged {
                if (it.isFocused != focused) {
                    error = if (it.isFocused) {
                        null
                    } else {
                        requirement(context, value)
                    }
                    focused = it.isFocused
                }
            },
        enabled = enabled,
        label = { ErrorLabel(label, error) },
        isError = enabled && error != null,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
    )
}

@Preview
@Composable
private fun TextFieldPreview() {
    AppTheme {
        var text by remember { mutableStateOf("") }
        var number by remember { mutableStateOf("") }
        var mail by remember { mutableStateOf("") }

        Column(modifier = Modifier.padding(AppTheme.space.large)) {
            AppTheme.TextField(
                label = "Text",
                value = text,
                modifier = Modifier.fillMaxWidth(1f),
                requirement = required
            ) { text = it }
            AppTheme.TextField(
                label = "Numeric",
                value = number,
                modifier = Modifier.fillMaxWidth(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                requirement = allOf(required, floating)
            ) { number = it }
            AppTheme.TextField(
                label = "E-Mail",
                value = mail,
                modifier = Modifier.fillMaxWidth(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                requirement = allOf(required, email)
            ) { mail = it }
        }
    }
}
