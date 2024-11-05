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

package com.nice.cxonechat.ui.composable.conversation

import android.icu.text.MessageFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import java.util.Locale

@Composable
internal fun PositionInQueue(
    position: Int,
    modifier: Modifier = Modifier.fillMaxWidth(1f)
) {
    val background = ChatTheme.chatColors.positionInQueueBackground
    val foreground = ChatTheme.chatColors.positionInQueueForeground
    val formatted = when {
        position == 1 -> stringResource(id = string.position_in_queue_next)
        else -> MessageFormat(stringResource(id = string.position_in_queue_ordinal), Locale.getDefault()).format(arrayOf(position))
    }

    Box(
        modifier = modifier
            .padding(8.dp)
            .background(background)
            .border(width = 1.dp, foreground)
            .padding(2.dp)
            .border(width = 2.dp, foreground)
            .padding(8.dp)
    ) {
        Text(formatted, color = foreground)
    }
}

@Preview
@Composable
private fun PositionInQueue_Preview() {
    ChatTheme {
        Column {
            for (position in 1.until(4)) {
                PositionInQueue(position = position)
            }
        }
    }
}
