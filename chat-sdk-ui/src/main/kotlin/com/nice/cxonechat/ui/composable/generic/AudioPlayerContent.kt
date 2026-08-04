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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.layout.systemBarsPadding
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space
import com.nice.cxonechat.ui.util.rememberAccessibilityTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * The view representing the audio player controls.
 *
 * @param currentTime Formatted string for the current playback position.
 * @param animatedProgress Animated progress value (0.0–1.0).
 * @param remainingTime Formatted string for the remaining duration.
 * @param isEnabled Whether the player is ready and controls are interactive.
 * @param isPlaying Whether the player is currently playing.
 * @param canSeekBack Whether seek-back is available.
 * @param seekBackIncrementMs Seek-back increment in milliseconds.
 * @param canSeekForward Whether seek-forward is available.
 * @param seekForwardIncrementMs Seek-forward increment in milliseconds.
 * @param modifier The modifier to apply to the AudioPlayerContent.
 * @param position Current playback position used for accessibility announcements.
 * @param duration Total media duration used for accessibility announcements.
 * @param onPlayPause Called when the play/pause button is clicked.
 * @param onSeekBack Called when the seek-back button is clicked.
 * @param onSeekForward Called when the seek-forward button is clicked.
 */
@Composable
internal fun AudioPlayerContent(
    currentTime: String,
    animatedProgress: Float,
    remainingTime: String,
    isEnabled: Boolean,
    isPlaying: Boolean,
    canSeekBack: Boolean,
    seekBackIncrementMs: Long,
    canSeekForward: Boolean,
    seekForwardIncrementMs: Long,
    modifier: Modifier = Modifier,
    position: Duration = Duration.ZERO,
    duration: Duration = Duration.ZERO,
    onPlayPause: () -> Unit = {},
    onSeekBack: () -> Unit = {},
    onSeekForward: () -> Unit = {},
) {
    val debouncedSeekbarMessage = rememberDebouncedSeekbarA11y(position = position, duration = duration)
    Column(
        modifier = modifier.clipToBounds(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Seekbar row — merged so TalkBack reads it as a single focusable node.
        // Buttons are kept outside this merge so they remain individually navigable
        // and properly scoped within the parent audio_attachment traversal group.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .focusable(true)
                .semantics(mergeDescendants = true) {
                    contentDescription = debouncedSeekbarMessage
                    traversalIndex = 0f
                }
        ) {
            IndicatorTime(currentTime, isEnabled)
            ProgressIndicator(animatedProgress, isEnabled)
            IndicatorTime(remainingTime, isEnabled)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .animateContentSize()
                .semantics { traversalIndex = 1f },
            horizontalArrangement = Arrangement.spacedBy(space.audioPlayerIconSpacing)
        ) {
            AnimatedVisibility(visible = canSeekBack, modifier = Modifier.minimumInteractiveComponentSize()) {
                SeekBackButton(isEnabled, onSeekBack, seekBackIncrementMs)
            }
            PlayPauseButton(isEnabled, onPlayPause, isPlaying)
            AnimatedVisibility(canSeekForward) {
                SeekForwardButton(isEnabled, onSeekForward, seekForwardIncrementMs)
            }
        }
    }
}

/**
 * Builds the seekbar accessibility message and debounces updates to avoid overwhelming TalkBack.
 */
@Composable
private fun rememberDebouncedSeekbarA11y(position: Duration, duration: Duration): String {
    val totalTimeAccessible = rememberAccessibilityTime(
        duration = duration,
        infinityTextRes = string.content_description_audio_duration_infinite,
    )
    val currentTimeAccessible = rememberAccessibilityTime(
        duration = position,
        infinityTextRes = string.content_description_audio_duration_infinite,
    )
    val seekbarMessage = stringResource(
        string.progress_indicator_content_description,
        currentTimeAccessible,
        totalTimeAccessible
    )
    val latestSeekbarMessage by rememberUpdatedState(seekbarMessage)
    var debouncedMessage by remember { mutableStateOf(seekbarMessage) }
    LaunchedEffect(currentTimeAccessible, totalTimeAccessible) {
        debouncedMessage = latestSeekbarMessage
    }
    return debouncedMessage
}

