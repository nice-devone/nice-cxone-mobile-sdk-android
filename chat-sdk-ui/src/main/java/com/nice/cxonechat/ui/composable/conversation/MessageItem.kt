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
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.message.MessageDirection.ToAgent
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.FIRST
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.LAST
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.MIDDLE
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState.SOLO
import com.nice.cxonechat.ui.composable.conversation.MessageStatusState.SELECTABLE
import com.nice.cxonechat.ui.composable.conversation.model.Message
import com.nice.cxonechat.ui.composable.conversation.model.Message.AttachmentsMessage
import com.nice.cxonechat.ui.composable.conversation.model.Message.AudioAttachment
import com.nice.cxonechat.ui.composable.conversation.model.Message.EmojiText
import com.nice.cxonechat.ui.composable.conversation.model.Message.ListPicker
import com.nice.cxonechat.ui.composable.conversation.model.Message.QuickReply
import com.nice.cxonechat.ui.composable.conversation.model.Message.RichLink
import com.nice.cxonechat.ui.composable.conversation.model.Message.Text
import com.nice.cxonechat.ui.composable.conversation.model.Message.Unsupported
import com.nice.cxonechat.ui.composable.conversation.model.Message.WithAttachments
import com.nice.cxonechat.ui.composable.theme.ChatColors.ColorPair
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.domain.model.Person
import com.nice.cxonechat.ui.domain.model.asPerson
import com.nice.cxonechat.ui.util.preview.message.UiSdkQuickReply
import com.nice.cxonechat.ui.util.preview.message.UiSdkText
import com.nice.cxonechat.ui.util.preview.message.UiSdkUnsupportedMessage

@Composable
internal fun MessageItem(
    message: Message,
    showStatus: DisplayStatus,
    messageStatusState: MessageStatusState,
    onQuickReplyOptionSelected: (Boolean) -> Unit,
    onListPickerSelected: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    itemGroupState: MessageItemGroupState = SOLO,
    onAttachmentClicked: (Attachment) -> Unit,
    onMoreClicked: (List<Attachment>) -> Unit,
    onShare: (Collection<Attachment>) -> Unit,
    snackBarHostState: SnackbarHostState,
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
                    messageStatusState = messageStatusState,
                    onQuickReplyOptionSelected = onQuickReplyOptionSelected,
                    onListPickerSelected = onListPickerSelected,
                    onAttachmentClicked = onAttachmentClicked,
                    onMoreClicked = onMoreClicked,
                    onShare = onShare,
                    snackBarHostState = snackBarHostState
                )

                when (showStatus) {
                    DisplayStatus.DISPLAY -> MessageStatusIndicator(message.status, modifier = Modifier.padding(top = space.small))
                    DisplayStatus.SPACER -> Spacer(modifier = Modifier.size(18.dp))
                    DisplayStatus.HIDE -> {
                        /* Do nothing */
                    }
                }
            }
        }
        if (showStatus !== DisplayStatus.DISPLAY && !showAvatar) {
            Spacer(modifier = Modifier.size(space.xSmall))
        }
    }
}

internal enum class DisplayStatus {
    DISPLAY,
    SPACER,
    HIDE,
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

    /** Item is last in the given group, but the previous should be displayed without extra spacing. */
    LAST_SQUASHED,

    /** Item is not part of a group. */
    SOLO,

    /* Special case for text surrounded by emoji which are grouped but should be displayed as solo. */
    SOLO_GROUPED
}

@Suppress("LongMethod")
@Composable
private fun MessageContent(
    message: Message,
    position: MessageItemGroupState,
    modifier: Modifier = Modifier,
    messageStatusState: MessageStatusState,
    onQuickReplyOptionSelected: (Boolean) -> Unit,
    onListPickerSelected: (Boolean) -> Unit,
    onAttachmentClicked: (Attachment) -> Unit,
    onMoreClicked: (List<Attachment>) -> Unit,
    onShare: (Collection<Attachment>) -> Unit,
    snackBarHostState: SnackbarHostState,
) = Column(modifier) {
    val padding = Modifier.padding(space.messagePadding)
    val toAgent = message.direction == ToAgent
    val chatColor = bubbleColor(
        toAgent = toAgent,
        transparentBackground =
            (message as? WithAttachments)?.attachments?.toList()?.size == 1 ||
                    message is EmojiText
    )
    val avatar = remember { if (!toAgent && position in listOf(LAST, SOLO)) message.sender?.asPerson else null }
    var showFrame by rememberSaveable { mutableStateOf(true) }
    var showListPickerDialog by rememberSaveable { mutableStateOf(false) }

    MessageFrameContent(
        message = message,
        position = position,
        chatColor = chatColor,
        avatar = avatar,
        showFrame = showFrame,
        setShowFrame = { showFrame = it },
        modifier = padding,
        messageStatusState = messageStatusState,
        onQuickReplyOptionSelected = onQuickReplyOptionSelected,
        onListPickerSelected = onListPickerSelected,
        onAttachmentClicked = onAttachmentClicked,
        onMoreClicked = onMoreClicked,
        onShare = onShare,
        snackBarHostState = snackBarHostState,
        showListPickerDialog = showListPickerDialog,
        setShowListPickerDialog = { showListPickerDialog = it },
    )
}

