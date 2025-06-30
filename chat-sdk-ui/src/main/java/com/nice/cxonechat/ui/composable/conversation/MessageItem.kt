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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.message.MessageDirection.ToAgent
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.FIRST
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.LAST
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.MIDDLE
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.SOLO
import com.nice.cxonechat.ui.composable.conversation.model.Message
import com.nice.cxonechat.ui.composable.conversation.model.Message.AudioAttachment
import com.nice.cxonechat.ui.composable.conversation.model.Message.EmojiText
import com.nice.cxonechat.ui.composable.conversation.model.Message.ListPicker
import com.nice.cxonechat.ui.composable.conversation.model.Message.QuickReply
import com.nice.cxonechat.ui.composable.conversation.model.Message.RichLink
import com.nice.cxonechat.ui.composable.conversation.model.Message.Text
import com.nice.cxonechat.ui.composable.conversation.model.Message.Unsupported
import com.nice.cxonechat.ui.composable.conversation.model.Message.WithAttachments
import com.nice.cxonechat.ui.composable.generic.AutoLinkedText
import com.nice.cxonechat.ui.composable.theme.ChatColors.ColorPair
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.TinySpacer
import com.nice.cxonechat.ui.domain.model.asPerson
import com.nice.cxonechat.ui.util.preview.message.UiSdkText

@Composable
internal fun MessageItem(
    message: Message,
    showStatus: Boolean,
    modifier: Modifier = Modifier,
    itemGroupState: MessageItemGroupState = SOLO,
    onAttachmentClicked: (Attachment) -> Unit,
    onMoreClicked: (List<Attachment>) -> Unit,
    onShare: (Collection<Attachment>) -> Unit,
) {
    val toAgent = message.direction === ToAgent
    val alignment = if (toAgent) Alignment.End else Alignment.Start
    val isEndOfGroup = itemGroupState in listOf(SOLO, LAST)
    val showAvatar = !toAgent && isEndOfGroup

    Column {
        Row(
            modifier = modifier
                .wrapContentWidth(align = alignment)
        ) {
            Column(horizontalAlignment = alignment) {
                MessageContent(
                    message = message,
                    position = itemGroupState,
                    onAttachmentClicked = onAttachmentClicked,
                    onMoreClicked = onMoreClicked,
                    onShare = onShare,
                )

                if (showStatus) {
                    MessageStatusIndicator(message.status, modifier = Modifier.padding(top = space.small))
                } else if (toAgent && isEndOfGroup) {
                    Spacer(modifier = Modifier.size(18.dp))
                }
            }
        }
        if (!showStatus && !showAvatar) {
            TinySpacer()
        }
    }
}

/**
 * Represents the state of a message regarding its position in a group of similar messages.
 */
@Immutable
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
    onMoreClicked: (List<Attachment>) -> Unit,
    onShare: (Collection<Attachment>) -> Unit,
) = Column(modifier) {
    val padding = Modifier.padding(space.messagePadding)
    val toAgent = message.direction == ToAgent
    val chatColor = BubbleColor(
        toAgent = toAgent,
        transparentBackground =
            (message as? WithAttachments)?.attachments?.toList()?.size == 1 ||
                    message is EmojiText
    )
    val avatar = remember { if (!toAgent && position in listOf(LAST, SOLO)) message.sender?.asPerson else null }
    var isMessageExtraAvailable by rememberSaveable { mutableStateOf(true) }
    var showFrame by rememberSaveable { mutableStateOf(true) }
    MessageFrame(
        position = position,
        isAgent = !toAgent,
        avatar = avatar,
        colors = chatColor,
        showFrame = showFrame,
        subFrameContent = {
            SubFrameContent(message, isMessageExtraAvailable, { isMessageExtraAvailable = false })
        },
        leadingContent = { LeadingContent(message, onShare) }
    ) {
        when (message) {
            is Unsupported -> FallbackText(message, padding)
            is Text -> TextMessage(message, padding)
            is EmojiText -> EmojiMessage(message, padding)
            is AudioAttachment -> AudioAttachment(
                attachment = message,
                modifier = Modifier.padding(space.audioMessagePadding)
            )

            is WithAttachments -> AttachmentMessage(
                message = message,
                modifier = Modifier.padding(space.attachmentMessagePadding),
                onAttachmentClicked = onAttachmentClicked,
                onMoreClicked = onMoreClicked,
                onShare = onShare,
                onShowFrame = {
                    showFrame = it
                },
            )

            is ListPicker -> ListPickerMessage(message, textColor = chatColor)
            is RichLink -> RichLinkMessage(message = message, textColor = chatColor)
            is QuickReply -> QuickReplyMessage(message, modifier = padding)
        }
    }
}

