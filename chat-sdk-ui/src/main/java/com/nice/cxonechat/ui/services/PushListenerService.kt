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

import android.app.ActivityManager
import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import androidx.annotation.Keep
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.nice.cxonechat.ChatInstanceProvider
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.debug
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.verbose
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.UiModule
import com.nice.cxonechat.ui.data.ChatNotificationManager
import com.nice.cxonechat.ui.domain.PushMessage
import com.nice.cxonechat.ui.domain.PushMessageParser
import com.nice.cxonechat.ui.screen.ChatActivity
import com.nice.cxonechat.ui.util.parseThreadDeeplink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named

@Keep
internal class PushListenerService : FirebaseMessagingService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private val chatProvider: ChatInstanceProvider by inject()

    private val parser: PushMessageParser by inject()

    private val notificationManager: ChatNotificationManager by inject()

    private val logger by lazy { LoggerScope(TAG, get(named(UiModule.LOGGER_NAME))) }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel("Service destroyed")
    }

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
        if (isChatActivityInForeground(applicationContext)) {
            debug("Application is in foreground, discarding push message")
        } else {
            val pushMessage = parser.parse(remoteMessage.data)
            sendNotification(pushMessage)
        }
    }

    private fun LoggerScope.sendNotification(message: PushMessage) = scope("sendNotification") {
        val iconResId = if (message.iconResId != 0) message.iconResId else R.drawable.ic_chat_push_service
        val context = applicationContext
        val deepLink = message.deepLink?.toUri()
        val pendingIntent = createContentIntent(deepLink, context)
        if (pendingIntent == null) {
            debug("No deep link provided, notification will not be clickable")
        }
        val id = deepLink.getId()
        serviceScope.launch {
            // lastMessageId is null so notification will always be sent
            notificationManager.sendMessageNotification(notificationId = id) {
                setContentIntent(pendingIntent)
                    .setSmallIcon(iconResId)
                    .setContentTitle(message.title)
                    .setContentText(message.message)
            }
        }
    }

    private fun isChatActivityInForeground(context: Context): Boolean {
        val activityManager = context.getSystemService(ActivityManager::class.java)
        val appTasks = activityManager.appTasks
        return appTasks.firstOrNull()?.taskInfo?.topActivity?.className == ChatActivity::class.java.name &&
                ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
    }

    private companion object {
        const val TAG: String = "PushListenerService"
        const val DEFAULT_ID = "pushMessage"
        private fun Uri?.getId(): String = this?.parseThreadDeeplink()?.getOrNull()?.toString() ?: DEFAULT_ID

        private fun createContentIntent(
            deepLink: Uri?,
            context: Context,
        ): PendingIntent? = deepLink?.let { uri ->
            PendingIntent.getActivity(
                context,
                0,
                ChatActivity.buildOpenThreadIntent(context, uri),
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
