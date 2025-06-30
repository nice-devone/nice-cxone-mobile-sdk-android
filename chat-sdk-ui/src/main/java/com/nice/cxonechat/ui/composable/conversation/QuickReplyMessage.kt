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

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.model.Action
import com.nice.cxonechat.ui.composable.conversation.model.Message.QuickReply
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.util.preview.message.UiSdkQuickReply

@Composable
internal fun QuickReplyMessage(
    message: QuickReply,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message.title, style = chatTypography.chatMessage)
    }
}

@Composable
internal fun QuickReplySubFrame(
    message: QuickReply,
    isMessageExtraAvailable: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!isMessageExtraAvailable) {
        Column(
            Modifier
                .padding(
                    top = 9.dp,
                    bottom = space.medium + space.messageAvatarSize / 2, // workaround to keep avatar as simple overlay
                )
                .alpha(0.5f)
                .testTag("quick_reply_option_selected")
                .then(modifier)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                CompositionLocalProvider(LocalContentColor provides ChatTheme.colorScheme.onBackground) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .border(width = 1.dp, color = LocalContentColor.current, shape = RoundedCornerShape(size = 16.dp))
                            .width(24.dp)
                            .height(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                        )
                    }
                    Text(text = stringResource(string.option_selected), style = chatTypography.quickReplySelectedText)
                }
            }
        }
    } else {
        Column(
            Modifier
                .padding(top = 12.dp)
                .then(modifier)
        ) {
            QuickReplyOptions(message, onOptionSelected = onClick)
        }
    }
}

@Composable
private fun QuickReplyOptions(message: QuickReply, onOptionSelected: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = Modifier
            .testTag("quick_reply_options")
            .then(modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        var selected: Action? by remember { mutableStateOf(null) }
        CompositionLocalProvider(LocalContentColor provides ChatTheme.colorScheme.primary) {
            ChipGroup(
                actions = message.actions,
                selection = selected,
                colors = ChipDefaults.chipColors(containerColor = ChatTheme.chatColors.agent.background)
            ) {
                selected = it
                onOptionSelected()
            }
        }
    }
}

@Preview
@Composable
private fun QuickReplyMessagePreview() {
    PreviewMessageItemBase(
        message = QuickReply(UiSdkQuickReply()) {},
    )
}
