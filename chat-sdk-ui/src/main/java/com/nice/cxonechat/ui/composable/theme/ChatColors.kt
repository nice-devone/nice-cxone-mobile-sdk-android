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

package com.nice.cxonechat.ui.composable.theme

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize.Min
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
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
    val token: ThemeColorTokens,
) {
    data class ColorPair(
        val background: Color,
        val foreground: Color,
    )

    val agent: ColorPair = ColorPair(token.background.surface.default, token.content.primary)
    val customer: ColorPair = ColorPair(token.brand.primary, token.brand.onPrimary)
}

internal val LocalChatColors = staticCompositionLocalOf {
    ChatColors(DefaultColors.lightTokens)
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
        "agentBubble" to ChatTheme.chatColors.agent,
        "customerBubble" to ChatTheme.chatColors.customer,
        "chatInfoLabel" to ColorPair(
            ChatTheme.chatColors.token.background.surface.default,
            ChatTheme.chatColors.token.content.primary
        ),
        "agentAvatar" to ColorPair(
            ChatTheme.chatColors.token.brand.primaryContainer,
            ChatTheme.chatColors.token.brand.onPrimaryContainer
        ),
        "leadingMessageIcon" to ColorPair(
            ChatTheme.chatColors.token.border.default,
            ChatTheme.chatColors.token.background.surface.subtle
        ),
    )
    ChatTheme {
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
            Text(
                text = "accentHeader",
                color = ChatTheme.chatColors.token.brand.onPrimary,
                modifier = Modifier
                    .background(ChatTheme.chatColors.token.brand.primary)
                    .padding(8.dp)
                    .fillMaxWidth(),
            )
        }
    }
}
