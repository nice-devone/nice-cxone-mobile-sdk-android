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

package com.nice.cxonechat.ui.composable.generic

import android.content.Context
import android.net.Uri
import android.view.ViewGroup.LayoutParams
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Icons.Outlined
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.R.string
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.C.VideoScalingMode
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.nice.cxonechat.ui.composable.conversation.PreviewAttachments
import com.nice.cxonechat.ui.composable.generic.VideoPlayerState.Error
import com.nice.cxonechat.ui.composable.generic.VideoPlayerState.Loading
import com.nice.cxonechat.ui.composable.generic.VideoPlayerState.Ready
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.LocalSpace

/**
 * ExoPlayer wrapped as Composable, set for video playing.
 *
 * @param uri An [Uri] of the video to be played.
 * @param modifier A [Modifier] which should be used by the player view.
 * @param videoScalingMode A [VideoScalingMode] to be used by the player,
 * the default is [C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING].
 */
@Composable
@OptIn(UnstableApi::class)
internal fun VideoPlayer(
    uri: Uri?,
    modifier: Modifier = Modifier,
    @VideoScalingMode videoScalingMode: Int = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING,
) {
    val iconModifier = Modifier
        .padding(space.large)
        .size(space.playStatusIconSize)
    if (uri == null) {
        ErrorIcon(iconModifier)
    } else {
        val context = LocalContext.current
        val playerResult by produceState<VideoPlayerState>(
            initialValue = Loading,
            key1 = context,
            key2 = uri
        ) {
            val result = runCatching {
                buildProgressivePlayerForUri(context, uri)
            }.map { player ->
                player.playWhenReady = true
                player.videoScalingMode = videoScalingMode
                player.repeatMode = Player.REPEAT_MODE_OFF
                Ready(player)
            }.onFailure {
                Error(it)
            }
            value = result.getOrThrow()
        }

        when (val result = playerResult) {
            is Loading -> CircularProgressIndicator(modifier = iconModifier)
            is Error -> ErrorIcon(iconModifier)
            is Ready -> if (LocalInspectionMode.current) {
                // ExoPlayer is not working in Preview mode
                LocalInspectionPlaceholder()
            } else {
                val exoPlayer = result.player
                AndroidView(
                    factory = { viewContext -> playerFactory(viewContext, exoPlayer) },
                    modifier = modifier,
                    onRelease = { playerView -> playerView.player?.release() },
                    update = { playerView -> playerView.updatePlayer(exoPlayer) },
                )
            }
        }
    }
}

@Composable
private fun ErrorIcon(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Default.ErrorOutline,
        contentDescription = stringResource(id = string.default_error_message),
        modifier = modifier
    )
}

/** Internal state used to track preparation of a Video player. */
private sealed interface VideoPlayerState {
    object Loading : VideoPlayerState
    data class Ready(val player: Player) : VideoPlayerState
    data class Error(val throwable: Throwable) : VideoPlayerState
}

private fun PlayerView.updatePlayer(playerInstance: Player) {
    val currentPlayer = player
    if (currentPlayer != playerInstance) {
        currentPlayer?.release()
        player = playerInstance
    }
}

@OptIn(UnstableApi::class)
private fun playerFactory(
    viewContext: Context,
    playerInstance: Player,
) =
    PlayerView(viewContext).apply {
        setShowNextButton(false)
        setShowPreviousButton(false)
        setShowShuffleButton(false)
        setShowRewindButton(false)
        setShowFastForwardButton(false)
        controllerAutoShow = true
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
        player = playerInstance
        layoutParams = FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        setEnableComposeSurfaceSyncWorkaround(true)
    }

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

@Preview
@Composable
private fun VideoPlayerPreview() {
    ChatTheme {
        VideoPlayer(
            uri = PreviewAttachments.movie.url.toUri(),
            modifier = Modifier.fillMaxSize()
        )
    }
}
