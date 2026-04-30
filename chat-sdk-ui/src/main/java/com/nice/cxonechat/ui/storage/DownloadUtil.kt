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
import androidx.annotation.VisibleForTesting
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.exoplayer.offline.DownloadManager
import com.nice.cxonechat.ui.composable.generic.ExoPlayerCacheHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Singleton holder for the Media3 [DownloadManager] used to pre-cache audio attachments.
 *
 * ## Lifecycle
 * The [DownloadManager] instance is created lazily on first access and lives for the
 * application lifetime. Media3's [DownloadManager] handles its own internal cleanup
 * gracefully when the process is killed — in-progress downloads are persisted to the
 * download index database and will resume on next launch.
 *
 * If explicit cleanup is required (e.g. during testing or low-memory conditions), call
 * [release] to stop active downloads and free resources. After [release], the next call
 * to [getDownloadManager] will create a fresh instance.
 *
 * ## Coroutine safety
 * Lazy initialisation uses a [Mutex] with a [Volatile] fast-path read to guarantee that
 * exactly one [DownloadManager] is created even under concurrent suspension. The function
 * must be called from a coroutine context; no blocking I/O is performed on the calling
 * thread.
 */
@UnstableApi
internal object DownloadUtil {

    @Volatile
    private var downloadManager: DownloadManager? = null

    private val mutex = Mutex()

    /**
     * Returns the singleton [DownloadManager], creating it on first call.
     *
     * The [Volatile] read provides a lock-free fast path once the instance is
     * initialised. The [Mutex] guards the first-time creation so that only one
     * instance is ever constructed, without blocking a thread.
     */
    suspend fun getDownloadManager(context: Context): DownloadManager =
        downloadManager ?: mutex.withLock {
            downloadManager ?: DownloadManager(
                context,
                getDatabaseProvider(context),
                getDownloadCache(context),
                DefaultHttpDataSource.Factory(),
                Dispatchers.IO.asExecutor()
            ).also {
                downloadManager = it
            }
        }

    /**
     * Releases the current [DownloadManager] instance and clears the singleton reference.
     *
     * After this call, the next invocation of [getDownloadManager] will create a fresh
     * instance. Uses the same [Mutex] as [getDownloadManager] to prevent a caller from
     * obtaining a reference to an already-released instance.
     *
     * This singleton is process-scoped, so [release] should only be called from a
     * process-level lifecycle observer (e.g. `ProcessLifecycleOwner`) — not from an
     * individual [android.app.Activity], which can be destroyed and re-created multiple
     * times within the same process.
     */
    suspend fun release() {
        mutex.withLock {
            downloadManager?.release()
            downloadManager = null
        }
    }

    private fun getDatabaseProvider(context: Context): DatabaseProvider =
        ExoPlayerCacheHolder.getDatabaseProvider(context)

    private suspend fun getDownloadCache(context: Context): Cache =
        ExoPlayerCacheHolder.getCache(context)

    /**
     * Resets the singleton reference without releasing the underlying [DownloadManager].
     * For use in tests only — allows tests to start from a clean state.
     * Uses the same [Mutex] as [getDownloadManager] and [release] to prevent concurrent
     * access during reset.
     */
    @VisibleForTesting
    internal suspend fun resetForTesting() {
        mutex.withLock {
            downloadManager = null
        }
    }
}
