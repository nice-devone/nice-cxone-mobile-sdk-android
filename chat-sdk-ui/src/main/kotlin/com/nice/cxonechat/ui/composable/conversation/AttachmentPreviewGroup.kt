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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.attachments.AttachmentFramedPreview
import com.nice.cxonechat.ui.composable.generic.ThumbnailSize
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatTypography
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.util.dsm
import com.nice.cxonechat.ui.util.typeAndName
import java.util.UUID
import kotlin.math.max

@Composable
internal fun AttachmentPreviewGroup(
    messageId: UUID,
    attachments: List<Attachment>,
    onAttachmentClicked: (Attachment) -> Unit,
    onMoreClicked: (List<Attachment>) -> Unit,
) {
    val totalCount = attachments.size
    val maxAttachmentsPreview = space.smallAttachmentRowCount * space.smallAttachmentRowSizeCount
    val onMoreAttachments: (Attachment) -> Unit = remember(attachments, onMoreClicked) { { onMoreClicked(attachments) } }
    val itemSize = space.attachmentPreviewRegularWidthPercentage.dsm
    val sizeMod = Modifier.size(itemSize)
    val displayOverflowBlur = totalCount > maxAttachmentsPreview
    val displayedItems = if (displayOverflowBlur) attachments.subList(0, maxAttachmentsPreview) else attachments
    val maxSize = itemSize * 2 + space.semiLarge
    val columns = 2

    Column(
        modifier = Modifier
            .semantics {
                testTag = "attachment_preview_group"
                isTraversalGroup = true
            }
            .width(maxSize),
        verticalArrangement = Arrangement.spacedBy(space.semiLarge),
    ) {
        displayedItems.chunked(columns).forEachIndexed { rowIndex, rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(space.semiLarge),
            ) {
                rowItems.forEachIndexed { colIndex, attachment ->
                    val index = rowIndex * columns + colIndex
                    AttachmentPreviewItem(
                        attachment = attachment,
                        messageId = messageId,
                        index = index,
                        totalCount = totalCount,
                        sizeMod = sizeMod,
                        isLastDisplayed = index == displayedItems.lastIndex,
                        displayOverflowBlur = displayOverflowBlur,
                        maxAttachmentsPreview = maxAttachmentsPreview,
                        onClick = if (index == displayedItems.lastIndex) onMoreAttachments else onAttachmentClicked,
                        onLongClick = onMoreAttachments,
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachmentPreviewItem(
    attachment: Attachment,
    messageId: UUID,
    index: Int,
    totalCount: Int,
    sizeMod: Modifier,
    isLastDisplayed: Boolean,
    displayOverflowBlur: Boolean,
    maxAttachmentsPreview: Int,
    onClick: (Attachment) -> Unit,
    onLongClick: (Attachment) -> Unit,
) {
    Box(contentAlignment = Alignment.Center) {
        val contentDesc = stringResource(
            id = string.content_description_attachment_item,
            attachment.typeAndName,
            index + 1,
            totalCount
        )
        AttachmentFramedPreview(
            attachment = attachment,
            messageId = messageId,
            modifier = sizeMod.semantics {
                testTag = "attachment_preview_$index"
                traversalIndex = index.toFloat()
            },
            blurred = displayOverflowBlur && index >= maxAttachmentsPreview - 1,
            thumbnailSize = ThumbnailSize.REGULAR,
            contentDescriptionText = contentDesc,
            onClick = onClick,
            onClickLabel = lastItemClickLabel(displayOverflowBlur && isLastDisplayed, totalCount),
            onLongClick = onLongClick,
            onLongClickLabel = stringResource(string.content_description_attachments_sharing),
            isGroupAttachment = true,
        )
        if (isLastDisplayed && displayOverflowBlur) {
            OverflowText(totalCount, maxAttachmentsPreview)
        }
    }
}

@Composable
private fun lastItemClickLabel(displayOverflowBlur: Boolean, totalCount: Int): String =
    if (displayOverflowBlur) {
        stringResource(string.content_description_show_all_attachments, totalCount)
    } else {
        stringResource(string.content_description_attachment_detail)
    }

@Composable
private fun OverflowText(totalCount: Int, maxAttachmentsPreview: Int) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .testTag("attachment_overflow")
            .semantics(true) {
                hideFromAccessibility() // Overflow count is already included in the onClickLabel of the blurred preview
            }
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

private data class AttachmentCountProvider(
    override val values: Sequence<Int> = (0..4).asSequence(),
) : PreviewParameterProvider<Int>

@PreviewLightDark
@Composable
private fun PreviewAttachmentPreviewGroup(
    @PreviewParameter(AttachmentCountProvider::class) count: Int,
) {
    ChatTheme {
        Box(
            modifier = Modifier
                .systemBarsPadding()
                .sizeIn(maxWidth = 240.dp)
        ) {
            AttachmentPreviewGroup(
                messageId = UUID.randomUUID(),
                attachments = PreviewAttachments.with(count).toList(),
                onAttachmentClicked = {},
                onMoreClicked = {}
            )
        }
    }
}
