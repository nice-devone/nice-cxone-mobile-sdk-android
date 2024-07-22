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

package com.nice.cxonechat.ui.composable.theme

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize.Min
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.composable.theme.ChatColors.ColorPair

@Immutable
internal data class ChatColors(
    val agent: ColorPair,
    val customer: ColorPair,
    val chatInfoLabel: ColorPair,
    val positionInQueueBackground: Color,
    val positionInQueueForeground: Color,
) {

    data class ColorPair(
        val background: Color,
        val foreground: Color,
    )

    constructor(colors: ThemeColors) : this(
        agent = ColorPair(colors.agentBackground, colors.agentText),
        customer = ColorPair(colors.customerBackground, colors.customerText),
        chatInfoLabel = ColorPair(colors.background, colors.onBackground),
        positionInQueueBackground = colors.positionInQueueBackground,
        positionInQueueForeground = colors.positionInQueueForeground,
    )
}

internal val LocalChatColors = staticCompositionLocalOf {
    ChatColors(DefaultColors.light)
}

@Composable
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun PreviewChatColorsNight() {
    ThemeColorsList()
}

@Composable
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
private fun PreviewChatColors() {
    ThemeColorsList()
}

@Composable
private fun ThemeColorsList() {
    val colors = listOf(
        "agent" to ChatTheme.chatColors.agent,
        "customer" to ChatTheme.chatColors.customer,
        "chatInfoLabel" to ChatTheme.chatColors.chatInfoLabel,
        "positionInQueue" to ColorPair(
            ChatTheme.chatColors.positionInQueueBackground,
            ChatTheme.chatColors.positionInQueueForeground
        )
    )
    ChatTheme {
        Surface {
            Column(
                verticalArrangement = spacedBy(8.dp),
                modifier = Modifier
                    .padding(8.dp)
                    .width(Min)
            ) {
                colors.forEach { (label, colorPair) ->
                    Text(
                        text = label,
                        color = colorPair.foreground,
                        modifier = Modifier
                            .background(color = colorPair.background)
                            .padding(8.dp)
                            .fillMaxWidth(),
                    )
                }
            }
        }
    }
}
