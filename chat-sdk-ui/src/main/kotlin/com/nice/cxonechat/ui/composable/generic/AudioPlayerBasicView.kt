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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.Lifecycle.Event.ON_PAUSE
import androidx.lifecycle.Lifecycle.Event.ON_RESUME
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.state.rememberPlayPauseButtonState
import androidx.media3.ui.compose.state.rememberSeekBackButtonState
import androidx.media3.ui.compose.state.rememberSeekForwardButtonState
import com.nice.cxonechat.ui.composable.generic.AudioPlayerState.Loading
import com.nice.cxonechat.ui.composable.generic.AudioPlayerState.Ready
import com.nice.cxonechat.ui.composable.player.produceAudioPlayerState
import com.nice.cxonechat.ui.composable.player.rememberPositionAndDuration
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatColors
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatShapes
import com.nice.cxonechat.ui.util.toTimeStamp
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * An audio player which plays audio from the given [uri] using a [Player] instance.
 *
 * It shows a progress bar, play/pause button, and seek buttons preset to [seekIncrement].
 * The buttons are displayed iff they are available as [Player.Commands].
 *
 * The view will show a loading indicator while the player is being prepared.
 *
 * The audio is preset to pause if the composable lifecycle owner is paused while it is playing
 * and will resume when the lifecycle owner is resumed if it was playing previously.
 *
 * The [Player] instance is released when the composable is disposed.
 */
@Composable
internal fun AudioPlayerBasicView(
    uri: Uri,
    modifier: Modifier = Modifier,
    seekIncrement: Long = 10.seconds.inWholeMilliseconds,
) {
    val context = LocalContext.current
    val playerResult by produceAudioPlayerState(context, uri) {
        setSeekBackIncrementMs(seekIncrement)
        setSeekForwardIncrementMs(seekIncrement)
    }
    AnimatedContent(
        targetState = playerResult,
        modifier = Modifier.animateContentSize(),
        contentAlignment = Alignment.TopCenter,
    ) { result ->
        when (result) {
            Loading -> LoadingView(modifier)

            is Ready -> AudioPlayerView(result.player, modifier)
        }
    }
}

@Composable
private fun LoadingView(modifier: Modifier) {
    val placeholderTime = remember { Duration.INFINITE.toTimeStamp(Locale.current) }
    AudioPlayerContent(
        currentTime = placeholderTime,
        animatedProgress = 0f,
        remainingTime = placeholderTime,
        position = Duration.INFINITE,
        duration = Duration.INFINITE,
        isEnabled = false,
        isPlaying = false,
        canSeekBack = false,
        seekBackIncrementMs = 0L,
        canSeekForward = false,
        seekForwardIncrementMs = 0L,
        modifier = modifier,
    )
}

@OptIn(UnstableApi::class)
@Composable
private fun AudioPlayerView(
    player: Player,
    modifier: Modifier = Modifier,
) {
    PlayerLifecycleEffect(player)
    val playPauseState = rememberPlayPauseButtonState(player)
    val seekBackState = rememberSeekBackButtonState(player)
    val seekForwardState = rememberSeekForwardButtonState(player)
    // Seek increments are configured at player creation time; read directly from player.
    val seekBackIncrementMs = remember(player) { player.seekBackIncrement }
    val seekForwardIncrementMs = remember(player) { player.seekForwardIncrement }
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
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    )
    AudioPlayerContent(
        currentTime = currentTime,
        animatedProgress = animatedProgress,
        remainingTime = remainingTime,
        position = position,
        duration = duration,
        isEnabled = playPauseState.isEnabled,
        isPlaying = !playPauseState.showPlay,
        canSeekBack = seekBackState.isEnabled,
        seekBackIncrementMs = seekBackIncrementMs,
        canSeekForward = seekForwardState.isEnabled,
        seekForwardIncrementMs = seekForwardIncrementMs,
        onPlayPause = { playPauseState.onClick() },
        onSeekBack = { seekBackState.onClick() },
        onSeekForward = { seekForwardState.onClick() },
        modifier = modifier,
    )
}

/**
 * Pauses the [player] when the lifecycle owner is paused and resumes it when the lifecycle
 * owner is resumed (only if it was playing before the pause).
 */
@Composable
private fun PlayerLifecycleEffect(player: Player) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(player, lifecycleOwner) {
        val observer = PlayerLifecycleObserver(player)
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

private class PlayerLifecycleObserver(private val player: Player) : LifecycleEventObserver {
    private var wasPlaying = player.isPlaying
    private var wasDestroyed = false

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            ON_RESUME -> resumeIfNeeded()
            ON_PAUSE -> pauseAndRemember()
            ON_DESTROY -> wasDestroyed = true
            else -> Unit
        }
    }

    private fun resumeIfNeeded() {
        if (!wasDestroyed && wasPlaying && player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE)) {
            player.play()
        }
    }

    private fun pauseAndRemember() {
        if (wasDestroyed) return
        wasPlaying = player.isPlaying
        if (player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE)) player.pause()
    }
}

internal sealed interface AudioPlayerState {
    data object Loading : AudioPlayerState
    data class Ready(val player: Player) : AudioPlayerState
}

@PreviewLightDark
@Composable
private fun AudioPlayerPreview() {
    ChatTheme {
        BoxWithConstraints(Modifier.systemBarsPadding()) {
            val maxAttachmentWidth = this.maxWidth.times(0.8f)
            Surface(
                color = chatColors.token.brand.primary,
                contentColor = chatColors.token.content.primary,
                shape = chatShapes.bubbleSoloShape,
                modifier = Modifier.widthIn(max = maxAttachmentWidth),
            ) {
                AudioPlayerBasicView(
                    uri = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3".toUri(),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
