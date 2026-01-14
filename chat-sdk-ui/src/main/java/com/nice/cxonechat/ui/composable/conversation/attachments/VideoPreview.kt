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

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.DpSize
import androidx.media3.common.util.UnstableApi
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.conversation.ContentType
import com.nice.cxonechat.ui.composable.conversation.MessageFrame
import com.nice.cxonechat.ui.composable.conversation.MessageItemGroupState
import com.nice.cxonechat.ui.composable.conversation.PreviewAttachments
import com.nice.cxonechat.ui.composable.conversation.attachments.PlayIndicator.HIDDEN
import com.nice.cxonechat.ui.composable.conversation.attachments.PlayIndicator.STANDARD
import com.nice.cxonechat.ui.composable.generic.AsyncImagePainters
import com.nice.cxonechat.ui.composable.generic.ThumbnailSize
import com.nice.cxonechat.ui.composable.generic.asyncImagePainters
import com.nice.cxonechat.ui.composable.generic.forwardingPainter
import com.nice.cxonechat.ui.composable.icons.ChatIcons
import com.nice.cxonechat.ui.composable.icons.filled.PlayCircle
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.util.contentDescription
import com.nice.cxonechat.ui.util.dw
import java.util.UUID

/**
 * Displays a video thumbnail with an optional play icon indicator.
 * The play icon is animated to appear when the video thumbnail is successfully loaded.
 *
 * @param attachment The video attachment containing the URL and content description.
 * @param messageId The ID of the message the attachment belongs to, used for caching purposes.
 * @param playIndicator The type of play icon to display (e.g., SMALL, STANDARD, or HIDDEN).
 * @param modifier Modifier to apply to the composable.
 * @param painters A set of painters for placeholder, fallback, and error states,
 * the default is [asyncImagePainters].
 * @param thumbnailSize The size of the video thumbnail (e.g., LARGE, REGULAR, or SMALL),
 * the default is [ThumbnailSize.REGULAR].
 */
@Composable
@OptIn(UnstableApi::class)
internal fun VideoPreview(
    attachment: Attachment,
    messageId: UUID?,
    playIndicator: PlayIndicator,
    modifier: Modifier = Modifier,
    painters: AsyncImagePainters = asyncImagePainters(),
    thumbnailSize: ThumbnailSize = ThumbnailSize.REGULAR,
) {
    // State to track whether the image has been successfully loaded
    var imageLoaded by remember { mutableStateOf(false) }
    val tint = ColorFilter.tint(LocalContentColor.current)
    // Configure the image loader with video frame decoding and crossfade support
    val imageLoader = ImageLoader.Builder(LocalContext.current)
        .components {
            add(VideoFrameDecoder.Factory())
        }
        .crossfade(true)
        .build()
    // Create forwarding (tinting) painters for placeholder, fallback, and error states
    val placeholder = forwardingPainter(
        painter = painters.placeholder,
        colorFilter = tint
    )
    val fallback = forwardingPainter(
        painter = painters.fallback,
        colorFilter = tint
    )
    val error = forwardingPainter(
        painter = painters.error,
        colorFilter = tint
    )
    val cacheKey = rememberSaveable(messageId, attachment.friendlyName) {
        "${messageId?.let { "${it}_" }}${attachment.friendlyName}"
    }

    // Display the video thumbnail and centered play icon
    Box(
        contentAlignment = Alignment.Center,
    ) {
        // Determine the size of the thumbnail based on the provided size
        val thumbnailDpSize: DpSize = getThumbnailDpSize(thumbnailSize)
        // Build the image request for the video thumbnail and caching
        val model = ImageRequest.Builder(LocalContext.current)
            .data(attachment.url)
            .diskCacheKey(cacheKey)
            .memoryCacheKey(cacheKey)
            .build()

        // AsyncImage to load and display the video thumbnail
        AsyncImage(
            imageLoader = imageLoader,
            model = model,
            contentDescription = attachment.contentDescription,
            placeholder = placeholder,
            fallback = fallback,
            error = error,
            onSuccess = { imageLoaded = true },
            contentScale = ContentScale.Crop,
            modifier = modifier
                .requiredSize(thumbnailDpSize.width, thumbnailDpSize.height),
        )
        // Animated visibility for the play icon
        AnimatedVisibility(
            visible = imageLoaded,
            enter = fadeIn(),
        ) {
            PlayIcon(playIndicator = playIndicator)
        }
    }
}

@Composable
private fun getThumbnailDpSize(thumbnailSize: ThumbnailSize): DpSize = when (thumbnailSize) {
    ThumbnailSize.LARGE -> space.attachmentPreviewLargeSize
    ThumbnailSize.SMALL -> space.attachmentPreviewSmallSize
    ThumbnailSize.REGULAR -> {
        val size = space.attachmentPreviewRegularWidthPercentage.dw
        DpSize(size, size)
    }
}

/**
 * Enum representing the different types of play indicators.
 */
internal enum class PlayIndicator {

    /**
     * The play icon is displayed in `space.playVideoIconSize` size.
     */
    STANDARD,

    /**
     * The play icon is not displayed.
     */
    HIDDEN,
}

/**
 * Displays a play icon based on the provided play indicator type.
 *
 * @param modifier Modifier to apply to the play icon.
 * @param playIndicator The type of play icon to display.
 */
@Composable
internal fun PlayIcon(modifier: Modifier = Modifier, playIndicator: PlayIndicator = STANDARD) {
    val finalModifier = when (playIndicator) {
        HIDDEN -> return
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
        Surface(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
        ) {
            Column(Modifier.wrapContentWidth(align = Alignment.End)) {
                MessageFrame(
                    position = MessageItemGroupState.SOLO,
                    messageContentType = ContentType.Attachment,
                    isAgent = false,
                    colors = chatColors.customer,
                    showFrame = true,
                ) {
                    VideoPreview(
                        attachment = PreviewAttachments.movie,
                        playIndicator = STANDARD,
                        thumbnailSize = ThumbnailSize.LARGE,
                        messageId = UUID.randomUUID(),
                    )
                }
            }
        }
    }
}

@Composable
@PreviewLightDark
private fun PreviewPlayIcon() {
    ChatTheme {
        Surface(Modifier.systemBarsPadding()) {
            Box(
                modifier = Modifier.padding(space.large),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(space.large)
                ) {
                    PlayIndicator.entries.forEach {
                        PlayIcon(playIndicator = it)
                    }
                }
            }
        }
    }
}
