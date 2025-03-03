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

package com.nice.cxonechat.ui.composable.conversation

import android.icu.util.Calendar.getInstance
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.tooling.preview.MultiLocalePreview
import com.nice.cxonechat.ui.util.toShortDateString
import java.util.Calendar

@Composable
internal fun MessageGroupHeader(dayString: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(vertical = ChatTheme.space.medium, horizontal = ChatTheme.space.large)
    ) {
        DayHeaderLine()
        Text(
            text = dayString,
            modifier = Modifier.padding(horizontal = ChatTheme.space.large),
            style = ChatTheme.chatTypography.chatDayHeader,
            color = ChatTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
        DayHeaderLine()
    }
}

@Composable
private fun RowScope.DayHeaderLine() {
    Spacer(
        modifier = Modifier
            .weight(1f)
            .align(Alignment.CenterVertically),
    )
}

@MultiLocalePreview
@PreviewLightDark
@Preview
@Composable
private fun PreviewMessageGroupHeader() {
    val context = LocalContext.current
    val previewValues = listOf(
        "Today" to context.toShortDateString(getInstance().time),
        "Yesterday" to context.toShortDateString(
            getDate(Calendar.DAY_OF_YEAR, -1)
        ),
        "The day before yesterday" to context.toShortDateString(
            getDate(Calendar.DAY_OF_YEAR, -2)
        ),
        "Long time ago" to context.toShortDateString(
            getDate(Calendar.YEAR, -2)
        )
    )
    ChatTheme {
        Surface {
            Column {
                previewValues.forEachIndexed { index, (label, dayString) ->
                    if (index != 0) HorizontalDivider()
                    PreviewItem(label, dayString)
                }
            }
        }
    }
}

@Composable
private fun PreviewItem(
    label: String,
    dayString: String,
) {
    MessageGroupHeader(dayString)
    Text(label, style = ChatTheme.typography.titleSmall)
}

private fun getDate(
    field: Int,
    amount: Int,
) = getInstance().apply {
    add(field, amount)
}.time
