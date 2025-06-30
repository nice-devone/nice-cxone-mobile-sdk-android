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

package com.nice.cxonechat.ui.services

import android.Manifest.permission
import android.annotation.SuppressLint
import android.app.Notification
import androidx.annotation.RequiresPermission
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.PlatformScheduler
import androidx.media3.exoplayer.scheduler.Scheduler
import com.nice.cxonechat.ui.storage.DownloadUtil
import androidx.media3.exoplayer.R as exoplayerR
import androidx.media3.ui.R as media3R

/**
 * A service that manages downloads for media content in the player.
 * It extends [DownloadService] to handle download operations and notifications.
 *
 * This service is designed to run in the foreground and provides a notification
 * to inform users about ongoing downloads.
 */
@UnstableApi
internal class PlayerDownloadService : DownloadService(
    FOREGROUND_NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    CHANNEL_ID,
    exoplayerR.string.exo_download_notification_channel_name,
    0
) {

    override fun getDownloadManager(): DownloadManager = DownloadUtil.getDownloadManager(this)

    @RequiresPermission(permission.RECEIVE_BOOT_COMPLETED)
    override fun getScheduler(): Scheduler = PlatformScheduler(this, JOB_ID)

    @SuppressLint("PrivateResource")
    override fun getForegroundNotification(
        downloads: List<Download>,
        notMetRequirements: Int,
    ): Notification = DownloadUtil.getDownloadNotificationHelper(this)
        .buildProgressNotification(
            application,
            media3R.drawable.exo_notification_play,
            null,
            getString(exoplayerR.string.exo_download_downloading),
            downloads,
            notMetRequirements
        )

    companion object {
        private const val FOREGROUND_NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "download_channel"
        private const val JOB_ID = 1
    }
}
