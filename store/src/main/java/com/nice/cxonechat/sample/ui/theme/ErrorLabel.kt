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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.sample.R.string

/**
 * A label to be used as either a label or an error message.
 *
 * When no [error] is present creates a simple Text composable with default
 * styling and a solid background.
 *
 * When [error] is specified, creates a Text field with red (AppTheme.colors.error) text
 * on a solid background.
 *
 * @param label Label to display.
 * @param error Error to display, if any.
 */
@Composable
fun AppTheme.ErrorLabel(label: String?, error: String?) {
    when {
        error != null ->
            Text(
                label?.let { stringResource(string.error_validation_label, it, error) } ?: error,
                Modifier.background(colors.background),
                color = colors.error
            )
        label != null ->
            Text(label, Modifier.background(colors.background))
    }
}

@Preview
@Composable
private fun ErrorLabelPreview() {
    AppTheme {
        Column(
            Modifier
                .background(AppTheme.colors.onBackground)
                .padding(8.dp)
        ) {
            AppTheme.ErrorLabel(label = "Label", error = "Error")
            AppTheme.ErrorLabel(label = "Label", error = null)
        }
    }
}
