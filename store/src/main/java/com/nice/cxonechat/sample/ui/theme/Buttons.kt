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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nice.cxonechat.sample.R.drawable
import com.nice.cxonechat.sample.R.string
import androidx.compose.material3.FloatingActionButton as MaterialFab

/**
 * The outlined button preferred through out the application with the AppTheme
 * styling applied.
 *
 * @param text Text for button.
 * @param modifier Compose Modifier to use.
 * @param isDefault Is this the default button?  Colors will be inverted on default buttons.
 * @param enabled is this button enabled?
 * @param onClick action to take when the button is clicked.
 */
@Composable
fun AppTheme.OutlinedButton(
    text: String,
    modifier: Modifier = Modifier,
    isDefault: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = buttonColors(isDefault),
    ) {
        Text(text)
    }
}

@Composable
private fun AppTheme.buttonColors(isDefault: Boolean): ButtonColors {
    val background = if (isDefault) colorScheme.primary else Color.Transparent
    return ButtonDefaults.buttonColors(
        containerColor = background,
        contentColor = contentColorFor(background)
    )
}

/**
 * Continue button used on various application pages.  Displayed horizontally centered
 * in a row.
 *
 * @param onClick Action to take when button is clicked.
 */
@Composable
fun AppTheme.ContinueButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        OutlinedButton(stringResource(string.continue_button), onClick = onClick)
    }
}

/**
 * The Chat FAB button, with AppTheme applied.
 *
 * @param onClick Action when button is clicked.
 */
@Composable
fun AppTheme.ChatFab(onClick: () -> Unit) {
    MaterialFab(onClick = onClick, containerColor = colorScheme.primary) {
        Icon(painterResource(drawable.ic_chat_24px), stringResource(string.open_chat))
    }
}

@Preview
@Composable
private fun PreviewButtons() {
    AppTheme {
        Column(modifier = Modifier.fillMaxWidth()) {
            AppTheme.ButtonRow {
                AppTheme.OutlinedButton("Default", isDefault = true, onClick = { })
                AppTheme.OutlinedButton("Normal", onClick = { })
            }
            AppTheme.ChatFab {}
        }
    }
}