@Composable
private fun EmojiMessage(message: EmojiText, padding: Modifier) {
    Text(
        text = message.text,
        modifier = Modifier
            .testTag("emoji_message")
            .then(padding),
        style = chatTypography.chatEmojiMessage,
    )
}

@Composable
private fun TextMessage(message: Text, padding: Modifier) {
    AutoLinkedText(
        text = message.text,
        modifier = Modifier
            .testTag("text_message")
            .then(padding),
        style = chatTypography.chatMessage,
    )
}

@Composable
private fun FallbackText(message: Unsupported, padding: Modifier) {
    Text(
        text = message.fallbackText ?: stringResource(string.text_unsupported_message_type),
        modifier = Modifier
            .testTag("fallback_text")
            .then(padding),
    )
}

@Composable
private fun BubbleColor(toAgent: Boolean, transparentBackground: Boolean): ColorPair {
    val colors = if (toAgent) {
        chatColors.customer
    } else {
        chatColors.agent
    }
    return if (transparentBackground) {
        colors.copy(background = Color.Transparent)
    } else {
        colors
    }
}

@Composable
private fun LeadingContent(message: Message, onShare: (Collection<Attachment>) -> Unit) {
    if (message is Message.AttachmentsMessage) {
        val attachmentList = remember { message.attachments.toList() }
        val contentDescription = if (attachmentList.size == 1) {
            stringResource(string.share_attachment, attachmentList[0].friendlyName)
        } else {
            stringResource(string.share_attachment_others, attachmentList[0].friendlyName, attachmentList.size - 1)
        }
        LeadingShareAttachmentsIcon(contentDescription) { onShare(attachmentList) }
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
@Suppress(
    "LongMethod", // Preview
)
@Composable
private fun PreviewContentTextMessage() {
    ChatTheme {
        Surface {
            LazyColumn {
                item {
                    PreviewMessageItem(
                        message = EmojiText(
                            UiSdkText(
                                text = "😎📱🇨🇿",
                                direction = ToAgent
                            )
                        ),
                        showStatus = true,
                    )
                }
                item {
                    PreviewMessageItem(
                        message = Text(
                            UiSdkText(
                                text = "Please send the videos! \uD83D\uDE4C\uD83C\uDFFC",
                            )
                        ),
                        showStatus = false,
                    )
                }
                item {
                    PreviewMessageItem(
                        message = Text(
                            UiSdkText(
                                text = "Hi and thank you for your response. \n" +
                                        "I can send the videos, sure.",
                                direction = ToAgent
                            )
                        ),
                        itemGroupState = FIRST,
                        showStatus = false,
                    )
                }
                item {
                    PreviewMessageItem(
                        message = Text(
                            UiSdkText(
                                text = "I’ll send four if that’s OK.",
                                direction = ToAgent
                            )
                        ),
                        itemGroupState = MIDDLE,
                        showStatus = false,
                    )
                }
                item {
                    PreviewMessageItem(
                        message = Text(
                            UiSdkText(
                                text = "Here they come!",
                                direction = ToAgent
                            )
                        ),
                        itemGroupState = LAST,
                        showStatus = false,
                    )
                }
                item {
                    PreviewMessageItem(
                        message = Text(
                            UiSdkText(
                                text = "Please send the videos! \uD83D\uDE4C\uD83C\uDFFC",
                            )
                        ),
                        showStatus = false,
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
        message = Unsupported(UiSdkText()),
    )
}
