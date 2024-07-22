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

package com.nice.cxonechat.ui.util

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.LegacyPlayerControlView

/**
 * Prepare an [ExoPlayer] to play the [mediaItem].
 *
 * @receiver [LegacyPlayerControlView] in need of an [ExoPlayer] to
 * play the media item.
 * @param mediaItem Prepare an ExoPlayer for this media item.
 */
@OptIn(UnstableApi::class)
fun LegacyPlayerControlView.preparePlayer(mediaItem: MediaItem) {
    val exoPlayer = ExoPlayer.Builder(context).build()
    exoPlayer.setMediaItem(mediaItem)
    exoPlayer.prepare()
    player = exoPlayer
}

/**
 * Release and destroy an [ExoPlayer] previously attached to this control view.
 *
 * @receiver A LegacyPlayerControlView that has previously been assigned
 * a player that is now no longer needed.
 */
@OptIn(UnstableApi::class)
fun LegacyPlayerControlView.releasePlayer() {
    val exoPlayer = player ?: return
    exoPlayer.release()
    player = null
}
