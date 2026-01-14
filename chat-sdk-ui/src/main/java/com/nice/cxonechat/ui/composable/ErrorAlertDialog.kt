/*
 * Copyright (c) 2021-2026. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.ui.composable

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.window.DialogProperties
import com.nice.cxonechat.ui.composable.theme.ChatTheme

/**
 * A simple alert dialog to show error messages.
 *
 * @param title The title of the dialog.
 * @param body The body text of the dialog.
 * @param modifier The modifier to be applied to the dialog.
 * @param buttonText The text of the confirm button.
 * @param onConfirmClick The callback to be invoked when the confirm button is clicked.
 */
@Composable
internal fun ChatTheme.ErrorAlertDialog(
    title: String,
    body: String,
    modifier: Modifier,
    buttonText: String,
    onConfirmClick: () -> Unit,
) {
    AlertDialog(
        modifier = modifier.testTag("chat_error_dialog"),
        onDismissRequest = onConfirmClick,
        title = { Text(text = title) },
        text = { Text(body) },
        containerColor = colorScheme.surface,
        confirmButton = {
            TextButton(
                onClick = onConfirmClick,
                modifier = Modifier.testTag("chat_error_close_button"),
            ) {
                Text(buttonText)
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        )
    )
}

@Composable
@PreviewLightDark
private fun PreviewErrorAlertDialog() {
    ChatTheme {
        ChatTheme.ErrorAlertDialog(
            title = "Error",
            body = "An unexpected error occurred.",
            modifier = Modifier,
            buttonText = "Close",
            onConfirmClick = {}
        )
    }
}
