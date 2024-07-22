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

package com.nice.cxonechat.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.Dialog
import com.nice.cxonechat.ui.composable.theme.OutlinedButton
import com.nice.cxonechat.ui.composable.theme.TextField

@Composable
internal fun EditThreadNameDialog(
    threadName: String,
    onCancel: () -> Unit,
    onAccept: (String) -> Unit,
) {
    var name by remember { mutableStateOf(threadName) }

    ChatTheme.Dialog(
        title = stringResource(string.update_thread_name),
        onDismiss = onCancel,
        confirmButton = {
            ChatTheme.OutlinedButton(text = stringResource(string.ok)) {
                onAccept(name)
            }
        },
        dismissButton = {
            ChatTheme.OutlinedButton(text = stringResource(string.cancel), onClick = onCancel)
        }
    ) {
        ChatTheme.TextField(label = stringResource(string.enter_thread_name), value = name) { name = it }
    }
}

@Preview
@Composable
private fun PreviewEditThreadDialog() {
    ChatTheme {
        var name = ""

        EditThreadNameDialog(
            threadName = name,
            onCancel = { },
            onAccept = { name = it }
        )
    }
}
