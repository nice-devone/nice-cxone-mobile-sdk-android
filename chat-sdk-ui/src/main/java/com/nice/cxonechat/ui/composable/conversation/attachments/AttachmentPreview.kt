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

package com.nice.cxonechat.ui.composable.conversation.attachments

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.ui.composable.conversation.AttachmentProvider
import com.nice.cxonechat.ui.composable.conversation.attachments.PlayIndicator.HIDDEN
import com.nice.cxonechat.ui.composable.conversation.attachments.PlayIndicator.SMALL
import com.nice.cxonechat.ui.composable.conversation.attachments.PlayIndicator.STANDARD
import com.nice.cxonechat.ui.composable.generic.FallbackThumbnail
import com.nice.cxonechat.ui.composable.generic.ThumbnailSize
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatShapes
import com.nice.cxonechat.ui.composable.theme.DefaultColors.staticDark
import com.nice.cxonechat.ui.composable.theme.SelectionFrame
import com.nice.cxonechat.ui.composable.theme.ShapedFrame

/**
 * Attachment preview with a frame indicating selection.
 *
 * @param attachment Attachment to preview.
 * @param modifier Modifier.
 * @param blurred Should the preview be blurred.
 * @param selectionFrame Should the preview be framed.
 * @param selectionCircle Should the selection indicator be visible, by default it is turned on with [selectionFrame].
 * @param selected Is the preview selected.
 * Selected state is only visible iff [selectionCircle] is true.
 * @param thumbnailSize Size of the thumbnail in case of fallback.
 * @param onClick Click action.
 * @param onLongClick Long click action.
 */
@Composable
internal fun AttachmentFramedPreview(
    attachment: Attachment,
    modifier: Modifier = Modifier,
    blurred: Boolean = false,
    selectionFrame: Boolean = false,
    selectionCircle: Boolean = selectionFrame,
    selected: Boolean = false,
    thumbnailSize: ThumbnailSize = ThumbnailSize.LARGE,
    onClick: (Attachment) -> Unit,
    onLongClick: (Attachment) -> Unit,
) {
    ChatTheme.SelectionFrame(
        modifier = modifier
            .combinedClickable(
                onClick = { onClick(attachment) },
                onLongClick = { onLongClick(attachment) }
            ),
        framed = selectionFrame,
        selectionCircle = selectionCircle,
        selected = selected,
    ) {
        if (blurred) {
            Spacer(
                Modifier
                    .fillMaxSize()
                    .alpha(0.5f)
                    .background(color = staticDark)
            )
        }
        AttachmentPreview(
            attachment = attachment,
            modifier = Modifier.run {
                if (blurred) blur(4.dp, edgeTreatment = BlurredEdgeTreatment(chatShapes.selectionFrame)) else this
            },
            playIndicator = if (blurred) HIDDEN else STANDARD,
            thumbnailSize = thumbnailSize,
        )
    }
}

@Composable
internal fun AttachmentFramedSmallPreview(
    attachment: Attachment,
    modifier: Modifier = Modifier,
) {
    ChatTheme.ShapedFrame(
        modifier = modifier,
        framed = true,
        shape = chatShapes.smallSelectionFrame,
        content = { AttachmentPreview(attachment = attachment, playIndicator = SMALL, thumbnailSize = ThumbnailSize.SMALL) },
    )
}

@Composable
internal fun AttachmentPreview(
    attachment: Attachment,
    modifier: Modifier = Modifier,
    playIndicator: PlayIndicator = STANDARD,
    thumbnailSize: ThumbnailSize = ThumbnailSize.LARGE,
    showFrame: (Boolean) -> Unit = {},
) {
    val mimeType = attachment.mimeType

    when {
        mimeType == null -> PlaceholderPreview(attachment, modifier, thumbnailSize)
        mimeType.startsWith("image/") -> ImagePreview(attachment, modifier)
        mimeType.startsWith("video/") -> VideoPreview(attachment, playIndicator, modifier)
        mimeType.startsWith("audio/") -> AudioPreview(attachment, modifier)
        mimeType.startsWith("application/pdf", ignoreCase = true) -> DocumentPreview(attachment, modifier, thumbnailSize, showFrame)
        else -> {
            showFrame(false)
            FallbackThumbnail(attachment.url, attachment.mimeType, modifier, thumbnailSize)
        }
    }
}

/**
 * Clickable attachment preview.
 *
 * @param attachment Attachment to preview.
 * @param modifier Modifier.
 * @param onClick Click action.
 * @param onLongClick Long click action.
 * @param showFrame Should the preview be framed/clipped.
 */
@Composable
internal fun AttachmentPreview(
    attachment: Attachment,
    modifier: Modifier = Modifier,
    onClick: (Attachment) -> Unit,
    onLongClick: (Attachment) -> Unit,
    showFrame: (Boolean) -> Unit,
) {
    // Audio attachments have direct interaction only.
    val singleAttachmentMod = if (!attachment.mimeType.orEmpty().startsWith("audio/")) {
        modifier.combinedClickable(
            onClick = { onClick(attachment) },
            onLongClick = { onLongClick(attachment) }
        )
    } else if (attachment.mimeType.orEmpty().startsWith("application/")) {
        modifier.padding(4.dp)
    } else {
        modifier
    }
    AttachmentPreview(attachment = attachment, modifier = singleAttachmentMod, showFrame = showFrame)
}

@PreviewLightDark
@Composable
private fun PreviewAttachmentIcon(
    @PreviewParameter(AttachmentProvider::class) attachment: Attachment,
) {
    ChatTheme {
        Surface {
            AttachmentFramedPreview(
                attachment = attachment,
                modifier = Modifier.size(125.dp),
                thumbnailSize = ThumbnailSize.REGULAR,
                onClick = {},
                onLongClick = {}
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PreviewAttachmentActionIcon(
    @PreviewParameter(AttachmentProvider::class) attachment: Attachment,
) {
    ChatTheme {
        Surface {
            AttachmentFramedSmallPreview(
                attachment = attachment,
                modifier = Modifier.size(125.dp),
            )
        }
    }
}
