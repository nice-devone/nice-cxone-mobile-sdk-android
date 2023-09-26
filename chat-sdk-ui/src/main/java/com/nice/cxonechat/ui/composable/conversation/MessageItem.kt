/*
 * Copyright (c) 2021-2023. NICE Ltd. All rights reserved.
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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.nice.cxonechat.message.MessageDirection.ToAgent
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.model.Message
import com.nice.cxonechat.ui.composable.conversation.model.Message.Attachment
import com.nice.cxonechat.ui.composable.conversation.model.Message.ListPicker
import com.nice.cxonechat.ui.composable.conversation.model.Message.Plugin
import com.nice.cxonechat.ui.composable.conversation.model.Message.QuickReply
import com.nice.cxonechat.ui.composable.conversation.model.Message.RichLink
import com.nice.cxonechat.ui.composable.conversation.model.Message.Text
import com.nice.cxonechat.ui.composable.conversation.model.Message.Unsupported
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatShapes
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.SmallSpacer

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LazyItemScope.MessageItem(
    message: Message,
    modifier: Modifier = Modifier,
    onClick: (Message) -> Unit = {},
    onMessageLongClick: (Message) -> Unit = {},
) {
    val alignment = if (message.direction == ToAgent) Alignment.End else Alignment.Start
    val chatColor = if (message.direction == ToAgent) chatColors.customer else chatColors.agent
    val shape = if (message.direction == ToAgent) chatShapes.bubbleShapeToAgent else chatShapes.bubbleShapeToClient
    Row(
        modifier = modifier
            .fillParentMaxWidth()
            .wrapContentWidth(align = alignment)
            .animateItemPlacement(),
    ) {
        Surface(
            color = chatColor.background,
            contentColor = chatColor.foreground,
            shape = shape,
        ) {
            MessageContent(
                message = message,
                modifier = Modifier
                    .weight(1f)
                    .combinedClickable(
                        onClick = { onClick(message) },
                        onLongClick = { onMessageLongClick(message) },
                    ),
            )
        }
    }
    SmallSpacer()
}

@Composable
private fun MessageContent(
    message: Message,
    modifier: Modifier = Modifier,
) = Column(modifier) {
    val padding = Modifier.padding(space.large)
    when (message) {
        is Unsupported -> Text(
            text = message.fallbackText ?: stringResource(string.text_unsupported_message_type),
            modifier = padding,
        )

        is Text -> Text(text = message.text, modifier = padding)
        is Attachment -> AttachmentMessage(message, modifier = padding)
        is ListPicker -> ListPickerMessage(message, modifier = padding)
        is RichLink -> RichLinkMessage(message = message, modifier = padding)
        is QuickReply -> QuickReplyMessage(message, modifier = padding)
        is Plugin -> PluginMessage(message, modifier = padding)
    }
}

@Preview
@Composable
private fun PreviewContentTextMessage() {
    PreviewMessageItemBase {
        MessageItem(
            message = Text(previewTextMessage("Text message")),
        )
    }
}

@Preview
@Composable
private fun PreviewContentUnsupported() {
    PreviewMessageItemBase {
        MessageItem(
            message = Unsupported(previewTextMessage("Unused")),
        )
    }
}
