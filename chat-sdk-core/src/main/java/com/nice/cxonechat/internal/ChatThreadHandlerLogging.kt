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

import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadHandler.OnThreadUpdatedListener
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.duration
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.verbose

internal class ChatThreadHandlerLogging(
    private val origin: ChatThreadHandler,
    logger: Logger,
) : ChatThreadHandler, LoggerScope by LoggerScope<ChatThreadHandler>(logger) {

    override fun get() = scope("get") {
        duration {
            origin.get()
        }
    }

    override fun get(listener: OnThreadUpdatedListener) = scope("get") {
        verbose("Registered")
        origin.get {
            duration {
                listener.onUpdated(it)
            }
        }
    }

    override fun setName(name: String) = scope("setName") {
        duration {
            origin.setName(name)
        }
    }

    override fun refresh() = scope("refresh") {
        duration {
            origin.refresh()
        }
    }

    override fun archive(onComplete: (Boolean) -> Unit) = scope("archive") {
        duration {
            origin.archive(onComplete)
        }
    }

    override fun messages() = scope("messages") {
        duration {
            var handler = origin.messages()
            handler = ChatThreadMessageHandlerLogging(handler, identity)
            handler
        }
    }

    override fun events() = scope("events") {
        duration {
            var handler = origin.events()
            handler = ChatThreadEventHandlerLogging(handler, identity)
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

    override fun endContact() = scope("endContact") {
        duration {
            origin.endContact()
        }
    }
}
