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

import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.media3.common.util.UnstableApi
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.PreviewAttachments
import com.nice.cxonechat.ui.composable.conversation.attachments.PlayIndicator.HIDDEN
import com.nice.cxonechat.ui.composable.conversation.attachments.PlayIndicator.SMALL
import com.nice.cxonechat.ui.composable.conversation.attachments.PlayIndicator.STANDARD
import com.nice.cxonechat.ui.composable.icons.ChatIcons
import com.nice.cxonechat.ui.composable.icons.filled.PlayCircle
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.util.contentDescription

@Composable
@OptIn(UnstableApi::class)
internal fun VideoPreview(
    attachment: Attachment,
    playIndicator: PlayIndicator,
    modifier: Modifier = Modifier,
) {
    val imageLoader = ImageLoader.Builder(LocalContext.current)
        .components {
            add(VideoFrameDecoder.Factory())
        }
        .crossfade(true)
        .build()
    Box(
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            imageLoader = imageLoader,
            model = attachment.url,
            contentDescription = attachment.contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier.fillMaxHeight(),
        )
        PlayIcon(playIndicator = playIndicator)
    }
}

internal enum class PlayIndicator {
    SMALL,
    STANDARD,
    HIDDEN,
}

@Composable
internal fun PlayIcon(modifier: Modifier = Modifier, playIndicator: PlayIndicator = STANDARD) {
    val finalModifier = when (playIndicator) {
        HIDDEN -> return
        SMALL -> modifier.size(space.playVideoIconSmallSize)
        STANDARD -> modifier.size(space.playVideoIconSize)
    }
    Image(
        imageVector = ChatIcons.PlayCircle,
        contentDescription = stringResource(string.content_description_play_video),
        modifier = finalModifier,
    )
}

@Preview
@Composable
private fun PreviewVideoPreview() {
    ChatTheme {
        Surface {
            VideoPreview(PreviewAttachments.movie, playIndicator = STANDARD)
        }
    }
}
