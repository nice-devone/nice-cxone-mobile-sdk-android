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

package com.nice.cxonechat.ui.composable.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.nice.cxonechat.ui.composable.generic.Requirement
import com.nice.cxonechat.ui.composable.generic.Requirements.allOf
import com.nice.cxonechat.ui.composable.generic.Requirements.email
import com.nice.cxonechat.ui.composable.generic.Requirements.floating
import com.nice.cxonechat.ui.composable.generic.Requirements.none
import com.nice.cxonechat.ui.composable.generic.Requirements.required

@Composable
internal fun ChatTheme.TextField(
    label: String?,
    value: TextFieldState,
    modifier: Modifier = Modifier,
    minimizedLabelBackground: Color? = null,
    lineLimits: TextFieldLineLimits = TextFieldLineLimits.SingleLine,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    validate: Requirement = none,
    placeholder: String? = null,
    onValueChange: (String) -> Unit = { }
) {
    var error by remember { mutableStateOf(null as String?) }
    var focused by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(value.text) {
        onValueChange(value.text.toString())
    }

    OutlinedTextField(
        state = value,
        modifier = modifier
            .onFocusChanged {
                if (it.isFocused != focused) {
                    val textValue = value.text.toString()
                    error = if (it.isFocused) {
                        null
                    } else {
                        validate(context, textValue)
                    }
                    focused = it.isFocused
                    onValueChange(textValue)
                }
            },
        label = {
            label?.let { labelText ->
                Text(
                    text = labelText,
                    modifier = minimizedLabelBackground?.let {
                        Modifier
                            .background(it)
                            .padding(horizontal = space.small)
                    } ?: Modifier,
                    color = if (error == null) {
                        chatColors.textFieldLabelText
                    } else {
                        colorScheme.error
                    }
                )
            }
        },
        labelPosition = TextFieldLabelPosition.Attached(alwaysMinimize = minimizedLabelBackground != null),
        placeholder = placeholder?.let { { Text(it) } },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = keyboardOptions,
        lineLimits = lineLimits,
    )
}

@PreviewLightDark
@Composable
private fun ValidatedTextFieldPreview() {
    ChatTheme {
        val text = rememberTextFieldState("")
        val number = rememberTextFieldState("")
        val mail = rememberTextFieldState("")
        Surface {
            Column(
                verticalArrangement = spacedBy(ChatTheme.space.large),
                modifier = Modifier.padding(ChatTheme.space.large)
            ) {
                ChatTheme.TextField(
                    label = "Text",
                    placeholder = "This value is required",
                    value = text,
                    modifier = Modifier.fillMaxWidth(1f),
                    validate = required
                )
                ChatTheme.TextField(
                    label = "Numeric",
                    placeholder = "Input number",
                    value = number,
                    modifier = Modifier.fillMaxWidth(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    validate = allOf(required, floating)
                )
                ChatTheme.TextField(
                    label = "E-Mail",
                    minimizedLabelBackground = ChatTheme.colorScheme.surfaceContainerHigh,
                    value = mail,
                    modifier = Modifier.fillMaxWidth(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    validate = allOf(required, email)
                )
            }
        }
    }
}
