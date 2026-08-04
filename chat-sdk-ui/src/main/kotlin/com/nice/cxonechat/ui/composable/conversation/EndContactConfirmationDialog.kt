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

package com.nice.cxonechat.ui.composable.conversation

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.Dialog

@Composable
internal fun EndContactConfirmationDialog(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    ChatTheme.Dialog(
        title = stringResource(string.attention),
        modifier = Modifier.testTag("end_conversation_confirmation_dialog"),
        onDismiss = onCancel,
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag("confirm_button"),
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
        Text(
            text = stringResource(string.livechat_end_confirmation_text),
            style = ChatTheme.chatTypography.dialogSubText
        )
    }
}

@PreviewLightDark
@Composable
private fun PreviewEndContactConfirmation() {
    ChatTheme {
        EndContactConfirmationDialog(
            onCancel = {},
            onConfirm = {}
        )
    }
}
