/*
 * Copyright (c) 2021-2026. NICE Ltd. All rights reserved.
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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.composable.conversation.model.Message
import com.nice.cxonechat.ui.composable.conversation.model.getMessageAccessibility
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.util.preview.message.Text

/**
 * Displays an emoji message with appropriate text styling.
 *
 * Applies larger font size for optimal emoji rendering.
 *
 * @param message The message containing emoji/text content.
 * @param paddingValues Padding to apply to the message container.
 */
@Composable
internal fun EmojiMessage(message: Message.EmojiText, paddingValues: PaddingValues) {
    val accessibilityDescription = message.getMessageAccessibility()

    Text(
        text = message.text,
        modifier = Modifier
            .padding(paddingValues)
            .semantics {
                testTag = "emoji_message"
                contentDescription = accessibilityDescription
            },
        style = chatTypography.chatEmojiMessage,
    )
}

@PreviewLightDark
@Composable
private fun EmojiMessagePreview() {
    PreviewMessageItemBase {
        EmojiMessage(
            message = Message.EmojiText(Text(text = "👋😊")),
            paddingValues = PaddingValues(8.dp)
        )
    }
}
