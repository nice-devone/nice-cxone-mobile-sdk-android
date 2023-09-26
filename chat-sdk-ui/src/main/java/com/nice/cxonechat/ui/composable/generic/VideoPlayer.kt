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

package com.nice.cxonechat.ui.composable.generic

import android.content.Context
import android.net.Uri
import android.view.ViewGroup.LayoutParams
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.LocalContentColor
import androidx.compose.material.icons.Icons.Outlined
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.nice.cxonechat.ui.composable.theme.LocalSpace

/**
 * ExoPlayer wrapped as Composable, set for video playing.
 */
@Composable
@OptIn(UnstableApi::class)
internal fun VideoPlayer(uri: Uri, modifier: Modifier = Modifier) {
    if (LocalInspectionMode.current) {
        // ExoPlayer is not working in Preview mode
        LocalInspectionPlaceholder()
    } else {
        val context = LocalContext.current

        val exoPlayer = remember {
            buildProgressivePlayerForUri(context, uri)
        }

        exoPlayer.playWhenReady = true
        exoPlayer.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
        exoPlayer.repeatMode = Player.REPEAT_MODE_OFF

        AndroidView(
            factory = { viewContext -> playerFactory(viewContext, exoPlayer) },
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
private fun playerFactory(viewContext: Context, exoPlayer: ExoPlayer) =
    PlayerView(viewContext).apply {
        controllerAutoShow = true
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        player = exoPlayer
        layoutParams = FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
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
