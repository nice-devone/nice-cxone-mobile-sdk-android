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

package com.nice.cxonechat.ui.composable.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.nice.cxonechat.ui.composable.generic.AudioPlayerState
import com.nice.cxonechat.ui.composable.generic.AudioPlayerState.Loading
import com.nice.cxonechat.ui.composable.generic.AudioPlayerState.Ready
import com.nice.cxonechat.ui.composable.generic.buildProgressivePlayerForUri
import com.nice.cxonechat.ui.composable.generic.releaseIfAvailable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val POSITION_POLL_INTERVAL_MS = 200L

private val PROGRESS_RELEVANT_PLAYER_EVENTS = listOf(
    Player.EVENT_IS_PLAYING_CHANGED,
    Player.EVENT_PLAYBACK_STATE_CHANGED,
    Player.EVENT_POSITION_DISCONTINUITY,
    Player.EVENT_TIMELINE_CHANGED,
)

/**
 * Produces the [AudioPlayerState] for the given [recordingUri], managing the [ExoPlayer] lifecycle.
 *
 * The player is built asynchronously and released when the composable leaves composition.
 *
 * @param context A [Context] used to build the player.
 * @param recordingUri The [Uri] of the audio to play.
 * @param buildUpon Optional lambda for additional [ExoPlayer.Builder] configuration.
 */
@OptIn(UnstableApi::class)
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
            runCatching { buildProgressivePlayerForUri(context, recordingUri, buildUpon) }
                .fold(onSuccess = { Ready(it) }, onFailure = { Loading })
        }
        awaitDispose {
            (value as? Ready)?.player?.releaseIfAvailable()
            value = Loading
        }
    }

/**
 * Tracks the given [player]'s current position and duration.
 * Updates are event-driven while the player is active, and polling resumes
 * at [POSITION_POLL_INTERVAL_MS] intervals during playback.
 * Returns [Duration.ZERO] for both when [player] is null.
 */
@Composable
internal fun rememberPositionAndDuration(player: Player?): State<Pair<Duration, Duration>> =
    produceState(initialValue = Duration.ZERO to Duration.ZERO, player) {
        if (player == null) return@produceState
        val listener = progressUpdateListener { value = it }

        player.addListener(listener)
        try {
            pollPositionAndDuration(
                player = player,
                shouldContinue = { isActive },
                onProgressChanged = { value = it }
            )
        } finally {
            player.removeListener(listener)
        }
    }

private suspend fun pollPositionAndDuration(
    player: Player,
    shouldContinue: () -> Boolean,
    onProgressChanged: (Pair<Duration, Duration>) -> Unit,
) {
    onProgressChanged(player.positionAndDuration())
    while (shouldContinue()) {
        if (!player.isPlaying) {
            player.awaitPlaying()
        }
        // Guard the window between awaitPlaying() returning and delay()'s own cancellation point.
        if (shouldContinue()) {
            delay(POSITION_POLL_INTERVAL_MS)
            onProgressChanged(player.positionAndDuration())
        }
    }
}

private fun progressUpdateListener(
    onProgressChanged: (Pair<Duration, Duration>) -> Unit,
): Player.Listener =
    object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.hasRelevantProgressUpdateEvent()) {
                onProgressChanged(player.positionAndDuration())
            }
        }
    }

private fun Player.Events.hasRelevantProgressUpdateEvent(): Boolean =
    PROGRESS_RELEVANT_PLAYER_EVENTS.any(::contains)

private fun Player.positionAndDuration(): Pair<Duration, Duration> {
    val position = currentPosition.milliseconds
    val rawDuration = duration
    val duration = if (rawDuration == C.TIME_UNSET) Duration.INFINITE else rawDuration.milliseconds
    return position to duration
}

private suspend fun Player.awaitPlaying() {
    if (isPlaying) return
    suspendCancellableCoroutine { continuation ->
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying && continuation.isActive) {
                    removeListener(this)
                    continuation.resume(Unit)
                }
            }
        }
        addListener(listener)

        // Cover the race where playback starts between the pre-check and listener registration.
        if (isPlaying && continuation.isActive) {
            removeListener(listener)
            continuation.resume(Unit)
            return@suspendCancellableCoroutine
        }

        continuation.invokeOnCancellation { removeListener(listener) }
    }
}
