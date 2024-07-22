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
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

/**
 * App themed dialog box.
 *
 * @param modifier Compose Modifier to apply.
 * @param title optional title to display
 * @param onDismiss dismiss action when clicking outside of dialog.
 * @param confirmButton "confirm" button displayed in last position.
 * @param dismissButton "dismiss" button displayed in first position.
 * @param content Composable content of alert.
 */
@Composable
internal fun AppTheme.Dialog(
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
                title?.let { Text(title, modifier = Modifier.padding(bottom = space.medium), style = typography.h6) }
                content()
            }
        }
    )
}

/**
 * App themed dialog box with a simple text message.
 *
 * @param modifier Compose Modifier to apply.
 * @param title optional title to display.
 * @param text Text to display in dialog.
 * @param onDismiss dismiss action when clicking outside of dialog.
 * @param confirmButton "confirm" button displayed in last position.
 * @param dismissButton "dismiss" button displayed in first position.
 */
@Composable
internal fun AppTheme.Dialog(
    modifier: Modifier = Modifier,
    title: String? = null,
    text: String,
    onDismiss: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
) {
    Dialog(
        modifier = modifier,
        title = title,
        onDismiss = onDismiss,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
    ) {
        Text(text, style = typography.body2)
    }
}

@Preview
@Composable
private fun TextDialogPreview() {
    AppTheme {
        AppTheme.Dialog(
            title = "Title",
            text = "Body",
            onDismiss = { },
            dismissButton = { AppTheme.OutlinedButton(text = "Dismiss") {} },
            confirmButton = { AppTheme.OutlinedButton(text = "Confirm") {} }
        )
    }
}

@Preview
@Composable
private fun DialogPreview() {
    AppTheme {
        AppTheme.Dialog(
            title = "Title",
            onDismiss = {},
            dismissButton = { AppTheme.OutlinedButton(text = "Dismiss") {} },
            confirmButton = { AppTheme.OutlinedButton(text = "Confirm") {} }
        ) {
            Text("Body")
        }
    }
}
