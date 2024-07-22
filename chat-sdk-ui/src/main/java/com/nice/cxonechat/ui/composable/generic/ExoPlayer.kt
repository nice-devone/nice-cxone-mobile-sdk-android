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
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource.Factory
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.ExoPlayer.Builder
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.nice.cxonechat.utilities.TaggingSocketFactory
import okhttp3.OkHttpClient

/**
 * Builds instance of [ExoPlayer] and set it up using [DefaultDataSource] and [ProgressiveMediaSource].
 *
 * @param context A [Context] for [ExoPlayer.Builder] instance.
 * @param uri The [Uri] for creation of [MediaItem].
 */
@OptIn(UnstableApi::class)
internal fun buildProgressivePlayerForUri(context: Context, uri: Uri): ExoPlayer =
    Builder(context)
        .build()
        .apply {
            val okHttpDataSource = OkHttpDataSource.Factory(
                OkHttpClient.Builder()
                    .socketFactory(TaggingSocketFactory)
                    .build()
            )
            val dataSourceFactory: Factory = DefaultDataSource.Factory(
                context,
                okHttpDataSource
            )
            val source = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(uri))
            setMediaSource(source)
            prepare()
        }
