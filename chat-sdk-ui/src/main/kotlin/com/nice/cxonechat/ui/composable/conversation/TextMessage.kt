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

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.nice.cxonechat.ui.composable.conversation.model.Message
import com.nice.cxonechat.ui.composable.conversation.model.getMessageAccessibility
import com.nice.cxonechat.ui.composable.generic.AutoLinkedText
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.util.preview.message.Text

/**
 * Displays a simple text message in a conversation.
 *
 * @param message The text message to display.
 * @param modifier Optional modifier to apply to the message container.
 */
@Composable
internal fun TextMessage(message: Message.Text, modifier: Modifier) {
    val accessibilityDescription = message.getMessageAccessibility()

    AutoLinkedText(
        text = message.text,
        modifier = modifier
            .semantics {
                testTag = "text_message"
                contentDescription = accessibilityDescription
            },
        style = chatTypography.chatMessage,
    )
}

@PreviewLightDark
@Composable
private fun TextMessagePreview() {
    PreviewMessageItemBase {
        TextMessage(
            message = Message.Text(Text()),
            modifier = Modifier
        )
    }
}
