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

package com.nice.cxonechat.ui.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.Builder
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.debug
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.warning
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.UiModule
import com.nice.cxonechat.ui.data.ChatNotificationManager.Companion.CHAT_GROUP
import com.nice.cxonechat.ui.storage.ValueStorage
import com.nice.cxonechat.ui.storage.ValueStorage.StringSetKey.DismissedNotificationsKey
import kotlinx.coroutines.flow.firstOrNull
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.incrementAndFetch

/**
 * Manages posting of notifications based on supplied id.
 * Provides a convenient way to create notifications for chat messages.
 */
@Single
internal class ChatNotificationManager(
    private val context: Context,
    private val valueStorage: ValueStorage,
    @Named(UiModule.LOGGER_NAME) logger: Logger,
) : LoggerScope by LoggerScope(TAG, logger) {
    @OptIn(ExperimentalAtomicApi::class)
    private val counter = AtomicInt(0)
    private val idMap = mutableMapOf<String, Int>()

    /**
     * Sends a notification which will contain a chat message for the user.
     *
     * The [Builder] will be preset with the following flags:
     * - Auto cancel
     * - Sound
     * - Category message
     * - High priority
     * - Private visibility
     * - Grouped under [CHAT_GROUP]
     *
     * You can override any of these flags or set others by using the [setupNotification] lambda,
     * but you have to set notification ContentTitle and ContentText.
     *
     * @param notificationId An id of the notification, it should be unique for each thread, usually message Id.
     * @param lastMessageId Id of the last message in the thread, used to determine if the notification should be skipped.
     * If the id is null, the notification will always be sent. The default value is null.
     * @param setupNotification A lambda that will be called with a [Builder] instance to finish the notification setup.
     */
    suspend fun sendMessageNotification(
        notificationId: NotificationId,
        lastMessageId: NotificationId? = null,
        setupNotification: Builder.() -> Builder,
    ) = scope("sendNotification") {
        if (lastMessageId != null && skipDismissed(notificationId, lastMessageId)) {
            debug("Notification with last message id: $lastMessageId was dismissed, skipping")
            return@scope
        }
        val channelId = context.getString(string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = Builder(context, channelId)
            .setExtras(
                Bundle().apply {
                    putString(
                        EXTRA_NOTIFICATION_ID,
                        notificationId
                    )
                }
            )
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setGroup(CHAT_GROUP)

        notificationBuilder.setupNotification()

        runCatching {
            val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Since android Oreo notification channel is needed.
                val channel = NotificationChannel(
                    channelId,
                    context.getString(string.notification_channel_title),
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(channel)
            }
            val notification = notificationBuilder.build()
            notificationManager.notify(
                notificationId.idInt(), // ID of notification
                notification
            )
        }.onFailure {
            warning("Failed to send notification with id: $notificationId", it)
        }
    }

    private suspend fun skipDismissed(notificationId: NotificationId, lastMessageId: String): Boolean =
        valueStorage.getStringSet(DismissedNotificationsKey)
            .firstOrNull()
            .orEmpty()
            .contains("$notificationId:$lastMessageId")

    @OptIn(ExperimentalAtomicApi::class)
    private fun NotificationId.idInt(): Int =
        idMap.getOrPut(this, counter::incrementAndFetch)

    private companion object {
        const val TAG = "NotificationManager"
        const val CHAT_GROUP = "com.nice.cxonechat.CHAT_GROUP"
        const val EXTRA_NOTIFICATION_ID = "com.nice.cxonechat.NOTIFICATION_ID"
    }
}

internal typealias NotificationId = String
