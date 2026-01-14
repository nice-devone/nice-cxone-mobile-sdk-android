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

package com.nice.cxonechat.sample

import android.content.Context
import androidx.startup.Initializer
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics
import com.google.firebase.crashlytics.setCustomKeys
import com.google.firebase.messaging.messaging
import com.nice.cxonechat.ChatInstanceProvider
import com.nice.cxonechat.log.LoggerAndroid
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.ProxyLogger
import com.nice.cxonechat.log.debug
import com.nice.cxonechat.log.duration
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.verbose
import com.nice.cxonechat.sample.data.models.ChatSettings
import com.nice.cxonechat.sample.data.repository.ChatSettingsRepository
import com.nice.cxonechat.sample.data.repository.UISettingsRepository
import com.nice.cxonechat.sample.utilities.FileLogger
import com.nice.cxonechat.sample.utilities.logging.FirebaseLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/** Automatic initialization of ChatInstanceProvider. */
class ChatInitializer :
    Initializer<ChatInstanceProvider>,
    LoggerScope by LoggerScope<ChatInitializer>(StoreApplication.sampleAppLogger) {

    override fun create(context: Context): ChatInstanceProvider = scope("create") {
        duration {
            /* set up the chat instance provider */
            val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            coroutineScope.launch {
                verbose("Started async loading of UI settings")
                UISettingsRepository(context, coroutineScope).load()
            }
            val settings: ChatSettings? = runBlocking {
                ChatSettingsRepository(context, coroutineScope).load()
            }
            verbose("Settings loaded, starting ChatInstanceProvider creation: $settings")
            Firebase.crashlytics.setCustomKeys {
                key("environment", settings?.sdkConfiguration?.environment?.name ?: "null")
                key("userName", settings?.userName?.fullName ?: "null")
                key("customerId", settings?.customerId ?: "null")
            }
            val provider = ChatInstanceProvider.create(
                configuration = settings?.sdkConfiguration?.asSocketFactoryConfiguration,
                authorization = null,
                userName = settings?.userName,
                developmentMode = true,
                deviceTokenProvider = { setToken ->
                    Firebase.messaging.token.addOnSuccessListener(setToken)
                },
                customerId = settings?.customerId,
                // SDK should use different logger then the app for readability and isolation
                logger = ProxyLogger(
                    FileLogger.getInstance(context),
                    FirebaseLogger(),
                    LoggerAndroid("CXoneChatSDK")
                )
            )
            debug("ChatInstanceProvider initialized")
            provider
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>?>?> = listOf(FileLogInitializer::class.java)
}
