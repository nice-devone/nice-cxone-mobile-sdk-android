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

package com.nice.cxonechat.ui.storage

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import com.nice.cxonechat.ui.composable.generic.ExoPlayerCacheHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking

@UnstableApi
internal object DownloadUtil {

    private const val CHANNEL_ID = "download_channel"

    @Volatile
    private var downloadManager: DownloadManager? = null

    @Volatile
    private var downloadNotificationHelper: DownloadNotificationHelper? = null

    fun getDownloadManager(context: Context): DownloadManager =
        downloadManager ?: synchronized(DownloadUtil) {
            DownloadManager(
                context,
                getDatabaseProvider(context),
                getDownloadCache(context),
                DefaultHttpDataSource.Factory(),
                Dispatchers.IO.asExecutor()
            ).also {
                downloadManager = it
            }
        }

    fun getDownloadNotificationHelper(context: Context): DownloadNotificationHelper {
        return downloadNotificationHelper ?: synchronized(DownloadUtil) {
            DownloadNotificationHelper(context, CHANNEL_ID).also {
                downloadNotificationHelper = it
            }
        }
    }

    private fun getDatabaseProvider(context: Context): DatabaseProvider =
        ExoPlayerCacheHolder.getDatabaseProvider(context)

    private fun getDownloadCache(context: Context): Cache =
        runBlocking { ExoPlayerCacheHolder.getCache(context) }
}
