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

import android.net.Uri
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.media3.common.Player
import androidx.media3.common.util.Util
import com.nice.cxonechat.ui.composable.generic.AudioPlayerState.Loading
import com.nice.cxonechat.ui.composable.generic.AudioPlayerState.Ready
import com.nice.cxonechat.ui.composable.player.PlaceholderState
import com.nice.cxonechat.ui.composable.player.produceAudioPlayerState
import com.nice.cxonechat.ui.composable.player.rememberPlayerState
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
 * The audio is preset to pause if the composable lifecycle owner is paused while it is playing and it will resume to
 * play when the lifecycle owner is resumed iff it was playing previously.
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
        playerState = PlaceholderState,
        modifier = modifier,
    )
}

@Composable
private fun AudioPlayerView(
    player: Player,
    modifier: Modifier = Modifier,
) {
    val playerState = rememberPlayerState(player)
    val currentTime by remember(playerState) {
        derivedStateOf {
            playerState.position.value.toTimeStamp(Locale.current)
        }
    }
    val remainingTime by remember(playerState) {
        derivedStateOf {
            (playerState.duration.value - playerState.position.value)
                .toTimeStamp(Locale.current)
        }
    }
    val animatedProgress by animateFloatAsState(
        targetValue = playerState.progress.value,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    )
    AudioPlayerContent(
        currentTime = currentTime,
        animatedProgress = animatedProgress,
        remainingTime = remainingTime,
        playerState = playerState,
        modifier = modifier,
        onSeekBack = remember(player) { { player.seekBack() } },
        onSeekForward = remember(player) { { player.seekForward() } },
        onPlayPause = remember(player) { { Util.handlePlayPauseButtonAction(player) } },
    )
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
