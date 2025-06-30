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

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.ui.composable.conversation.PreviewAttachments
import com.nice.cxonechat.ui.composable.generic.ThumbnailSize
import com.nice.cxonechat.ui.composable.generic.forwardingPainter
import com.nice.cxonechat.ui.composable.icons.ChatIcons
import com.nice.cxonechat.ui.composable.icons.notint.Document
import com.nice.cxonechat.ui.composable.icons.notint.DocumentLarge
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.util.contentDescription

@Composable
internal fun PlaceholderPreview(
    attachment: Attachment,
    modifier: Modifier = Modifier,
    thumbnailSize: ThumbnailSize = ThumbnailSize.REGULAR,
) {
    Image(
        painter = forwardingPainter(
            rememberVectorPainter(
                image = when (thumbnailSize) {
                    ThumbnailSize.LARGE -> ChatIcons.DocumentLarge
                    else -> ChatIcons.Document
                }
            ),
        ),
        contentDescription = attachment.contentDescription,
        modifier = modifier
    )
}

@PreviewLightDark
@Composable
private fun Preview() {
    ChatTheme {
        Surface {
            PlaceholderPreview(
                attachment = PreviewAttachments.pdf,
                modifier = Modifier.size(100.dp),
                thumbnailSize = ThumbnailSize.REGULAR
            )
        }
    }
}
