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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
internal fun ChatTheme.Dialog(
    modifier: Modifier = Modifier,
    title: String? = null,
    onDismiss: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = confirmButton,
        modifier = modifier,
        dismissButton = dismissButton,
        text = {
            Column {
                title?.let {
                    Text(
                        it,
                        modifier = Modifier.padding(bottom = space.large),
                        style = chatTypography.dialogTitle
                    )
                }
                content()
            }
        }
    )
}

@Preview
@Composable
private fun DialogPreviewTitle() {
    ChatTheme {
        ChatTheme.Dialog(
            title = "Title",
            onDismiss = { },
            dismissButton = { ChatTheme.OutlinedButton(text = "Dismiss") {} },
            confirmButton = { ChatTheme.OutlinedButton(text = "Confirm") {} }
        ) {
            ChatTheme.TextField(label = "Label", value = "Body", onValueChanged = { _ -> })
        }
    }
}

@Preview
@Composable
private fun DialogPreviewNoTitle() {
    ChatTheme {
        ChatTheme.Dialog(
            onDismiss = { },
            dismissButton = { ChatTheme.OutlinedButton(text = "Dismiss") {} },
            confirmButton = { ChatTheme.OutlinedButton(text = "Confirm") {} }
        ) {
            ChatTheme.TextField(label = "Label", value = "Body", onValueChanged = { _ -> })
        }
    }
}
