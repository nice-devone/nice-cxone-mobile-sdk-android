/*
 * Copyright (c) 2021-2023. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.internal

import com.nice.cxonechat.Authorization
import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.ChatBuilder
import com.nice.cxonechat.ChatBuilder.OnChatBuiltCallback
import com.nice.cxonechat.ChatStateListener
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.duration
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.severe

internal class ChatBuilderLogging(
    private val origin: ChatBuilder,
    entrails: ChatEntrails,
) : ChatBuilder, LoggerScope by LoggerScope<ChatBuilder>(entrails.logger) {

    private var developmentMode: Boolean = false

    override fun setAuthorization(authorization: Authorization) = scope("setAuthorization") {
        origin.setAuthorization(authorization)
    }

    override fun setDevelopmentMode(enabled: Boolean) = scope("setDevelopmentMode") {
        this@ChatBuilderLogging.developmentMode = enabled
        origin.setDevelopmentMode(enabled)
    }

    override fun setUserName(first: String, last: String) = scope("setUserName") {
        origin.setUserName(first, last)
    }

    override fun setChatStateListener(listener: ChatStateListener): ChatBuilder = scope("setChatStateListener") {
        origin.setChatStateListener(listener)
    }

    override fun setDeviceToken(token: String): ChatBuilder = scope("setDeviceToken") {
        origin.setDeviceToken(token)
    }

    override fun build(callback: OnChatBuiltCallback): Cancellable = scope("build") {
        return try {
            duration { origin.build(callback) }
        } catch (expected: Throwable) {
            if (developmentMode) severe("Failed to initialize", expected)
            throw expected
        }
    }
}
