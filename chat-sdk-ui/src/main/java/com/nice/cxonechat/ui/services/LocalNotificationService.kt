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

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.timedScope
import com.nice.cxonechat.log.verbose
import com.nice.cxonechat.ui.UiModule
import com.nice.cxonechat.ui.services.LocalNotificationService.Companion.ACTION_DISMISS_NOTIFICATION
import com.nice.cxonechat.ui.storage.ValueStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.get
import org.koin.android.ext.android.inject
import org.koin.core.qualifier.named
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * A Service that handles notification dismissal actions ([ACTION_DISMISS_NOTIFICATION]).
 * It processes intents to dismiss notifications and stores dismissed notification IDs.
 */
internal class LocalNotificationService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private val invalidCommandScope = CoroutineScope(Dispatchers.Default + serviceJob)

    /** Thread-safe queue to store startId values in the order they are received. */
    private val startIdQueue = ConcurrentLinkedQueue<Int>()

    /** Mutex to ensure thread-safe access to the queue. */
    private val mutex = Mutex()

    /** Storage for managing dismissed notification IDs. */
    private val valueStorage: ValueStorage by inject()

    private val logger by lazy { LoggerScope(TAG, get(named(UiModule.loggerName))) }

    /**
     * This service does not support binding.
     */
    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException("Called service is non-binding")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel("Service destroyed")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = logger.scope("onStartCommand") {
        startIdQueue.add(startId)
        val action = intent?.action
        if (ACTION_DISMISS_NOTIFICATION == action) {
            storeIdOfNotification(intent)
        } else {
            invalidCommandScope.launch { cleanup() }
        }
        return START_REDELIVER_INTENT
    }

    /**
     * Stores the notification ID and processes it in the background.
     *
     * @param intent The intent containing the notification ID to be dismissed.
     */
    private fun LoggerScope.storeIdOfNotification(intent: Intent) = scope("storeIdOfNotification") {
        val notificationId = intent.getStringExtra(KEY_NOTIFICATION_DISMISS_ID)
        if (notificationId != null) {
            serviceScope.launch {
                timedScope("storeIdOfNotification_OnBackground") {
                    verbose("Storing dismissed notification ID: $notificationId")
                    try {
                        valueStorage.addStringToSet(ValueStorage.StringSetKey.DismissedNotificationsKey, notificationId)
                    } finally {
                        cleanup()
                    }
                }
            }
        }
    }

    /**
     * Ensures that `stopSelf` is called in the correct order using a mutex and a queue.
     */
    private suspend fun LoggerScope.cleanup() = scope("cleanup") {
        withContext(Dispatchers.Unconfined) {
            mutex.withLock {
                val nextStartId = startIdQueue.poll() ?: return@withLock
                if (stopSelfResult(nextStartId)) {
                    verbose("Stopping service.")
                }
            }
        }
    }

    internal companion object {
        private const val TAG = "LocalNotificationService"

        /** Action string for dismissing a notification. */
        private const val ACTION_DISMISS_NOTIFICATION = "com.nice.cxonechat.ui.action.DISMISS_NOTIFICATION"

        /** Key for retrieving the notification dismiss ID from the intent. */
        private const val KEY_NOTIFICATION_DISMISS_ID = "notification_dismiss_id"

        /**
         * Builds an intent to dismiss a notification.
         *
         * @param context The context to use for creating the intent.
         * @param dismissId The ID of the notification to be dismissed.
         * @return The intent to start the service with the dismiss action.
         */
        fun buildDismissIntent(context: Context, dismissId: String): Intent {
            return Intent(context, LocalNotificationService::class.java).apply {
                setAction(ACTION_DISMISS_NOTIFICATION)
                putExtra(KEY_NOTIFICATION_DISMISS_ID, dismissId)
            }
        }
    }
}
