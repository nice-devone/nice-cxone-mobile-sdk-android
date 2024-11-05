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

package com.nice.cxonechat.sample.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.nice.cxonechat.sample.R
import com.nice.cxonechat.sample.ui.theme.AppTheme
import com.nice.cxonechat.sample.ui.theme.OutlinedButton

/**
 * Display a busy spinner and a status message.
 */
@Composable
fun BusySpinner(message: String, onCancel: (() -> Unit)? = null) {
    Dialog(
        onDismissRequest = { },
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = AppTheme.colorScheme.background.copy(alpha = 0.75f)
            )
        ) {
            Column(
                modifier = Modifier.padding(AppTheme.space.defaultPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppTheme.space.medium),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = AppTheme.colorScheme.primary,
                )
                Text(message)
                onCancel?.let {
                    AppTheme.OutlinedButton(
                        text = stringResource(id = R.string.cancel),
                        onClick = onCancel
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun BusySpinnerPreview() {
    AppTheme {
        BusySpinner(message = "Loading...", onCancel = { })
    }
}
