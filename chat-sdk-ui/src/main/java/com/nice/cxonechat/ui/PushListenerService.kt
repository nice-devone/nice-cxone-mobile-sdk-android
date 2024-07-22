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

package com.nice.cxonechat.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat.Builder
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.nice.cxonechat.ChatInstanceProvider
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.debug
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.verbose
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.domain.PushMessage
import com.nice.cxonechat.ui.domain.PushMessageParser
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named

internal class PushListenerService : FirebaseMessagingService() {

    private val chatProvider: ChatInstanceProvider by inject()

    private val parser: PushMessageParser by inject()

    private val logger by lazy { LoggerScope(TAG, get(named(UiModule.loggerName))) }

    override fun onNewToken(token: String) = logger.scope("onNewToken") {
        super.onNewToken(token)
        val chat = chatProvider.chat
        if (chat == null) {
            verbose("No chat instance present, token not passed")
            return@scope
        }
        chat.setDeviceToken(token)
        debug("Registering push notifications token: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage): Unit = logger.scope("onMessageReceived") {
        super.onMessageReceived(remoteMessage)
        debug("Received push message: " + remoteMessage.data)
        if (isAppInForeground()) {
            debug("Application is in foreground, discarding push message")
        } else {
            val pushMessage = parser.parse(remoteMessage.data)
            sendNotification(pushMessage)
        }
    }

    private fun isAppInForeground() =
        ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)

    private fun sendNotification(message: PushMessage) {
        val channelId = getString(string.default_notification_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val iconResId = if (message.iconResId == 0) R.mipmap.ic_launcher else message.iconResId

        val notificationBuilder = Builder(this, channelId)
            .setSmallIcon(iconResId)
            .setContentTitle(message.title)
            .setContentText(message.message)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(2)

        val pendingIntent = message.deepLink?.let {
            PendingIntent.getActivity(
                this,
                0,
                Intent(Intent.ACTION_VIEW, Uri.parse(it)),
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        if (pendingIntent != null) {
            pendingIntent.also(notificationBuilder::setContentIntent)
        } else {
            logger.debug("No deep link provided, notification will not be clickable")
        }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Since android Oreo notification channel is needed.
            val channel = NotificationChannel(
                channelId,
                getString(string.notification_channel_title),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }
        val notification = notificationBuilder.build()
        notificationManager.notify(
            0, // ID of notification
            notification
        )
    }

    companion object {
        val TAG: String = PushListenerService::class.java.simpleName
    }
}
