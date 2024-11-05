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

package com.nice.cxonechat.ui.composable.generic

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.datasource.LoremIpsum
import com.nice.cxonechat.ui.R.string

/**
 * Alert dialog which will display supplied [message] and confirmation button.
 * It will invoke [dismiss] for [Button] `onClick` call and for [AlertDialog] `onDismissRequest` call.
 */
@Composable
internal fun SimpleAlertDialog(
    message: String,
    dismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = dismiss,
        confirmButton = {
            Button(
                onClick = dismiss,
            ) {
                Text(stringResource(string.ok))
            }
        },
        text = { Text(message) },
    )
}

@Preview
@Composable
private fun SimpleAlertDialogPreview() {
    MaterialTheme {
        SimpleAlertDialog(message = LoremIpsum(7).values.joinToString()) { }
    }
}
