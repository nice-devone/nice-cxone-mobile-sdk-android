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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.message.MessageDirection.ToAgent
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.FIRST
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.LAST
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.MIDDLE
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.SOLO
import com.nice.cxonechat.ui.composable.conversation.model.Message
import com.nice.cxonechat.ui.composable.conversation.model.Message.ListPicker
import com.nice.cxonechat.ui.composable.conversation.model.Message.QuickReply
import com.nice.cxonechat.ui.composable.conversation.model.Message.RichLink
import com.nice.cxonechat.ui.composable.conversation.model.Message.Text
import com.nice.cxonechat.ui.composable.conversation.model.Message.Unsupported
import com.nice.cxonechat.ui.composable.conversation.model.Message.WithAttachments
import com.nice.cxonechat.ui.composable.conversation.model.PreviewMessageProvider
import com.nice.cxonechat.ui.composable.generic.AutoLinkedText
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.TinySpacer
import com.nice.cxonechat.ui.model.asPerson

@Composable
internal fun LazyItemScope.MessageItem(
    message: Message,
    showStatus: Boolean,
    modifier: Modifier = Modifier,
    itemGroupState: MessageItemGroupState = SOLO,
    onAttachmentClicked: (Attachment) -> Unit,
    onMoreClicked: (List<Attachment>, String) -> Unit,
    onShare: (Collection<Attachment>) -> Unit,
) {
    val toAgent = message.direction == ToAgent
    val alignment = if (toAgent) Alignment.End else Alignment.Start
    val showAvatar = !toAgent && itemGroupState in listOf(SOLO, LAST)

    Column {
        Row(
            modifier = modifier
                .fillParentMaxWidth()
                .wrapContentWidth(align = alignment)
                .animateItem(),
        ) {
            Column(horizontalAlignment = alignment) {
                MessageContent(
                    message = message,
                    position = itemGroupState,
                    onAttachmentClicked = onAttachmentClicked,
                    onMoreClicked = onMoreClicked,
                    onShare = onShare,
                )

                if (toAgent && showStatus) {
                    MessageStatusIndicator(message.status, modifier = Modifier.padding(top = space.small))
                }

                if (showAvatar) {
                    Spacer(modifier = Modifier.height(space.messageAvatarBottomPadding))
                }
            }
        }
        if (!showAvatar) {
            TinySpacer()
        }
    }
}

/**
 * Represents the state of a message regarding its position in a group of similar messages.
 */
internal enum class MessageItemGroupState {
    /** Item is positioned as first in given group. */
    FIRST,

    /** Item is part of the group, but it is not first nor last. */
    MIDDLE,

    /** Item is last in the given group. */
    LAST,

    /** Item is not part of a group. */
    SOLO
}

@Composable
private fun MessageContent(
    message: Message,
    position: MessageItemGroupState,
    modifier: Modifier = Modifier,
    onAttachmentClicked: (Attachment) -> Unit,
    onMoreClicked: (List<Attachment>, String) -> Unit,
    onShare: (Collection<Attachment>) -> Unit,
) = Column(modifier) {
    val padding = Modifier.padding(space.messagePadding)
    val toAgent = message.direction == ToAgent
    val chatColor = if (toAgent) chatColors.customer else chatColors.agent
    val avatar = remember { if (!toAgent && position in listOf(LAST, SOLO)) message.sender?.asPerson else null }
    var isMessageExtraAvailable by rememberSaveable { mutableStateOf(true) }
    MessageFrame(
        position = position,
        isAgent = !toAgent,
        avatar = avatar,
        colors = chatColor,
        subFrameContent = {
            SubFrameContent(message, isMessageExtraAvailable, { isMessageExtraAvailable = false })
        },
    ) {
        when (message) {
            is Unsupported -> Text(
                text = message.fallbackText ?: stringResource(string.text_unsupported_message_type),
                modifier = padding,
            )

            is Text -> AutoLinkedText(
                text = message.text,
                modifier = padding,
                style = chatTypography.chatMessage,
            )

            is WithAttachments -> AttachmentMessage(
                message,
                modifier = padding,
                onAttachmentClicked = onAttachmentClicked,
                onMoreClicked = onMoreClicked,
                onShare = onShare,
            )

            is ListPicker -> ListPickerMessage(message, textColor = chatColor)
            is RichLink -> RichLinkMessage(message = message, textColor = chatColor)
            is QuickReply -> QuickReplyMessage(message, modifier = padding)
        }
    }
}

@Composable
private fun SubFrameContent(message: Message, isMessageExtraAvailable: Boolean, onClick: () -> Unit) {
    when (message) {
        is QuickReply -> QuickReplySubFrame(message, isMessageExtraAvailable, onClick)

        else -> {}
    }
}

@PreviewLightDark
@Preview(showBackground = true)
@Composable
private fun PreviewContentTextMessage() {
    ChatTheme {
        Surface {
            LazyColumn {
                item {
                    PreviewMessageItem(
                        message = Text(
                            PreviewMessageProvider.Text(
                                text = "Please send the videos! \uD83D\uDE4C\uD83C\uDFFC",
                            )
                        ),
                        showStatus = true,
                    )
                }
                item {
                    PreviewMessageItem(
                        message = Text(
                            PreviewMessageProvider.Text(
                                text = "Hi and thank you for your response. \n" +
                                        "I can send the videos, sure.",
                                direction = ToAgent
                            )
                        ),
                        itemGroupState = FIRST,
                    )
                }
                item {
                    PreviewMessageItem(
                        message = Text(
                            PreviewMessageProvider.Text(
                                text = "I’ll send four if that’s OK.",
                                direction = ToAgent
                            )
                        ),
                        itemGroupState = MIDDLE,
                    )
                }
                item {
                    PreviewMessageItem(
                        message = Text(
                            PreviewMessageProvider.Text(
                                text = "Here they come!",
                                direction = ToAgent
                            )
                        ),
                        showStatus = true,
                        itemGroupState = LAST,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun PreviewContentUnsupported() {
    PreviewMessageItemBase(
        message = Unsupported(PreviewMessageProvider.Text()),
    )
}