@Composable
private fun RowScope.ProgressIndicator(
    animatedProgress: Float,
    enabled: Boolean,
) {
    val color = LocalContentColor.current.let {
        if (enabled) it else it.copy(0.38f)
    }
    LinearProgressIndicator(
        progress = { animatedProgress },
        modifier = Modifier
            .padding(10.dp)
            .height(space.medium)
            .weight(1f)
            .semantics {
                testTag = "progress_indicator"
                hideFromAccessibility()
            },
        trackColor = color.copy(alpha = 0.5f),
        color = color,
        strokeCap = StrokeCap.Round,
        gapSize = (-5).dp, // We don't want to show the gap
        drawStopIndicator = {}, // We don't want to show the stop indicator
    )
}

@Composable
private fun SeekBackButton(enabled: Boolean, onSeekBack: () -> Unit, seekBackIncrementMs: Long) {
    IconButton(
        enabled = enabled,
        onClick = onSeekBack,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("seek_back_button")
    ) {
        AnimatedContent(seekBackIncrementMs) { incrementMs ->
            val seconds = incrementMs.milliseconds.inWholeSeconds.toInt()
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
private fun PlayPauseButton(enabled: Boolean, onPlayPause: () -> Unit, isPlaying: Boolean) {
    IconButton(
        enabled = enabled,
        onClick = onPlayPause,
        modifier = Modifier.testTag("play_pause_button")
    ) {
        AnimatedContent(isPlaying) {
            val iconMod = Modifier.size(space.audioPlayerPlayIconSize)
            if (!it) {
                Icon(Icons.Default.PlayArrow, contentDescription = stringResource(string.content_description_play), iconMod)
            } else {
                Icon(Icons.Default.Pause, contentDescription = stringResource(string.content_description_pause), iconMod)
            }
        }
    }
}

@Composable
private fun SeekForwardButton(enabled: Boolean, onSeekForward: () -> Unit, seekForwardIncrementMs: Long) {
    IconButton(
        enabled = enabled,
        onClick = onSeekForward,
        modifier = Modifier.testTag("seek_forward_button")
    ) {
        AnimatedContent(seekForwardIncrementMs) { incrementMs ->
            val seconds = incrementMs.milliseconds.inWholeSeconds.toInt()
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
private fun IndicatorTime(
    time: String,
    enabled: Boolean = true,
) {
    Text(
        text = time,
        modifier = Modifier
            .animateContentSize()
            .alpha(if (enabled) 1.0f else 0.38f)
            .semantics {
                hideFromAccessibility() // Time is announced via the parent AudioPlayerContent's content description
            },
        style = ChatTheme.chatTypography.timestampIndicator
    )
}

@PreviewLightDark
@Composable
private fun AudioPlayerContentPreview() {
    ChatTheme {
        val animatedProgress by animateFloatAsState(
            targetValue = 0.25f,
            animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
        )
        Column(
            verticalArrangement = spacedBy(space.medium),
            modifier = Modifier
                .systemBarsPadding()
                .fillMaxWidth()
        ) {
            PreviewContent(
                color = chatColors.token.background.surface.default,
                currentTime = "0:20",
                animatedProgress = animatedProgress,
                remainingTime = "0:40",
            )
            PreviewContent(
                color = chatColors.token.brand.primary,
                currentTime = "0:20",
                animatedProgress = animatedProgress,
                remainingTime = "0:40",
            )
        }
    }
}

@Composable
private fun PreviewContent(
    color: Color,
    currentTime: String,
    animatedProgress: Float,
    remainingTime: String,
) {
    BoxWithConstraints {
        val maxAttachmentWidth = this.maxWidth.times(0.745f)
        Surface(
            color = color,
            shape = ChatTheme.chatShapes.bubbleSoloShape,
            modifier = Modifier.widthIn(min = space.smallAttachmentSize, max = maxAttachmentWidth)
        ) {
            AudioPlayerContent(
                currentTime = currentTime,
                animatedProgress = animatedProgress,
                remainingTime = remainingTime,
                isEnabled = true,
                isPlaying = false,
                canSeekBack = true,
                seekBackIncrementMs = 10_000L,
                canSeekForward = true,
                seekForwardIncrementMs = 10_000L,
                modifier = Modifier.padding(space.audioMessagePadding)
            )
        }
    }
}
