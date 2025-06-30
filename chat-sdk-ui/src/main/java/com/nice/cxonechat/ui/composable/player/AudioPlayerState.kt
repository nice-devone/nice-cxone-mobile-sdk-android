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

package com.nice.cxonechat.ui.composable.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LongState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle.Event
import androidx.lifecycle.Lifecycle.Event.ON_DESTROY
import androidx.lifecycle.Lifecycle.Event.ON_PAUSE
import androidx.lifecycle.Lifecycle.Event.ON_RESUME
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.Player
import androidx.media3.common.Player.Commands
import androidx.media3.common.Player.Listener
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.nice.cxonechat.ui.composable.generic.AudioPlayerState
import com.nice.cxonechat.ui.composable.generic.AudioPlayerState.Loading
import com.nice.cxonechat.ui.composable.generic.AudioPlayerState.Ready
import com.nice.cxonechat.ui.composable.generic.buildProgressivePlayerForUri
import com.nice.cxonechat.ui.composable.generic.releaseIfAvailable
import com.nice.cxonechat.ui.composable.player.PlaceholderState.available
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * A placeholder [PlayerState] instance that can be used when no actual player is available.
 * It's [available] is always false and all other properties are initialized with default values.
 */
internal object PlaceholderState : PlayerState {
    override val available: State<Boolean> = mutableStateOf(false)
    override val isPlaying = mutableStateOf(false)
    override val duration = mutableStateOf(0.seconds)
    override val position = mutableStateOf(0.seconds)
    override val progress = mutableFloatStateOf(0.0f)
    override val canSeekForward = mutableStateOf(false)
    override val canSeekBackward = mutableStateOf(false)
    override val seekBackIncrement = mutableLongStateOf(0)
    override val seekForwardIncrement = mutableLongStateOf(0)
}

/**
 * Represents the state of a [Player] instance in a form usable in Compose views.
 */
internal interface PlayerState {
    /** Indicates if the player is available for usage. */
    val available: State<Boolean>

    /** Indicates if the player is currently playing. */
    val isPlaying: State<Boolean>

    /** The duration of the current media. */
    val duration: State<Duration>

    /** The current position in the media. */
    val position: State<Duration>

    /** The progress of the media playback as a float between 0.0 and 1.0. */
    val progress: State<Float>

    /** Indicates if the player can seek forward in the current media. */
    val canSeekForward: State<Boolean>

    /** Indicates if the player can seek backward in the current media. */
    val canSeekBackward: State<Boolean>

    /** The seek increment for seeking backward in the current media. */
    val seekBackIncrement: LongState

    /** The seek increment for seeking forward in the current media. */
    val seekForwardIncrement: LongState
}

/**
 * Simple representation of the [Player] state intended for use in Compose.
 * For correct functionality this listener has to be added to the [Player] instance and it should be removed,
 * when the Composable is disposed.
 */
