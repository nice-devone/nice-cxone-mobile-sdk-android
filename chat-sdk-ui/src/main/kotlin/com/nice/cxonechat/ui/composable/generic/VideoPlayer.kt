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

import android.net.Uri
import androidx.annotation.OptIn
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Icons.Outlined
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.VideoFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.R.string
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.common.C
import androidx.media3.common.C.VideoScalingMode
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import androidx.media3.ui.compose.state.rememberPresentationState
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.conversation.PreviewAttachments
import com.nice.cxonechat.ui.composable.generic.VideoPlayerState.Error
import com.nice.cxonechat.ui.composable.generic.VideoPlayerState.Loading
import com.nice.cxonechat.ui.composable.generic.VideoPlayerState.Ready
import com.nice.cxonechat.ui.composable.player.rememberPositionAndDuration
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.composable.theme.LocalSpace
import com.nice.cxonechat.ui.util.toTimeStamp
import kotlinx.coroutines.delay
import kotlin.time.Duration

/**
 * Media3 Compose video player composable.
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
        return
    }
    // Neither a preview nor a screenshot test can decode video, so preparing a player would only leave a
    // blank surface behind. Draw the placeholder before building one rather than after.
    if (LocalInspectionMode.current) {
        LocalInspectionPlaceholder(modifier)
        return
    }
    val context = LocalContext.current
    val playerResult by produceState<VideoPlayerState>(
        initialValue = Loading,
        key1 = context,
        key2 = uri
    ) {
        value = runCatching {
            buildProgressivePlayerForUri(context, uri)
        }.fold(
            onSuccess = { player ->
                player.playWhenReady = true
                player.videoScalingMode = videoScalingMode
                player.repeatMode = Player.REPEAT_MODE_OFF
                Ready(player)
            },
            onFailure = { Error(it) }
        )
        awaitDispose {
            (value as? Ready)?.player?.release()
        }
    }

    val loadingDescription = stringResource(R.string.loading)
    when (val result = playerResult) {
        is Loading -> CircularProgressIndicator(
            modifier = iconModifier.semantics { contentDescription = loadingDescription }
        )
        is Error -> ErrorIcon(iconModifier)
        is Ready -> VideoPlayerContent(player = result.player, modifier = modifier)
    }
}

/**
 * The video surface and the playback controls over it, for a player that is already prepared.
 *
 * Visible beyond [VideoPlayer] so a screenshot test can supply its own [Player]. [VideoPlayer] builds a real
 * [androidx.media3.exoplayer.ExoPlayer], which decodes nothing under Robolectric, and its position and
 * duration would then be whatever playback had reached.
 *
 * @param player A prepared player to draw and drive the controls from.
 * @param modifier A [Modifier] applied to the box holding the surface and the controls.
 */
@Composable
@VisibleForTesting
@OptIn(UnstableApi::class)
internal fun VideoPlayerContent(
    player: Player,
    modifier: Modifier = Modifier,
) {
    var showControls by remember { mutableStateOf(true) }
    val playPauseState = rememberPlayPauseButtonState(player)
    val isPlaying = !playPauseState.showPlay
    val positionAndDuration by rememberPositionAndDuration(player)
    val (position, duration) = positionAndDuration
    val progress = if (duration.isFinite() && duration > Duration.ZERO) (position / duration).toFloat() else 0f
    val currentTime = remember(position, duration) {
        if (duration.isInfinite()) Duration.INFINITE.toTimeStamp(Locale.current) else position.toTimeStamp(Locale.current)
    }
    val remainingTime = remember(position, duration) {
        val remaining = if (duration.isInfinite()) duration else (duration - position).coerceAtLeast(Duration.ZERO)
        remaining.toTimeStamp(Locale.current)
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
    )
    // Auto-hide controls after 3s when playing; re-show when paused or playback ends.
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(3_000)
            showControls = false
        } else if (!isPlaying) {
            showControls = true
        }
    }
    val presentationState = rememberPresentationState(player)
    Box(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
        ) { showControls = true },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            // A SurfaceView scales its buffer to its own bounds, so the bounds are the only thing that can
            // hold the clip's proportions. The controls share the scaled box rather than the container, so
            // they span the frame being watched instead of the bars around it. The modifier leaves the box
            // filling its container until the clip's size is known, which arrives with the decoder's output
            // format rather than with the first frame drawn from it.
            modifier = Modifier
                .resizeWithContentScale(ContentScale.Fit, presentationState.videoSizeDp)
                .testTag("video_frame"),
        ) {
            PlayerSurface(player = player, modifier = Modifier.fillMaxSize())
            VideoControlsOverlay(
                showControls = showControls,
                showPlay = playPauseState.showPlay,
                isPlayEnabled = playPauseState.isEnabled,
                onPlayPause = { playPauseState.onClick() },
                currentTime = currentTime,
                remainingTime = remainingTime,
                animatedProgress = animatedProgress,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .testTag("video_controls"),
            )
        }
    }
}

@Composable
private fun VideoControlsOverlay(
    showControls: Boolean,
    showPlay: Boolean,
    isPlayEnabled: Boolean,
    onPlayPause: () -> Unit,
    currentTime: String,
    remainingTime: String,
    animatedProgress: Float,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = showControls,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(horizontal = space.large, vertical = space.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            VideoProgressRow(
                currentTime = currentTime,
                remainingTime = remainingTime,
                animatedProgress = animatedProgress,
            )
            IconButton(onClick = onPlayPause, enabled = isPlayEnabled) {
                Icon(
                    imageVector = if (showPlay) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (showPlay) {
                        stringResource(R.string.content_description_play)
                    } else {
                        stringResource(R.string.content_description_pause)
                    },
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun VideoProgressRow(
    currentTime: String,
    remainingTime: String,
    animatedProgress: Float,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = currentTime,
            color = Color.White,
            style = ChatTheme.chatTypography.timestampIndicator,
        )
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = space.medium)
                .height(space.medium),
            color = Color.White,
            trackColor = Color.White.copy(alpha = 0.5f),
            strokeCap = StrokeCap.Round,
            gapSize = (-5).dp, // We don't want to show the gap
            drawStopIndicator = {},
        )
        Text(
            text = remainingTime,
            color = Color.White,
            style = ChatTheme.chatTypography.timestampIndicator,
        )
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

/** Internal state used to track preparation of a video player. */
private sealed interface VideoPlayerState {
    data object Loading : VideoPlayerState
    data class Ready(val player: Player) : VideoPlayerState
    data class Error(val throwable: Throwable) : VideoPlayerState
}

/**
 * Stands in for the video where none can be decoded: in a preview, and under a screenshot test.
 *
 * The border marks out the area the video is given. The icon inside it is centred and keeps its own
 * proportions, so without it a capped media area and one spanning the whole window draw alike, and no golden
 * could tell the two apart.
 */
@Composable
private fun LocalInspectionPlaceholder(modifier: Modifier = Modifier) {
    Image(
        imageVector = Outlined.VideoFile,
        contentDescription = "Preview replacement for video",
        modifier = modifier
            .fillMaxSize() // Presume that video is large
            .defaultMinSize(LocalSpace.current.clickableSize, LocalSpace.current.clickableSize)
            .border(4.dp, color = Color.Cyan),
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
