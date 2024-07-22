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

import com.nice.cxonechat.Chat
import com.nice.cxonechat.internal.socket.EventLogger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.duration
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.verbose

internal class ChatLogging(
    private val origin: ChatWithParameters,
) : ChatWithParameters by origin, LoggerScope by LoggerScope<Chat>(origin.entrails.logger) {

    init {
        verbose("Initialized (config=$configuration,environment=$environment)")
        origin.socketListener.addListener(EventLogger(identity))
    }

    override fun setDeviceToken(token: String?) = scope("setDeviceToken") {
        duration {
            verbose("token=${token ?: "null"}")
            origin.setDeviceToken(token)
        }
    }

    override fun signOut() = scope("signOut") {
        duration {
            origin.signOut()
        }
    }

    override fun threads() = scope("threads") {
        duration {
            var handler = origin.threads()
            handler = ChatThreadsHandlerLogging(handler, identity)
            handler
        }
    }

    override fun events() = scope("events") {
        duration {
            var handler = origin.events()
            handler = ChatEventHandlerLogging(handler, identity)
            handler
        }
    }

    override fun customFields() = scope("customFields") {
        duration {
            var handler = origin.customFields()
            handler = ChatFieldHandlerLogging(handler, identity)
            handler
        }
    }

    override fun actions() = scope("actions") {
        duration {
            var handler = origin.actions()
            handler = ChatActionHandlerLogging(handler, identity)
            handler
        }
    }

    override fun connect() = scope("connect") {
        duration {
            origin.connect()
        }
    }

    override fun close() = scope("close") {
        duration {
            origin.close()
        }
    }
}
