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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ContextualFlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
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
import com.nice.cxonechat.ui.util.preview.message.UiSdkText
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
    BoxWithConstraints(
        modifier = Modifier.testTag("attachment_message")
    ) {
        val maxAttachmentWidth = this.maxWidth.times(0.6f)
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
                maxAttachmentWidth = maxAttachmentWidth,
                attachment = attachments[0],
                onShowFrame = onShowFrame,
                onAttachmentClicked = onAttachmentClicked,
                onShare = onShare
            )

            else -> Box(
                modifier = modifier.padding(0.dp), // We want to completely fill MessageFrame
            ) {
                AttachmentPreviewGroup(
                    maxAttachmentWidth = maxAttachmentWidth,
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
            chatColors.agent.foreground
        } else {
            chatColors.customer.foreground
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
    maxAttachmentWidth: Dp,
    attachments: List<Attachment>,
    onAttachmentClicked: (Attachment) -> Unit,
    onMoreClicked: (List<Attachment>) -> Unit,
) {
    val totalCount = remember { attachments.size }
    val maxAttachmentsPreview = space.smallAttachmentRowCount * space.smallAttachmentRowSizeCount
    val widthDiv = minOf(totalCount, space.smallAttachmentRowSizeCount)
    val onMoreAttachments: (Attachment) -> Unit = remember { { onMoreClicked(attachments) } }
    ContextualFlowRow(
        modifier = Modifier
            .wrapContentHeight(align = Alignment.Top)
            .testTag("attachment_preview_group"),
        horizontalArrangement = Arrangement.spacedBy(space.semiLarge),
        verticalArrangement = Arrangement.spacedBy(space.semiLarge),
        maxLines = space.smallAttachmentRowCount,
        maxItemsInEachRow = space.smallAttachmentRowSizeCount,
        itemCount = totalCount
    ) { index ->
        val sizeMod = Modifier.size(minOf(maxAttachmentWidth, maxWidthInLine).div(widthDiv))
        val attachment = attachments[index]
        Box(contentAlignment = Alignment.Center) {
            val displayOverflowBlur = index >= maxAttachmentsPreview - 1 && totalCount > maxAttachmentsPreview
            AttachmentFramedPreview(
                attachment = attachment,
                modifier = sizeMod.testTag("attachment_preview_$index"),
                blurred = displayOverflowBlur,
                thumbnailSize = ThumbnailSize.REGULAR,
                onClick = if (displayOverflowBlur) onMoreAttachments else onAttachmentClicked,
                onLongClick = onMoreAttachments,
            )
            if (displayOverflowBlur) {
                OverFlowText(totalCount, maxAttachmentsPreview)
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
                .background(color = Color.White, shape = CircleShape),
        )
        val overflowCount = remember(totalCount, maxAttachmentsPreview) {
            max(0, totalCount - maxAttachmentsPreview)
        }
        Text(
            text = stringResource(string.extra_attachments_count, overflowCount),
            style = chatTypography.overflowText
        )
    }
}

@Composable
private fun SingleAttachmentPreview(
    maxAttachmentWidth: Dp,
    attachment: Attachment,
    onShowFrame: (Boolean) -> Unit,
    onAttachmentClicked: (Attachment) -> Unit,
    onShare: (Collection<Attachment>) -> Unit,
) {
    AttachmentPreview(
        attachment = attachment,
        modifier = Modifier
            .widthIn(min = space.smallAttachmentSize, max = maxAttachmentWidth)
            .heightIn(min = space.smallAttachmentSize)
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
            containerColor = chatColors.leadingMessageIconContainer,
        ),
        border = BorderStroke(1.dp, chatColors.leadingMessageIconBorder),
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