@UnstableApi
internal class BasicPlayerState(
    private val player: Player,
    private val scope: CoroutineScope,
) : Listener, PlayerState {
    override val available: State<Boolean> = mutableStateOf(true)
    override val isPlaying = mutableStateOf(false)
    override val duration = mutableStateOf(0.seconds)
    override val position = mutableStateOf(0.seconds)
    override val progress = derivedStateOf {
        (position.value / duration.value).toFloat()
    }
    override val canSeekForward = mutableStateOf(player.isCommandAvailable(Player.COMMAND_SEEK_FORWARD))
    override val canSeekBackward = mutableStateOf(player.isCommandAvailable(Player.COMMAND_SEEK_BACK))
    override val seekBackIncrement = mutableLongStateOf(player.seekBackIncrement)
    override val seekForwardIncrement = mutableLongStateOf(player.seekForwardIncrement)

    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        super.onTimelineChanged(timeline, reason)
        val currentDuration = this.duration.value
        if (currentDuration.isInfinite() || currentDuration.inWholeMilliseconds == 0L && !timeline.isEmpty) {
            this.duration.value = player.duration.milliseconds
        }
    }

    override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
        super.onMediaMetadataChanged(mediaMetadata)
        val currentDuration = this.duration.value
        if (currentDuration.isInfinite() || currentDuration.inWholeMilliseconds == 0L) {
            this.duration.value = mediaMetadata.durationMs?.milliseconds ?: player.duration.milliseconds
        }
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        super.onIsPlayingChanged(isPlaying)
        this.isPlaying.value = isPlaying
        val currentDuration = this.duration.value
        if (currentDuration.isInfinite() || currentDuration == 0.milliseconds) {
            this.duration.value = player.duration.milliseconds
        }

        scope.launch {
            while (player.isPlaying && isActive) {
                position.value = player.currentPosition.milliseconds
                if (this@BasicPlayerState.duration.value.isInfinite()) {
                    this@BasicPlayerState.duration.value = player.duration.milliseconds
                }
                delay(200)
            }
        }
    }

    override fun onMetadata(metadata: Metadata) {
        super.onMetadata(metadata)
        val currentDuration = this.duration.value
        if (currentDuration.isInfinite() || currentDuration == 0.milliseconds) {
            this.duration.value = metadata.presentationTimeUs.milliseconds
        }
    }

    override fun onAvailableCommandsChanged(availableCommands: Commands) {
        super.onAvailableCommandsChanged(availableCommands)
        canSeekForward.value = availableCommands.contains(Player.COMMAND_SEEK_FORWARD)
        canSeekBackward.value = availableCommands.contains(Player.COMMAND_SEEK_BACK)
    }

    override fun onSeekBackIncrementChanged(seekBackIncrementMs: Long) {
        super.onSeekBackIncrementChanged(seekBackIncrementMs)
        seekBackIncrement.longValue = seekBackIncrementMs
    }

    override fun onSeekForwardIncrementChanged(seekForwardIncrementMs: Long) {
        super.onSeekForwardIncrementChanged(seekForwardIncrementMs)
        seekForwardIncrement.longValue = seekForwardIncrementMs
    }
}

@Composable
internal fun produceAudioPlayerState(
    context: Context,
    recordingUri: Uri,
    buildUpon: (ExoPlayer.Builder).() -> ExoPlayer.Builder = { this },
): State<AudioPlayerState> =
    produceState<AudioPlayerState>(
        initialValue = Loading,
        key1 = context,
        key2 = recordingUri
    ) {
        value = if (recordingUri == Uri.EMPTY) {
            Loading
        } else {
            Ready(
                player = buildProgressivePlayerForUri(context, recordingUri, buildUpon)
            )
        }
        this.awaitDispose {
            (value as? Ready)?.player?.releaseIfAvailable()
            value = Loading
        }
    }

@OptIn(UnstableApi::class)
@Composable
internal fun rememberPlayerState(
    player: Player,
    isLifecycleAware: Boolean = true,
): PlayerState {
    val scope = rememberCoroutineScope()
    val playerState = remember(player) { BasicPlayerState(player, scope) }
    DisposableEffect(playerState, player) {
        player.addListener(playerState)
        onDispose {
            player.removeListener(playerState)
        }
    }
    if (isLifecycleAware) {
        SetupPlayerLifecycle(player)
    }
    return playerState
}

@Composable
private fun SetupPlayerLifecycle(player: Player) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(player, lifecycleOwner) {
        val observer = AudioPlayerLifecycleEventObserver(player)
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.releaseIfAvailable()
        }
    }
}

private class AudioPlayerLifecycleEventObserver(private val player: Player) : LifecycleEventObserver {
    private var wasPlaying = player.isPlaying
    private var wasDestroyed = false
    override fun onStateChanged(source: LifecycleOwner, event: Event) {
        when (event) {
            ON_RESUME -> if (!wasDestroyed && wasPlaying && player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE)) {
                player.play()
            }

            ON_PAUSE -> if (!wasDestroyed) {
                wasPlaying = player.isPlaying
                if (player.isCommandAvailable(Player.COMMAND_PLAY_PAUSE)) {
                    player.pause()
                }
            }

            ON_DESTROY -> wasDestroyed = true

            else -> {}
        }
    }
}
