/*
 * Copyright (c) 2021-2024. NICE Ltd. All rights reserved.
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons.Outlined
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.FastRewind
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.LegacyPlayerControlView
import com.nice.cxonechat.ui.composable.theme.ChatTheme

/**
 * ExoPlayer wrapped in [AndroidView], suitable for audio playback.
 * ExoPlayer will be displayed using [LegacyPlayerControlView], so no visualization will be available.
 * The controller view will also hide the following buttons: previous, next, shuffle, vr, multiWindowTimeBar
 *
 * @param uri Uri which points to media content to be played.
 * @param modifier The modifier to be applied to the [AndroidView] layout.
 */
@Composable
@OptIn(UnstableApi::class)
internal fun AudioPlayer(uri: Uri, modifier: Modifier = Modifier) {
    if (LocalInspectionMode.current) {
        // ExoPlayer is not compatible with preview, using placeholder.
        InspectionPlaceholder(modifier)
    } else {
        val context = LocalContext.current

        val exoPlayer = remember {
            buildProgressivePlayerForUri(context, uri)
        }

        AndroidView(
            factory = { viewContext -> playerFactory(viewContext, exoPlayer) },
            modifier = modifier,
            onRelease = { controlView -> controlView.player?.release() },
            update = { controlView -> controlView.updatePlayer(exoPlayer) },
        )
    }
}

@OptIn(UnstableApi::class)
private fun LegacyPlayerControlView.updatePlayer(exoPlayer: ExoPlayer) {
    val currentPlayer = player
    if (currentPlayer != exoPlayer) {
        currentPlayer?.release()
        player = exoPlayer
    }
}

@OptIn(UnstableApi::class)
private fun playerFactory(context: Context, exoPlayer: ExoPlayer) =
    LegacyPlayerControlView(context).apply {
        showTimeoutMs = -1
        player = exoPlayer
        layoutParams = FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        showShuffleButton = false
        showVrButton = false
        setShowPreviousButton(false)
        setShowNextButton(false)
        setShowMultiWindowTimeBar(false)
        setShowRewindButton(true)
        setShowFastForwardButton(true)
    }

@Preview
@Composable
private fun InspectionPlaceholder(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        border = BorderStroke(1.dp, ChatTheme.colors.onSurface.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .width(320.dp)
                .height(80.dp)
        ) {
            LinearProgressIndicator(
                progress = 0.5f,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
                IconButton(onClick = {}, modifier = Modifier.fillMaxHeight()) {
                    Icon(Outlined.FastRewind, null)
                }
                IconButton(onClick = {}, modifier = Modifier.fillMaxHeight()) {
                    Icon(Outlined.PlayCircleOutline, null)
                }
                IconButton(onClick = {}, modifier = Modifier.fillMaxHeight()) {
                    Icon(Outlined.FastForward, null)
                }
            }
        }
    }
}
