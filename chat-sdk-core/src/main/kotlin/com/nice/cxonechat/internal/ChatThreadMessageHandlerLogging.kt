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

package com.nice.cxonechat.internal

import com.nice.cxonechat.ChatThreadMessageHandler
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.duration
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.verbose
import com.nice.cxonechat.message.OutboundMessage

internal class ChatThreadMessageHandlerLogging(
    private val origin: ChatThreadMessageHandler,
    logger: Logger,
) : ChatThreadMessageHandler, LoggerScope by LoggerScope<ChatThreadMessageHandler>(logger) {

    override fun loadMore() = scope("loadMore") {
        duration {
            origin.loadMore()
        }
    }

    override suspend fun send(message: OutboundMessage): String = scope("send(${message.hashCode()})") {
        verbose("(message=${message.message},attachments=${message.attachments},postback=${message.postback})")
        val id = origin.send(message)
        verbose("sent id=$id")
        id
    }
}
