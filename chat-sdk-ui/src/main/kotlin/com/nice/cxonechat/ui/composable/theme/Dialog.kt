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

package com.nice.cxonechat.ui.composable.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.DialogProperties
import com.nice.cxonechat.ui.R

@Composable
internal fun ChatTheme.Dialog(
    modifier: Modifier = Modifier,
    title: String? = null,
    titlePadding: Dp = space.large,
    onDismiss: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = confirmButton,
        modifier = modifier,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        dismissButton = dismissButton,
        containerColor = chatColors.token.background.surface.subtle,
        text = {
            Column {
                title?.let {
                    Text(
                        it,
                        modifier = Modifier.padding(bottom = titlePadding),
                        style = chatTypography.dialogTitle,
                        color = colorScheme.onSurface
                    )
                }
                content()
            }
        }
    )
}

@PreviewLightDark
@Composable
private fun DialogPreviewTitle() {
    ChatTheme {
        ChatTheme.Dialog(
            title = "Title",
            onDismiss = { },
            dismissButton = { TextButton(onClick = {}) { Text("Dismiss") } },
            confirmButton = { TextButton(onClick = {}) { Text(stringResource(R.string.ok)) } },
        ) {
            val body = rememberTextFieldState("Body")
            ChatTheme.TextField(label = "Label", value = body)
        }
    }
}

@Preview
@Composable
private fun DialogPreviewNoTitle() {
    ChatTheme {
        ChatTheme.Dialog(
            onDismiss = { },
            dismissButton = { TextButton(onClick = {}) { Text("Dismiss") } },
            confirmButton = { TextButton(onClick = {}) { Text(stringResource(R.string.ok)) } },
        ) {
            val body = rememberTextFieldState("Body")
            ChatTheme.TextField(label = "Label", value = body)
        }
    }
}
