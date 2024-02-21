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
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.message.MessageDirection.ToAgent
import com.nice.cxonechat.message.MessageDirection.ToClient
import com.nice.cxonechat.message.MessageStatus.FAILED_TO_DELIVER
import com.nice.cxonechat.message.MessageStatus.READ
import com.nice.cxonechat.message.MessageStatus.SEEN
import com.nice.cxonechat.message.MessageStatus.SENDING
import com.nice.cxonechat.message.MessageStatus.SENT
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.model.Message
import com.nice.cxonechat.ui.composable.conversation.model.Message.ListPicker
import com.nice.cxonechat.ui.composable.conversation.model.Message.Plugin
import com.nice.cxonechat.ui.composable.conversation.model.Message.QuickReply
import com.nice.cxonechat.ui.composable.conversation.model.Message.RichLink
import com.nice.cxonechat.ui.composable.conversation.model.Message.Text
import com.nice.cxonechat.ui.composable.conversation.model.Message.Unsupported
import com.nice.cxonechat.ui.composable.conversation.model.Message.WithAttachments
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatShapes
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.SmallSpacer

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LazyItemScope.MessageItem(
    message: Message,
    showSender: Boolean,
    modifier: Modifier = Modifier,
    onAttachmentClicked: (Attachment) -> Unit,
    onMoreClicked: (List<Attachment>, String) -> Unit,
    onShare: (Collection<Attachment>) -> Unit,
) {
    val toAgent = message.direction == ToAgent
    val alignment = if (toAgent) Alignment.End else Alignment.Start
    val chatColor = if (toAgent) chatColors.customer else chatColors.agent
    val shape = if (toAgent) chatShapes.bubbleShapeToAgent else chatShapes.bubbleShapeToClient
    val showAgentSender = showSender && !toAgent

    Row(
        modifier = modifier
            .fillParentMaxWidth()
            .wrapContentWidth(align = alignment)
            .animateItemPlacement(),
    ) {
        Column(horizontalAlignment = alignment) {
            if (showAgentSender) {
                Text(
                    message.sender?.ifBlank { null } ?: stringResource(id = string.default_agent_name),
                    style = chatTypography.chatAgentName,
                )
            }
            Surface(
                color = chatColor.background,
                contentColor = chatColor.foreground,
                shape = shape,
            ) {
                MessageContent(
                    message = message,
                    modifier = Modifier
                        .weight(1f),
                    onAttachmentClicked = onAttachmentClicked,
                    onMoreClicked = onMoreClicked,
                    onShare = onShare,
                )
            }
            if (toAgent) {
                MessageStatus(message)
            }
        }
    }
    SmallSpacer()
}

@Composable
private fun MessageStatus(message: Message) {
    Text(
        when (message.status) {
            SENDING -> stringResource(string.status_sending)
            SENT -> stringResource(string.status_sent)
            FAILED_TO_DELIVER -> stringResource(string.status_failed)
            SEEN -> stringResource(string.status_received)
            READ -> stringResource(string.status_read)
        },
        style = chatTypography.chatStatus,
    )
}

@Composable
private fun MessageContent(
    message: Message,
    modifier: Modifier = Modifier,
    onAttachmentClicked: (Attachment) -> Unit,
    onMoreClicked: (List<Attachment>, String) -> Unit,
    onShare: (Collection<Attachment>) -> Unit,
) = Column(modifier) {
    val padding = Modifier.padding(space.large)
    when (message) {
        is Unsupported -> Text(
            text = message.fallbackText ?: stringResource(string.text_unsupported_message_type),
            modifier = padding,
        )

        is Text -> Text(
            text = message.text,
            modifier = padding,
            style = chatTypography.chatMessage
        )
        is WithAttachments -> AttachmentMessage(
            message,
            modifier = padding,
            onAttachmentClicked = onAttachmentClicked,
            onMoreClicked = onMoreClicked,
            onShare = onShare,
        )
        is ListPicker -> ListPickerMessage(message, modifier = padding)
        is RichLink -> RichLinkMessage(message = message, modifier = padding)
        is QuickReply -> QuickReplyMessage(message, modifier = padding)
        is Plugin -> PluginMessage(message, modifier = padding)
    }
}

@Preview
@Composable
private fun PreviewContentTextMessage() {
    PreviewMessageItemBase(
        message = Text(previewTextMessage("Text message", direction = ToClient)),
        showSender = true,
    )
}

@Preview
@Composable
private fun PreviewContentUnsupported() {
    PreviewMessageItemBase(
        message = Unsupported(previewTextMessage("Unused")),
        showSender = true,
    )
}
