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

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize.Min
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.composable.generic.AudioPlayerState.Loading
import com.nice.cxonechat.ui.composable.generic.AudioPlayerState.Ready
import com.nice.cxonechat.ui.composable.player.PlaceholderState
import com.nice.cxonechat.ui.composable.player.rememberPlayerState
import com.nice.cxonechat.ui.composable.theme.ChatTheme
import com.nice.cxonechat.ui.composable.theme.ChatTheme.chatShapes
import com.nice.cxonechat.ui.composable.theme.ChatTheme.space

@OptIn(UnstableApi::class)
@Composable
internal fun AudioPlayerButton(
    playerResult: AudioPlayerState,
    modifier: Modifier = Modifier,
) {
    val playPause: () -> Unit = remember(playerResult) {
        {
            (playerResult as? Ready)?.player?.let(Util::handlePlayPauseButtonAction)
        }
    }
    val isReady = playerResult is Ready
    val onClick: () -> Unit = {
        if (isReady) playPause()
    }
    val state = when (playerResult) {
        Loading -> PlaceholderState
        is Ready -> rememberPlayerState(playerResult.player)
    }
    PlayerButton(modifier, onClick, isReady, state.isPlaying.value)
}

@Composable
private fun PlayerButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    isReady: Boolean,
    isPlaying: Boolean,
) {
    FilledIconButton(
        modifier = modifier,
        shape = chatShapes.actionButtonShape,
        onClick = onClick,
        enabled = isReady,
    ) {
        AnimatedContent(isPlaying) { playArrow ->
            when (playArrow) {
                true -> Icon(Icons.Default.Stop, contentDescription = stringResource(R.string.record_audio_stop_content_description))
                false -> Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.recording_audio_preview_message))
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun PlayerButtonPreview() {
    ChatTheme {
        Surface {
            var ready by remember { mutableStateOf(true) }
            var playing by remember { mutableStateOf(false) }
            Column(
                modifier = Modifier
                    .padding(space.small)
                    .width(Min)
            ) {
                PlayerButton(
                    onClick = {
                        playing = !playing
                    },
                    isReady = ready,
                    isPlaying = playing,
                )
                HorizontalDivider()
                Row(
                    horizontalArrangement = spacedBy(space = space.medium),
                    verticalAlignment = CenterVertically,
                ) {
                    Switch(ready, { ready = it })
                    Text("Ready")
                }
            }
        }
    }
}
