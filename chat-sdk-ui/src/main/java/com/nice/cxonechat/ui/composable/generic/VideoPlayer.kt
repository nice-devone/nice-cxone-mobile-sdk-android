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

package com.nice.cxonechat.ui.composable.generic

import android.content.Context
import android.net.Uri
import android.view.ViewGroup.LayoutParams
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Icons.Outlined
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.R.string
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.C.VideoScalingMode
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.media3.ui.PlayerView.FullscreenButtonClickListener
import com.nice.cxonechat.ui.composable.theme.LocalSpace

/**
 * ExoPlayer wrapped as Composable, set for video playing.
 *
 * @param uri An [Uri] of the video to be played.
 * @param modifier A [Modifier] which should be used by the player view.
 * @param videoScalingMode A [VideoScalingMode] to be used by the player,
 * the default is [C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING].
 * @param onFullScreenClickListener An optional listener which will
 */
@Composable
@OptIn(UnstableApi::class)
internal fun VideoPlayer(
    uri: Uri?,
    modifier: Modifier = Modifier,
    @VideoScalingMode videoScalingMode: Int = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING,
    onFullScreenClickListener: FullscreenButtonClickListener? = null,
) {
    if (uri == null) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = stringResource(id = string.default_error_message)
        )
    } else if (LocalInspectionMode.current) {
        // ExoPlayer is not working in Preview mode
        LocalInspectionPlaceholder()
    } else {
        val context = LocalContext.current

        val exoPlayer = remember {
            buildProgressivePlayerForUri(context, uri)
        }

        exoPlayer.playWhenReady = true
        exoPlayer.videoScalingMode = videoScalingMode
        exoPlayer.repeatMode = Player.REPEAT_MODE_OFF

        AndroidView(
            factory = { viewContext -> playerFactory(viewContext, exoPlayer, onFullScreenClickListener) },
            modifier = modifier,
            onRelease = { playerView -> playerView.player?.release() },
            update = { playerView -> playerView.updatePlayer(exoPlayer) },
        )
    }
}

private fun PlayerView.updatePlayer(exoPlayer: ExoPlayer) {
    val currentPlayer = player
    if (currentPlayer != exoPlayer) {
        currentPlayer?.release()
        player = exoPlayer
    }
}

@OptIn(UnstableApi::class)
private fun playerFactory(
    viewContext: Context,
    exoPlayer: ExoPlayer,
    onFullScreenClickListener: FullscreenButtonClickListener?,
) =
    PlayerView(viewContext).apply {
        controllerAutoShow = true
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        player = exoPlayer
        layoutParams = FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        setFullscreenButtonClickListener(onFullScreenClickListener)
    }

@Preview
@Composable
private fun LocalInspectionPlaceholder() {
    Image(
        imageVector = Outlined.VideoFile,
        contentDescription = "Preview replacement for video",
        modifier = Modifier
            .fillMaxSize()
            .defaultMinSize(LocalSpace.current.clickableSize, LocalSpace.current.clickableSize), // Presume that video is large
        colorFilter = ColorFilter.tint(LocalContentColor.current)
    )
}
