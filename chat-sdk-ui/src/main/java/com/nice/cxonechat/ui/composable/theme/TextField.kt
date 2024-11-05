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

package com.nice.cxonechat.ui.composable.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
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
import com.nice.cxonechat.ui.composable.generic.Requirement
import com.nice.cxonechat.ui.composable.generic.Requirements.allOf
import com.nice.cxonechat.ui.composable.generic.Requirements.email
import com.nice.cxonechat.ui.composable.generic.Requirements.floating
import com.nice.cxonechat.ui.composable.generic.Requirements.none
import com.nice.cxonechat.ui.composable.generic.Requirements.required

@Composable
internal fun ChatTheme.TextField(
    label: String?,
    value: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    validate: Requirement = none,
    onValueChanged: (String) -> Unit = { _ -> },
) {
    var error by remember { mutableStateOf(null as String?) }
    var focused by remember { mutableStateOf(false) }
    val context = LocalContext.current

    OutlinedTextField(
        value = value,
        onValueChange = { onValueChanged(it) },
        modifier = modifier
            .onFocusChanged {
                if (it.isFocused != focused) {
                    error = if (it.isFocused) {
                        null
                    } else {
                        validate(context, value)
                    }
                    focused = it.isFocused
                }
            },
        label = { ErrorLabel(label, error) },
        isError = error != null,
        keyboardOptions = keyboardOptions,
        singleLine = singleLine,
    )
}

@Preview
@Composable
private fun ValidatedTextFieldPreview() {
    ChatTheme {
        var text by remember { mutableStateOf("") }
        var number by remember { mutableStateOf("") }
        var mail by remember { mutableStateOf("") }

        Column(modifier = Modifier.padding(ChatTheme.space.large)) {
            ChatTheme.TextField(
                label = "Text",
                value = text,
                modifier = Modifier.fillMaxWidth(1f),
                validate = required
            ) { text = it }
            ChatTheme.TextField(
                label = "Numeric",
                value = number,
                modifier = Modifier.fillMaxWidth(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                validate = allOf(required, floating)
            ) { number = it }
            ChatTheme.TextField(
                label = "E-Mail",
                value = mail,
                modifier = Modifier.fillMaxWidth(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                validate = allOf(required, email)
            ) { mail = it }
        }
    }
}
