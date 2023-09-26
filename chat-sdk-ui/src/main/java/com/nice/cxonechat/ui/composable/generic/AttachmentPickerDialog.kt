/*
 * Copyright (c) 2021-2023. NICE Ltd. All rights reserved.
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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nice.cxonechat.ui.R.array
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.theme.ChatTheme

@Composable
internal fun AttachmentPickerDialog(
    onDismissed: () -> Unit,
    getContent: (mimeType: String) -> Unit,
) {
    val (selectedOption, onOptionSelected) = remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismissed,
        dismissButton = {
            Button(onClick = {
                onDismissed()
            }) {
                Text(text = stringResource(string.cancel))
            }
        },
        confirmButton = {
            Button(onClick = {
                selectedOption?.let(getContent)
                onDismissed()
            }) {
                Text(text = stringResource(string.ok))
            }
        },
        title = {
            Text(text = stringResource(string.title_attachment_picker))
        },
        text = {
            val labels = stringArrayResource(array.attachment_type_labels)
            val options = stringArrayResource(array.attachment_type_mimetypes)
            Column {
                labels.zip(options).forEach { (label, option) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = option == selectedOption,
                                onClick = { onOptionSelected(option) }
                            )
                            .padding(horizontal = ChatTheme.space.large),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = option == selectedOption,
                            onClick = { onOptionSelected(option) }
                        )
                        Text(
                            text = label,
                            style = ChatTheme.typography.body1.merge(),
                            modifier = Modifier.padding(start = ChatTheme.space.large)
                        )
                    }
                }
            }
        }
    )
}

@Preview
@Composable
private fun DialogPreview() {
    ChatTheme {
        Column {
            val (selection, onSelected) = remember { mutableStateOf<String?>(null) }
            if (selection == null) {
                AttachmentPickerDialog(onDismissed = {}, getContent = { onSelected(it) })
            }
            Text(text = selection ?: "Nothing")
            Button(onClick = { onSelected(null) }) {
                Text(text = "Reset dialog")
            }
        }
    }
}
