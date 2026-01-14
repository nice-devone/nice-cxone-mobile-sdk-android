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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.message.MessageDirection.ToClient
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.attachments.AttachmentFramedPreview
import com.nice.cxonechat.ui.composable.conversation.attachments.AttachmentPreview
import com.nice.cxonechat.ui.composable.conversation.model.Message
import com.nice.cxonechat.ui.composable.conversation.model.Message.WithAttachments
import com.nice.cxonechat.ui.composable.generic.AudioPlayerBasicView
import com.nice.cxonechat.ui.composable.generic.ThumbnailSize
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.colorScheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.util.dw
import com.nice.cxonechat.ui.util.preview.message.UiSdkText
import java.util.UUID
import kotlin.math.max

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
    Box(
        modifier = Modifier.testTag("attachment_message")
    ) {
        when (totalCount) {
            // This really can't happen since the decision to get to AttachmentMessage
            // was predicated on attachments.count > 0
            0 -> Box(
                modifier = Modifier
                    .testTag("missing_attachments")
                    .then(modifier)
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
    CompositionLocalProvider(
        LocalContentColor provides if (attachment.direction === ToClient) {
            chatColors.token.content.primary
        } else {
            chatColors.token.brand.onPrimary
        }
    ) {
        BoxWithConstraints(
            modifier = Modifier.testTag("audio_attachment")
        ) {
            val maxAttachmentWidth = this.maxWidth.times(0.745f)
            AudioPlayerBasicView(
                uri = attachment.attachment.url.toUri(),
                modifier = Modifier
                    .testTag("audio_player")
                    .then(modifier)
                    .widthIn(min = space.smallAttachmentSize, max = maxAttachmentWidth)
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun AttachmentPreviewGroup(
    messageId: UUID,
    attachments: List<Attachment>,
    onAttachmentClicked: (Attachment) -> Unit,
    onMoreClicked: (List<Attachment>) -> Unit,
) {
    val totalCount = remember { attachments.size }
    val maxAttachmentsPreview = space.smallAttachmentRowCount * space.smallAttachmentRowSizeCount
    val onMoreAttachments: (Attachment) -> Unit = remember { { onMoreClicked(attachments) } }
    val sizeMod = Modifier.size(space.attachmentPreviewRegularWidthPercentage.dw)
    val chunks = attachments.chunked(space.smallAttachmentRowSizeCount)
    val displayOverflowBlur = totalCount > maxAttachmentsPreview
    Column(
        verticalArrangement = Arrangement.spacedBy(space.semiLarge)
    ) {
        GroupRow(
            list = chunks[0],
            messageId = messageId,
            modifier = sizeMod,
            onAttachmentClicked = onAttachmentClicked,
            onMoreAttachments = onMoreAttachments,
            totalCount = totalCount,
            maxAttachmentsPreview = maxAttachmentsPreview
        )
        if (chunks.size > 1 && chunks[1].isNotEmpty()) {
            GroupRow(
                list = chunks[1],
                messageId = messageId,
                modifier = sizeMod,
                attachmentIdStart = space.smallAttachmentRowSizeCount,
                onAttachmentClicked = onAttachmentClicked,
                onMoreAttachments = onMoreAttachments,
                displayOverflowBlur = displayOverflowBlur,
                totalCount = totalCount,
                maxAttachmentsPreview = maxAttachmentsPreview
            )
        }
    }
}

@Composable
private fun GroupRow(
    list: List<Attachment>,
    messageId: UUID,
    totalCount: Int,
    maxAttachmentsPreview: Int,
    modifier: Modifier = Modifier,
    attachmentIdStart: Int = 0,
    displayOverflowBlur: Boolean = false,
    onAttachmentClicked: (Attachment) -> Unit,
    onMoreAttachments: (Attachment) -> Unit,
) {
    Row(
        modifier = Modifier.testTag("attachment_preview_row_${attachmentIdStart / space.smallAttachmentRowSizeCount}"),
        horizontalArrangement = Arrangement.spacedBy(space.semiLarge)
    ) {
        AttachmentFramedPreview(
            attachment = list[0],
            messageId = messageId,
            modifier = modifier.testTag("attachment_preview_$attachmentIdStart"),
            thumbnailSize = ThumbnailSize.REGULAR,
            onClick = onAttachmentClicked,
            onLongClick = onMoreAttachments,
        )
        if (list.size > 1) {
            Box(contentAlignment = Alignment.Center) {
                val onClick = if (displayOverflowBlur) onMoreAttachments else onAttachmentClicked
                AttachmentFramedPreview(
                    attachment = list[1],
                    messageId = messageId,
                    modifier = modifier.testTag("attachment_preview_${attachmentIdStart + 1}"),
                    blurred = displayOverflowBlur,
                    thumbnailSize = ThumbnailSize.REGULAR,
                    onClick = onClick,
                    onLongClick = onMoreAttachments,
                )
                if (displayOverflowBlur) {
                    OverFlowText(totalCount, maxAttachmentsPreview)
                }
            }
        }
    }
}

@Composable
private fun OverFlowText(totalCount: Int, maxAttachmentsPreview: Int) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.testTag("attachment_overflow")
    ) {
        Spacer(
            modifier = Modifier
                .alpha(0.75f)
                .padding(1.dp)
                .size(space.xxl)
                .background(color = chatColors.token.brand.onPrimary, shape = CircleShape),
        )
        val overflowCount = remember(totalCount, maxAttachmentsPreview) {
            max(0, totalCount - maxAttachmentsPreview)
        }
        Text(
            text = stringResource(string.extra_attachments_count, overflowCount),
            style = chatTypography.overflowText,
            color = chatColors.token.content.primary
        )
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
    AttachmentPreview(
        attachment = attachment,
        messageId = messageId,
        modifier = Modifier
            .size(space.attachmentPreviewRegularWidthPercentage.dw)
            .testTag("attachment_preview_${attachment.url}"),
        onClick = onAttachmentClicked,
        onLongClick = remember { { attachment -> onShare(listOf(attachment)) } },
        showFrame = onShowFrame,
    )
}

@Composable
internal fun LeadingShareAttachmentsIcon(contentDescription: String? = null, onClick: () -> Unit) {
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
private fun PreviewLeadingShareIcon() {
    ChatTheme {
        LeadingShareAttachmentsIcon {}
    }
}
