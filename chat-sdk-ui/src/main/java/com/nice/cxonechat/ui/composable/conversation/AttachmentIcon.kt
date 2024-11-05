/*
 * Copyright (c) 2021-2024. NICE Ltd. All rights reserved.
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

import android.content.Context
import android.net.Uri
import android.view.ViewGroup.LayoutParams
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons.Outlined
import androidx.compose.material.icons.outlined.FilePresent
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.nice.cxonechat.message.Attachment
import com.nice.cxonechat.ui.composable.generic.PresetAsyncImage
import com.nice.cxonechat.ui.composable.generic.buildProgressivePlayerForUri
import com.nice.cxonechat.ui.composable.generic.forwardingPainter
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.SelectionFrame
import com.nice.cxonechat.ui.util.contentDescription

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AttachmentIcon(
    attachment: Attachment,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (Attachment) -> Unit,
    onLongClick: (Attachment) -> Unit,
) {
    ChatTheme.SelectionFrame(
        modifier = modifier
            .combinedClickable(
                onClick = { onClick(attachment) },
                onLongClick = { onLongClick(attachment) }
            ),
        selected = selected,
    ) {
        val mimeType = attachment.mimeType

        when {
            mimeType == null -> PlaceholderIcon(attachment)
            mimeType.startsWith("image/") -> ImageIcon(attachment)
            mimeType.startsWith("video/") -> VideoIcon(attachment)
            mimeType.startsWith("audio/") -> AudioIcon(attachment)
            else -> PlaceholderIcon(attachment)
        }
    }
}

@Composable
private fun PlaceholderIcon(attachment: Attachment) {
    Image(
        painter = forwardingPainter(
            rememberVectorPainter(image = Outlined.FilePresent),
            colorFilter = ColorFilter.tint(LocalContentColor.current)
        ),
        contentDescription = attachment.contentDescription,
        modifier = Modifier.padding(space.small)
    )
}

@Composable
private fun ImageIcon(attachment: Attachment) {
    PresetAsyncImage(
        model = attachment.url,
        contentDescription = attachment.contentDescription,
        contentScale = ContentScale.Crop,
    )
}

@Composable
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
private fun VideoIcon(attachment: Attachment) {
    Box {
        if (LocalInspectionMode.current) {
            // ExoPlayer is not working in Preview mode
            Image(
                painter = forwardingPainter(
                    rememberVectorPainter(image = Outlined.Videocam),
                    colorFilter = ColorFilter.tint(LocalContentColor.current)
                ),
                contentDescription = attachment.contentDescription,
                modifier = Modifier.padding(space.small).align(Alignment.Center),
            )
        } else {
            val context = LocalContext.current
            val background = MaterialTheme.colorScheme.surface.toArgb()

            val exoPlayer = remember {
                buildProgressivePlayerForUri(context, Uri.parse(attachment.url)).apply {
                    playWhenReady = false
                    seekTo(0)
                    videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                    repeatMode = Player.REPEAT_MODE_OFF
                }
            }
            AndroidView(
                factory = { viewContext ->
                    playerFactory(viewContext, exoPlayer, background)
                },
                onRelease = { playerView -> playerView.player?.release() },
                update = { playerView -> playerView.updatePlayer(exoPlayer) },
            )
        }
        Image(
            imageVector = Outlined.PlayArrow,
            contentDescription = attachment.contentDescription,
            modifier = Modifier.padding(space.small).align(Alignment.Center).fillMaxSize(),
        )
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
private fun playerFactory(viewContext: Context, exoPlayer: ExoPlayer, @ColorInt background: Int) =
    PlayerView(viewContext).apply {
        controllerAutoShow = false
        useController = false
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
        player = exoPlayer
        layoutParams = FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        setShutterBackgroundColor(background)
    }

private fun PlayerView.updatePlayer(exoPlayer: ExoPlayer) {
    val currentPlayer = player
    if (currentPlayer != exoPlayer) {
        currentPlayer?.release()
        player = exoPlayer
    }
}

@Composable
private fun AudioIcon(attachment: Attachment) {
    Image(
        forwardingPainter(
            painter = rememberVectorPainter(image = Outlined.Mic),
            colorFilter = ColorFilter.tint(LocalContentColor.current)
        ),
        contentDescription = attachment.contentDescription,
        modifier = Modifier.padding(space.small)
    )
}

@Preview
@Composable
private fun PreviewAttachmentIcon(
    @PreviewParameter(AttachmentProvider::class) attachment: Attachment
) {
    ChatTheme {
        AttachmentIcon(
            attachment = attachment,
            modifier = Modifier.size(48.dp),
            onClick = {},
            onLongClick = {}
        )
    }
}
