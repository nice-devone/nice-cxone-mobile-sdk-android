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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

@Composable
internal fun AppTheme.Alert(
    message: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    onDismiss: () -> Unit,
    dismissLabel: String,
) {
    Dialog(
        modifier = modifier,
        title = title,
        onDismiss = onDismiss,
        confirmButton = {
            OutlinedButton(text = dismissLabel, onClick = onDismiss)
        }
    ) {
        Text(message, modifier = Modifier.fillMaxWidth(), style = typography.body2)
    }
}

@Preview
@Composable
private fun AlertPreview() {
    var title by remember { mutableStateOf<String?>("Title") }

    AppTheme {
        AppTheme.Alert(
            message = "Some Message",
            title = title,
            onDismiss = {
                title = if(title != null) null else "Title"
            },
            dismissLabel = "Ok",
        )
    }
}
