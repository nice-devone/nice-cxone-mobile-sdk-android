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

package com.nice.cxonechat.sample

import android.content.Context
import androidx.startup.Initializer
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.ktx.messaging
import com.nice.cxonechat.ChatInstanceProvider
import com.nice.cxonechat.log.LoggerAndroid
import com.nice.cxonechat.log.ProxyLogger
import com.nice.cxonechat.sample.data.repository.ChatSettingsRepository
import com.nice.cxonechat.sample.data.repository.UISettingsRepository
import com.nice.cxonechat.sample.utilities.logging.FirebaseLogger

/** Automatic initialization of ChatInstanceProvider. */
class ChatInitializer : Initializer<ChatInstanceProvider> {
    override fun create(context: Context): ChatInstanceProvider {
        /* set up the chat instance provider */
        val settings = ChatSettingsRepository(context).load()
        UISettingsRepository(context).load()
        return ChatInstanceProvider.create(
            configuration = settings?.sdkConfiguration?.asSocketFactoryConfiguration,
            authorization = null,
            userName = settings?.userName,
            developmentMode = true,
            deviceTokenProvider = { setToken ->
                Firebase.messaging.token.addOnSuccessListener(setToken)
            },
            customerId = settings?.customerId,
            logger = ProxyLogger(
                FirebaseLogger(),
                LoggerAndroid("CXoneChat")
            )
        )
    }

    override fun dependencies() = emptyList<Class<out Initializer<*>>>()
}
