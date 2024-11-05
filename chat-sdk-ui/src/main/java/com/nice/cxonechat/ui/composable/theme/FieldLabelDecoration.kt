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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// TODO replace usage of this extension function with supporting text usage
@Composable
internal fun ChatTheme.FieldLabelDecoration(
    label: String?,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = TextFieldDefaults.colors()
    val labelColor = if (isError) colors.errorLabelColor else colors.unfocusedLabelColor

    Box(modifier = modifier) {
        Box(
            Modifier
                .padding(top = space.medium)
                .border(1.dp, labelColor.copy(0.35f), RoundedCornerShape(4.dp))
        ) {
            Box(Modifier.padding(space.large)) {
                content()
            }
        }

        if(label != null) {
            Row(
                Modifier
                    .padding(start = space.large - 1.dp)
                    .background(colorScheme.background)
            ) {
                Text(
                    label,
                    modifier = Modifier.padding(start = 1.dp, end = 1.dp),
                    color = labelColor,
                    style = chatTypography.surveyListItem
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FieldLabelDecorationPreview() {
    ChatTheme {
        var name by remember { mutableStateOf("Fred") }
        var error: String? by remember { mutableStateOf(null) }

        Column {
            ChatTheme.TextField(
                "Text Field",
                name,
                onValueChanged = {
                    name = it
                    error = if (it.isEmpty()) "Missing Name" else "Greeting"
                }
            )
            ChatTheme.FieldLabelDecoration(label = error ?: name, isError = error != null) {
                Text("Greetings $name", modifier = Modifier.fillMaxWidth(1f))
            }
        }
    }
}
