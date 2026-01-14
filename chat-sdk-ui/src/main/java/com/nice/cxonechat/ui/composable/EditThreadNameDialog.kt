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

import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.LocalAbsoluteTonalElevation
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.Dialog
import com.nice.cxonechat.ui.composable.theme.TextField

@Composable
internal fun EditThreadNameDialog(
    threadName: String,
    onCancel: () -> Unit,
    onAccept: (String) -> Unit,
) {
    val name = rememberTextFieldState(threadName)
    CompositionLocalProvider(
        LocalAbsoluteTonalElevation provides 0.dp
    ) {
        ChatTheme.Dialog(
            title = stringResource(string.update_thread_name),
            modifier = Modifier.testTag("edit_thread_name_dialog"),
            onDismiss = onCancel,
            confirmButton = {
                TextButton(
                    onClick = { onAccept(name.text.toString()) },
                    modifier = Modifier.testTag("confirm_button"),
                    enabled = name.text != threadName,
                ) {
                    Text(text = stringResource(string.confirm), style = ChatTheme.chatTypography.dialogButtonText)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.testTag("cancel_button"),
                ) {
                    Text(text = stringResource(string.cancel), style = ChatTheme.chatTypography.dialogButtonText)
                }
            }
        ) {
            ChatTheme.TextField(
                label = stringResource(string.label_thread_name),
                placeholder = stringResource(string.enter_thread_name),
                minimizedLabelBackground = Color.Unspecified,
                value = name,
                modifier = Modifier.testTag("edit_thread_name_text_field"),
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PreviewEditThreadDialog() {
    ChatTheme {
        var name = "Test"

        EditThreadNameDialog(
            threadName = name,
            onCancel = { },
            onAccept = { name = it }
        )
    }
}