@Composable
private fun MessageContentBody(
    message: Message,
    modifier: Modifier,
    chatColor: ColorPair,
    onAttachmentClicked: (Attachment) -> Unit,
    onMoreClicked: (List<Attachment>) -> Unit,
    onShare: (Collection<Attachment>) -> Unit,
    onListPickerSelected: (Boolean) -> Unit,
    messageStatusState: MessageStatusState,
    setShowFrame: (Boolean) -> Unit
) {
    when (message) {
        is Unsupported -> FallbackText(message, Modifier.padding(space.unsupportedMessagePadding))
        is Text -> TextMessage(message, modifier)
        is EmojiText -> EmojiMessage(message, space.emojiPadding)
        is AudioAttachment -> AudioAttachment(message, Modifier.padding(space.audioMessagePadding))
        is WithAttachments -> AttachmentMessage(
            message = message,
            modifier = Modifier.padding(space.attachmentMessagePadding),
            onAttachmentClicked = onAttachmentClicked,
            onMoreClicked = onMoreClicked,
            onShare = onShare,
            onShowFrame = setShowFrame
        )
        is ListPicker -> ListPickerMessage(
            message,
            onMessageClick = { if (messageStatusState == SELECTABLE) onListPickerSelected(true) }
        )
        is RichLink -> RichLinkMessage(message, chatColor)
        is QuickReply -> QuickReplyMessage(message, Modifier.padding(space.quickReplyMessagePadding))
    }
}

@Composable
private fun MessageFrameContent(
    message: Message,
    position: MessageItemGroupState,
    chatColor: ColorPair,
    avatar: Person?,
    showFrame: Boolean,
    setShowFrame: (Boolean) -> Unit,
    modifier: Modifier,
    messageStatusState: MessageStatusState,
    onQuickReplyOptionSelected: (Boolean) -> Unit,
    onListPickerSelected: (Boolean) -> Unit,
    onAttachmentClicked: (Attachment) -> Unit,
    onMoreClicked: (List<Attachment>) -> Unit,
    onShare: (Collection<Attachment>) -> Unit,
    snackBarHostState: SnackbarHostState,
    showListPickerDialog: Boolean,
    setShowListPickerDialog: (Boolean) -> Unit
) {
    MessageFrame(
        position = position,
        messageContentType = message.contentType,
        isAgent = message.direction != ToAgent,
        avatar = avatar,
        colors = chatColor,
        showFrame = showFrame,
        subFrameContent = {
            SubFrameContent(
                message = message,
                messageStatusState = messageStatusState,
                onClick = {
                    if (message.contentType == ContentType.QuickReply) onQuickReplyOptionSelected(false)
                }
            )
        },
        messageStatusContent = {
            MessageStatusContentHandler(
                message = message,
                messageStatusState = messageStatusState,
                snackBarHostState = snackBarHostState,
                setShowListPickerDialog = setShowListPickerDialog
            )
        },
        leadingContent = { LeadingContent(message, onShare) }
    ) {
        MessageContentBody(
            message = message,
            modifier = modifier,
            chatColor = chatColor,
            onAttachmentClicked = onAttachmentClicked,
            onMoreClicked = onMoreClicked,
            onShare = onShare,
            onListPickerSelected = setShowListPickerDialog,
            messageStatusState = messageStatusState,
            setShowFrame = setShowFrame
        )
    }

    if (showListPickerDialog && messageStatusState == SELECTABLE) {
        ListPickerBottomSheet(
            message = message as ListPicker,
            onDismiss = { setShowListPickerDialog(false) },
            onDone = {
                onListPickerSelected(false)
                setShowListPickerDialog(false)
            },
        )
    }
}

@Composable
private fun bubbleColor(toAgent: Boolean, transparentBackground: Boolean): ColorPair {
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
    if (message is AttachmentsMessage) {
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
private fun SubFrameContent(message: Message, messageStatusState: MessageStatusState, onClick: () -> Unit) {
    when {
        message is QuickReply && messageStatusState == SELECTABLE ->
            QuickReplyOptionSubFrame(
                message = message,
                onClick = onClick
            )
    }
}

@PreviewLightDark
@Suppress(
    "LongMethod", // Preview
)
@Composable
private fun PreviewContentTextMessage() {
    ChatTheme {
        LazyColumn {
            item {
                PreviewMessageItem(
                    message = EmojiText(
                        UiSdkText(
                            text = "😎📱🇨🇿",
                            direction = ToAgent
                        )
                    ),
                    showStatus = DisplayStatus.DISPLAY,
                )
            }
            item {
                PreviewMessageItem(
                    message = Text(
                        UiSdkText(
                            text = "Please send the videos! \uD83D\uDE4C\uD83C\uDFFC",
                        )
                    ),
                    showStatus = DisplayStatus.HIDE,
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
                    showStatus = DisplayStatus.HIDE,
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
                    showStatus = DisplayStatus.HIDE,
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
                    showStatus = DisplayStatus.HIDE,
                )
            }
            item {
                PreviewMessageItem(
                    message = Text(
                        UiSdkText(
                            text = "Please send the videos! \uD83D\uDE4C\uD83C\uDFFC",
                        )
                    ),
                    showStatus = DisplayStatus.HIDE,
                )
            }
            item {
                PreviewMessageItem(
                    message = QuickReply(UiSdkQuickReply()) {},
                )
            }
            item {
                PreviewMessageItem(
                    Unsupported(UiSdkUnsupportedMessage()),
                )
            }
        }
    }
}
