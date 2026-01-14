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

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSink
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.ExoPlayer.Builder
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.amr.AmrExtractor
import androidx.media3.extractor.mp3.Mp3Extractor
import androidx.media3.extractor.mp4.Mp4Extractor
import com.nice.cxonechat.utilities.TaggingSocketFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File

/**
 * Builds instance of [Player] and set it up using [DefaultDataSource] and [ProgressiveMediaSource].
 *
 * @param context A [Context] for [ExoPlayer.Builder] instance.
 * @param uri The [Uri] for creation of [MediaItem].
 * @param buildUpon A lambda for additional setup of [ExoPlayer.Builder].
 */
@OptIn(UnstableApi::class)
internal suspend fun buildProgressivePlayerForUri(
    context: Context,
    uri: Uri,
    buildUpon: Builder.() -> Builder = { this },
): ExoPlayer =
    Builder(context)
        .setMediaSourceFactory(
            mediaSourceFactory(context)
        )
        .buildUpon()
        .build()
        .apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
        }

@OptIn(UnstableApi::class)
private suspend fun mediaSourceFactory(context: Context): MediaSource.Factory =
    DefaultMediaSourceFactory(
        /* dataSourceFactory = */
        getCacheDataSourceFactory(context),
        /* extractorsFactory = */
        DefaultExtractorsFactory()
            .setAmrExtractorFlags(AmrExtractor.FLAG_ENABLE_INDEX_SEEKING)
            .setMp3ExtractorFlags(Mp3Extractor.FLAG_ENABLE_INDEX_SEEKING)
            .setMp4ExtractorFlags(
                Mp4Extractor.FLAG_READ_WITHIN_GOP_SAMPLE_DEPENDENCIES_H265
                        or Mp4Extractor.FLAG_READ_WITHIN_GOP_SAMPLE_DEPENDENCIES
                        or Mp4Extractor.FLAG_READ_SEF_DATA
            )
            .setConstantBitrateSeekingEnabled(true)
    )

@OptIn(UnstableApi::class)
private suspend fun getCacheDataSourceFactory(context: Context): DataSource.Factory {
    val httpDataSource = withContext(Dispatchers.IO) {
        OkHttpDataSource.Factory(
            OkHttpClient.Builder()
                .socketFactory(TaggingSocketFactory)
                .build()
        )
    }
    // Configure the DataSource.Factory with the cache and factory for the desired HTTP stack.
    val cache = ExoPlayerCacheHolder.getCache(context.applicationContext)
    val cacheDataSourceFactory =
        CacheDataSource.Factory()
            .setCache(cache)
            .setCacheWriteDataSinkFactory(CacheDataSink.Factory().setCache(cache))
            .setUpstreamDataSourceFactory(httpDataSource)
    val dataSourceFactory = DefaultDataSource.Factory(
        context,
        cacheDataSourceFactory
    )
    return dataSourceFactory
}

@UnstableApi
internal object ExoPlayerCacheHolder {
    private const val MAX_BYTES = 100 * 1024 * 1024L // 100 MB

    // These objects must be singletons
    @Volatile
    private var databaseProvider: DatabaseProvider? = null

    @Volatile
    private var cache: SimpleCache? = null

    internal fun getDatabaseProvider(context: Context): DatabaseProvider =
        databaseProvider ?: StandaloneDatabaseProvider(context).also { databaseProvider = it }

    private fun getCacheDir(context: Context) = File(context.cacheDir, "media3").apply { mkdirs() }

    suspend fun getCache(context: Context): SimpleCache = withContext(Dispatchers.IO) {
        synchronized(ExoPlayerCacheHolder) {
            cache ?: SimpleCache(
                getCacheDir(context),
                LeastRecentlyUsedCacheEvictor(MAX_BYTES),
                getDatabaseProvider(context)
            ).also { cache = it }
        }
    }
}

internal fun Player.releaseIfAvailable() {
    if (isCommandAvailable(Player.COMMAND_RELEASE)) {
        release()
    }
}
