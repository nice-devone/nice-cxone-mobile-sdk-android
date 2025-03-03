/*
 * Copyright (c) 2021-2025. NICE Ltd. All rights reserved.
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

import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.composable.theme.ChatTheme

@Composable
internal fun DialogTitle(text: String, modifier: Modifier = Modifier) = Text(
    text = text,
    style = ChatTheme.chatTypography.dialogTitle,
    modifier = modifier,
)

@Composable
internal fun CardTitle(title: String, modifier: Modifier = Modifier) = Text(
    text = title,
    style = ChatTheme.chatTypography.chatCardTitle,
    modifier = modifier,
)

@Composable
@Preview
private fun PreviewTexts() {
    ChatTheme {
        Column(
            verticalArrangement = spacedBy(8.dp),
            modifier = Modifier.padding(8.dp)
        ) {
            DialogTitle(text = "Dialog title")
            CardTitle(title = "Card title")
        }
    }
}
