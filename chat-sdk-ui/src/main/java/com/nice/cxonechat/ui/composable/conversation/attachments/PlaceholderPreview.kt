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

package com.nice.cxonechat.ui.composable.conversation.attachments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.ui.composable.conversation.PreviewAttachments
import com.nice.cxonechat.ui.composable.generic.ThumbnailSize
import com.nice.cxonechat.ui.composable.icons.ChatIcons
import com.nice.cxonechat.ui.composable.icons.notint.Document
import com.nice.cxonechat.ui.composable.icons.notint.DocumentLarge
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.colorScheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.contentColorFor
import com.nice.cxonechat.ui.util.contentDescription

@Composable
internal fun PlaceholderPreview(
    attachment: Attachment,
    modifier: Modifier = Modifier,
    thumbnailSize: ThumbnailSize = ThumbnailSize.REGULAR,
) {
    val iconMod = if (thumbnailSize == ThumbnailSize.LARGE) {
        modifier.sizeIn(maxWidth = 126.dp, maxHeight = 164.dp)
    } else {
        modifier
    }
    val imageVector = when (thumbnailSize) {
        ThumbnailSize.LARGE -> ChatIcons.DocumentLarge
        else -> ChatIcons.Document
    }
    Icon(
        imageVector = imageVector,
        contentDescription = attachment.contentDescription,
        modifier = iconMod,
        tint = contentColorFor(colorScheme.primary)
    )
}

@PreviewLightDark
@Composable
private fun Preview() {
    ChatTheme {
        Surface(Modifier.systemBarsPadding()) {
            Column(verticalArrangement = Arrangement.spacedBy(space.medium)) {
                PlaceholderPreview(
                    attachment = PreviewAttachments.evil,
                    thumbnailSize = ThumbnailSize.LARGE
                )
                Text("LARGE")
                PlaceholderPreview(
                    attachment = PreviewAttachments.evil,
                    thumbnailSize = ThumbnailSize.REGULAR
                )
                Text("REGULAR")
            }
        }
    }
}
