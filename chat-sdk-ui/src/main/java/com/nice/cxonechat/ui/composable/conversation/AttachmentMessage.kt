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

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons.Outlined
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.Preview
import com.nice.cxonechat.message.MessageDirection.ToClient
import com.nice.cxonechat.ui.composable.conversation.model.Message.Attachment
import com.nice.cxonechat.ui.composable.generic.AudioPlayer
import com.nice.cxonechat.ui.composable.generic.PresetAsyncImage
import com.nice.cxonechat.ui.composable.generic.VideoPlayer
import com.nice.cxonechat.ui.composable.generic.forwardingPainter
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatShapes
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.LocalSpace
import com.nice.cxonechat.message.Attachment as SdkAttachment

@Composable
internal fun AttachmentMessage(message: Attachment, modifier: Modifier) {
    val mimeType = message.mimeType
    when {
        mimeType == null -> PlaceholderContent(
            message = message,
            placeholder = rememberVectorPainter(image = Outlined.ErrorOutline),
            modifier = modifier,
            colorFilter = ColorFilter.tint(ChatTheme.colors.error),
        )

        mimeType.startsWith("video/") -> VideoAttachmentContent(message, modifier)
        mimeType.startsWith("image/") -> ImageAttachmentContent(message, modifier)
        mimeType.startsWith("audio/") -> AudioAttachmentContent(message, modifier)
        else -> PlaceholderContent(message, rememberVectorPainter(image = Outlined.Description), modifier)
    }
}

@Composable
private fun AttachmentContentDescription(message: Attachment, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (message.name.isNotBlank()) {
            Text(
                text = message.name,
                style = ChatTheme.chatTypography.chatAttachmentCaption,
            )
        }
        if (message.text.isNotBlank()) {
            Text(
                text = message.text,
                style = ChatTheme.chatTypography.chatAttachmentMessage,
            )
        }
    }
}

@Composable
private fun PlaceholderContent(
    message: Attachment,
    placeholder: Painter,
    modifier: Modifier = Modifier,
    colorFilter: ColorFilter = ColorFilter.tint(LocalContentColor.current),
) {
    val placeholderPainter = forwardingPainter(
        painter = placeholder,
        colorFilter = colorFilter
    )
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = placeholderPainter,
            contentDescription = message.name,
            modifier = Modifier.size(LocalSpace.current.clickableSize)
        )
        AttachmentContentDescription(message)
    }
}

@Composable
private fun ImageAttachmentContent(
    message: Attachment,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PresetAsyncImage(
            model = message.originalUrl.ifBlank { null },
            contentDescription = message.name,
            modifier = Modifier
                .defaultMinSize(LocalSpace.current.clickableSize, LocalSpace.current.clickableSize)
                .clip(RoundedCornerShape(space.large)),
        )
        AttachmentContentDescription(message = message)
    }
}

@Composable
private fun VideoAttachmentContent(
    message: Attachment,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        VideoPlayer(uri = Uri.parse(message.originalUrl), modifier = Modifier.clip(chatShapes.chatVideoPlayer))
        AttachmentContentDescription(message = message, Modifier.padding(top = space.small))
    }
}

@Composable
private fun AudioAttachmentContent(message: Attachment, modifier: Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AudioPlayer(uri = Uri.parse(message.originalUrl), modifier = Modifier.clip(chatShapes.chatAudioPlayer))
        AttachmentContentDescription(message = message, Modifier.padding(top = space.small))
    }
}

@Preview
@Composable
private fun PreviewPlaceholder() {
    ChatTheme {
        Surface {
            AttachmentMessage(
                message = Attachment(
                    message = previewTextMessage("Example document", direction = ToClient),
                    attachment = object : SdkAttachment {
                        override val url: String = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf"
                        override val friendlyName: String = "dummy.pdf"
                        override val mimeType: String? = null
                    }
                ),
                modifier = Modifier
            )
        }
    }
}

@Preview
@Composable
private fun PreviewContentAttachmentImage() {
    PreviewMessageItemBase {
        MessageItem(
            message = Attachment(
                message = previewTextMessage("Preview image"),
                attachment = object : SdkAttachment {
                    override val url: String = "https://http.cat/203"
                    override val friendlyName: String = "cat_no_content.jpeg"
                    override val mimeType: String = "image/jpeg"
                }
            ),
        )
    }
}

@Preview
@Composable
private fun PreviewMessageAttachmentVideo() {
    PreviewMessageItemBase {
        MessageItem(
            message = Attachment(
                message = previewTextMessage("Preview video", direction = ToClient),
                attachment = object : SdkAttachment {
                    override val url: String = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                    override val friendlyName: String = "example.webm"
                    override val mimeType: String = "video/mp4"
                }
            ),
        )
    }
}

@Preview
@Composable
private fun PreviewMessageAttachmentPdf() {
    PreviewMessageItemBase {
        MessageItem(
            message = Attachment(
                message = previewTextMessage("Example document", direction = ToClient),
                attachment = object : SdkAttachment {
                    override val url: String = "https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf"
                    override val friendlyName: String = "dummy.pdf"
                    override val mimeType: String = "application/pdf"
                }
            ),
        )
    }
}
