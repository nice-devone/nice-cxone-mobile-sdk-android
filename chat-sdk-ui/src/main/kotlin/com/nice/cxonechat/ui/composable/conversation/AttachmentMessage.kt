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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.message.MessageDirection.ToClient
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.attachments.AttachmentPreview
import com.nice.cxonechat.ui.composable.conversation.model.Message
import com.nice.cxonechat.ui.composable.conversation.model.Message.WithAttachments
import com.nice.cxonechat.ui.composable.conversation.model.getMessageAccessibility
import com.nice.cxonechat.ui.composable.generic.AudioPlayerBasicView
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.colorScheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.util.dsm
import com.nice.cxonechat.ui.util.mimeTypeToDescription
import com.nice.cxonechat.ui.util.preview.message.UiSdkText
import java.util.UUID

@Composable
internal fun AttachmentMessage(
    message: WithAttachments,
    modifier: Modifier,
    onShowFrame: (Boolean) -> Unit,
    onAttachmentClicked: (Attachment) -> Unit,
    onMoreClicked: (List<Attachment>) -> Unit,
    onShare: (Collection<Attachment>) -> Unit,
) {
    val attachments = remember { message.attachments.toList() }
    val totalCount = attachments.size
    val overrideText = when (totalCount) {
        1 -> stringResource(string.content_description_attachment_single, attachments[0].mimeTypeToDescription())
        else -> stringResource(string.content_description_attachment_message, totalCount)
    }
    val accessibilityDescription = message.getMessageAccessibility(
        overrideText = overrideText
    )
    Box(
        modifier = Modifier
            .semantics {
                testTag = "attachment_message"
                isTraversalGroup = true
            }
    ) {
        // Overlay providing the message-level accessibility description, read first via traversalIndex.
        // Separate from the outer Box to avoid mergeDescendants, which would collapse child
        // attachment nodes into a single accessibility element and break individual navigation.
        // focusable(true) is required — without it TalkBack does not discover this node.
        Box(
            modifier = Modifier
                .matchParentSize()
                .focusable(true)
                .semantics {
                    testTag = "attachment_message_description"
                    contentDescription = accessibilityDescription
                    traversalIndex = -1f
                }
        )
        when (totalCount) {
            // This really can't happen since the decision to get to AttachmentMessage
            // was predicated on attachments.count > 0
            0 -> Box(
                modifier = modifier.testTag("missing_attachments")
            ) {
                Text(
                    text = stringResource(string.error_missing_attachments),
                    style = chatTypography.chatMessage,
                )
            }

            1 -> SingleAttachmentPreview(
                messageId = message.id,
                attachment = attachments[0],
                onShowFrame = onShowFrame,
                onAttachmentClicked = onAttachmentClicked,
                onShare = onShare
            )

            else -> Box(
                modifier = modifier.padding(0.dp), // We want to completely fill MessageFrame
            ) {
                AttachmentPreviewGroup(
                    messageId = message.id,
                    attachments = attachments,
                    onAttachmentClicked = onAttachmentClicked,
                    onMoreClicked = onMoreClicked
                )
            }
        }
    }
}

@Composable
internal fun AudioAttachment(
    attachment: Message.AudioAttachment,
    modifier: Modifier = Modifier,
) {
    val accessibilityDescription = attachment.getMessageAccessibility(
        overrideText = stringResource(string.accessibility_audio_message)
    )
    CompositionLocalProvider(
        LocalContentColor provides if (attachment.direction === ToClient) {
            chatColors.token.content.primary
        } else {
            chatColors.token.brand.onPrimary
        }
    ) {
        BoxWithConstraints(
            modifier = Modifier.semantics {
                testTag = "audio_attachment"
                isTraversalGroup = true
            }
        ) {
            // Overlay for TalkBack description — same pattern as AttachmentMessage.
            // focusable(true) is required for TalkBack to discover this node.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .focusable(true)
                    .semantics {
                        testTag = "audio_attachment_description"
                        contentDescription = accessibilityDescription
                        traversalIndex = -1f
                    }
            )
            val maxAttachmentWidth = this.maxWidth.times(0.745f)
            AudioPlayerBasicView(
                uri = attachment.attachment.url.toUri(),
                modifier = modifier
                    .testTag("audio_player")
                    .widthIn(min = space.smallAttachmentSize, max = maxAttachmentWidth)
            )
        }
    }
}

@Composable
private fun SingleAttachmentPreview(
    messageId: UUID,
    attachment: Attachment,
    onShowFrame: (Boolean) -> Unit,
    onAttachmentClicked: (Attachment) -> Unit,
    onShare: (Collection<Attachment>) -> Unit,
) {
    val contentDescriptionText = attachment.mimeTypeToDescription()
    AttachmentPreview(
        attachment = attachment,
        messageId = messageId,
        modifier = Modifier
            .size(space.attachmentPreviewRegularWidthPercentage.dsm)
            .semantics(true) {
                testTag = "attachment_preview_${attachment.url}"
                contentDescription = contentDescriptionText
            },
        onClick = onAttachmentClicked,
        onClickLabel = stringResource(string.content_description_attachment_detail),
        onLongClick = remember { { attachment -> onShare(listOf(attachment)) } },
        onLongClickLabel = stringResource(string.share_attachment_selected),
        showFrame = onShowFrame,
        isGroupAttachment = false
    )
}

@Composable
internal fun ShareAttachmentsIcon(contentDescription: String? = null, onClick: () -> Unit) {
    OutlinedIconButton(
        onClick = onClick,
        shape = CircleShape,
        colors = IconButtonDefaults.outlinedIconButtonColors(
            contentColor = colorScheme.primary,
            containerColor = chatColors.token.background.surface.subtle,
        ),
        border = BorderStroke(1.dp, chatColors.token.border.default),
        modifier = Modifier
            .size(34.dp)
            .testTag("share_icon_button"),
    ) {
        ShareIcon(contentDescription)
    }
}

@Composable
internal fun ShareIcon(contentDescription: String? = null) {
    Icon(
        painter = painterResource(R.drawable.ic_share),
        contentDescription = contentDescription,
    )
}

private data class CountProvider(
    override val values: Sequence<Int> = (0..5).asSequence(),
) : PreviewParameterProvider<Int>

@PreviewLightDark
@Composable
private fun PreviewAttachmentMessage(
    @PreviewParameter(CountProvider::class) count: Int,
) {
    val attachments = PreviewAttachments.with(count)
    val message = WithAttachments(
        message = UiSdkText(
            "Preview video",
            direction = ToClient,
            attachments = attachments
        ),
        attachments = attachments
    )

    PreviewMessageItemBase(
        message = message,
    )
}

@Composable
@PreviewLightDark
private fun PreviewShareIcon() {
    ChatTheme {
        ShareAttachmentsIcon {}
    }
}
