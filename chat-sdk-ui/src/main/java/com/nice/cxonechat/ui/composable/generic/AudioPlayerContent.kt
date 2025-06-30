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

package com.nice.cxonechat.ui.composable.generic

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Forward30
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Replay30
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.player.PlayerState
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.util.toTimeStamp
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toDuration
import kotlin.time.toDurationUnit

/**
 * The view representing the audio player.
 */
@UnstableApi
@Composable
internal fun AudioPlayerContent(
    modifier: Modifier,
    currentTime: String,
    animatedProgress: Float,
    remainingTime: String,
    playerState: PlayerState,
    onSeekBack: () -> Unit = {},
    onSeekForward: () -> Unit = {},
    onPlayPause: () -> Unit = {},
) {
    val enabled by playerState.available
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IndicatorTime(currentTime, enabled)
            ProgressIndicator(animatedProgress, enabled)
            IndicatorTime(remainingTime, enabled)
        }
        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .animateContentSize(),
            horizontalArrangement = Arrangement.spacedBy(space.audioPlayerIconSpacing)
        ) {
            AnimatedVisibility(playerState.canSeekBackward.value) {
                SeekBackButton(enabled, onSeekBack, playerState)
            }
            PlayPauseButton(enabled, onPlayPause, playerState)
            AnimatedVisibility(playerState.canSeekForward.value) {
                SeekForwardButton(enabled, onSeekForward, playerState)
            }
        }
    }
}

@Composable
private fun RowScope.ProgressIndicator(animatedProgress: Float, enabled: Boolean) {
    val color = LocalContentColor.current.let {
        if (enabled) it else it.copy(0.38f)
    }
    LinearProgressIndicator(
        progress = { animatedProgress },
        modifier = Modifier
            .padding(10.dp)
            .height(space.medium)
            .weight(1f),
        trackColor = color.copy(alpha = 0.5f),
        color = color,
        strokeCap = StrokeCap.Round,
        gapSize = (-5).dp, // We don't want to show the gap
        drawStopIndicator = {}, // We don't want to show the stop indicator
    )
}

@Composable
private fun SeekBackButton(enabled: Boolean, onSeekBack: () -> Unit, playerState: PlayerState) {
    IconButton(enabled = enabled, onClick = onSeekBack) {
        AnimatedContent(playerState.seekBackIncrement.longValue) { increment ->
            val seconds = increment.milliseconds.inWholeSeconds.toInt()
            val description = pluralStringResource(R.plurals.seek_back, seconds, seconds)
            val iconMod = Modifier.size(space.audioPlayerSecondaryIconSize)
            when (seconds) {
                30 -> Icon(imageVector = Icons.Default.Replay30, contentDescription = description, iconMod)
                10 -> Icon(imageVector = Icons.Default.Replay10, contentDescription = description, iconMod)
                5 -> Icon(imageVector = Icons.Default.Replay5, contentDescription = description, iconMod)
                else -> Icon(imageVector = Icons.Default.Replay, contentDescription = description, iconMod)
            }
        }
    }
}

@Composable
private fun PlayPauseButton(enabled: Boolean, onPlayPause: () -> Unit, playerState: PlayerState) {
    IconButton(enabled = enabled, onClick = onPlayPause) {
        AnimatedContent(playerState.isPlaying.value) {
            val iconMod = Modifier.size(space.audioPlayerPlayIconSize)
            if (!it) {
                Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.content_description_play), iconMod)
            } else {
                Icon(Icons.Default.Pause, contentDescription = stringResource(R.string.content_description_pause), iconMod)
            }
        }
    }
}

@Composable
private fun SeekForwardButton(enabled: Boolean, onSeekForward: () -> Unit, playerState: PlayerState) {
    IconButton(enabled = enabled, onClick = onSeekForward) {
        AnimatedContent(playerState.seekForwardIncrement.longValue) { increment ->
            val seconds = increment.milliseconds.inWholeSeconds.toInt()
            val description = pluralStringResource(R.plurals.seek_forward, seconds, seconds)
            val iconMod = Modifier.size(space.audioPlayerSecondaryIconSize)
            when (seconds) {
                30 -> Icon(imageVector = Icons.Default.Forward30, contentDescription = description, iconMod)
                10 -> Icon(imageVector = Icons.Default.Forward10, contentDescription = description, iconMod)
                5 -> Icon(imageVector = Icons.Default.Forward5, contentDescription = description, iconMod)
                else -> Icon(imageVector = Icons.Default.FastForward, contentDescription = description, iconMod)
            }
        }
    }
}

@Composable
private fun IndicatorTime(remainingTime: String, enabled: Boolean = true) {
    Text(
        text = remainingTime,
        modifier = Modifier
            .animateContentSize()
            .alpha(if (enabled) 1.0f else 0.38f),
        style = ChatTheme.chatTypography.timestampIndicator
    )
}

@OptIn(UnstableApi::class)
@PreviewLightDark
@Composable
private fun AudioPlayerContentPreview() {
    ChatTheme {
        val state = remember {
            object : PlayerState {
                override val available: State<Boolean> = mutableStateOf(true)
                override val isPlaying = mutableStateOf(false)
                override val duration = mutableStateOf(MINUTES.toMillis(1).toDuration(MILLISECONDS.toDurationUnit()))
                override val position = mutableStateOf(SECONDS.toMillis(20).toDuration(MILLISECONDS.toDurationUnit()))
                override val progress = mutableFloatStateOf(0.25f)
                override val canSeekForward = mutableStateOf(true)
                override val canSeekBackward = mutableStateOf(true)
                override val seekBackIncrement = mutableLongStateOf(SECONDS.toMillis(10))
                override val seekForwardIncrement = mutableLongStateOf(SECONDS.toMillis(10))
            }
        }
        val currentTime by remember(state) {
            derivedStateOf {
                state.position.value.toTimeStamp(Locale.current)
            }
        }
        val remainingTime by remember(state) {
            derivedStateOf {
                (state.duration.value - state.position.value)
                    .toTimeStamp(Locale.current)
            }
        }
        val animatedProgress by animateFloatAsState(
            targetValue = state.progress.floatValue,
            animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
        )
        Column(verticalArrangement = spacedBy(space.medium), modifier = Modifier.fillMaxWidth()) {
            CompositionLocalProvider(
                LocalContentColor provides chatColors.agent.foreground
            ) {
                PreviewContent(
                    chatColors.agent.background,
                    currentTime,
                    animatedProgress,
                    remainingTime,
                    state
                )
            }
            CompositionLocalProvider(
                LocalContentColor provides chatColors.customer.foreground
            ) {
                PreviewContent(
                    chatColors.customer.background,
                    currentTime,
                    animatedProgress,
                    remainingTime,
                    state
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun PreviewContent(
    color: Color,
    currentTime: String,
    animatedProgress: Float,
    remainingTime: String,
    state: PlayerState,
) {
    BoxWithConstraints {
        val maxAttachmentWidth = this.maxWidth.times(0.745f)
        Surface(
            color = color,
            shape = ChatTheme.chatShapes.bubbleSoloShape,
            modifier = Modifier.widthIn(min = space.smallAttachmentSize, max = maxAttachmentWidth)
        ) {
            AudioPlayerContent(
                Modifier.padding(space.audioMessagePadding),
                currentTime,
                animatedProgress,
                remainingTime,
                state
            )
        }
    }
}
