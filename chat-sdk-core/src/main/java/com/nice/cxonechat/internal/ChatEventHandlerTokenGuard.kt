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

package com.nice.cxonechat.internal

import com.nice.cxonechat.ChatEventHandler
import com.nice.cxonechat.ChatEventHandler.OnEventErrorListener
import com.nice.cxonechat.ChatEventHandler.OnEventSentListener
import com.nice.cxonechat.event.ChatEvent
import com.nice.cxonechat.event.RefreshToken
import com.nice.cxonechat.util.expiresWithin
import java.util.Date
import kotlin.time.Duration.Companion.seconds

internal class ChatEventHandlerTokenGuard(
    private val origin: ChatEventHandler,
    private val chat: ChatWithParameters,
) : ChatEventHandler by origin {

    override fun trigger(event: ChatEvent, listener: OnEventSentListener?, errorListener: OnEventErrorListener?) {
        val expiresAt = chat.storage.authTokenExpDate ?: Date(Long.MAX_VALUE)
        if (expiresAt.expiresWithin(10.seconds) && event !is RefreshToken) {
            origin.trigger(RefreshToken)
        }
        origin.trigger(event, listener, errorListener)
    }
}
