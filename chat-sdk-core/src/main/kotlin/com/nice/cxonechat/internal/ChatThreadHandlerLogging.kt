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

import com.nice.cxonechat.ChatThreadActionHandler
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.duration
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.verbose
import com.nice.cxonechat.thread.ChatThread
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

internal class ChatThreadHandlerLogging(
    private val origin: ChatThreadHandler,
    logger: Logger,
) : ChatThreadHandler, LoggerScope by LoggerScope<ChatThreadHandler>(logger) {

    private val messagesHandler by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        scope("messages") {
            ChatThreadMessageHandlerLogging(origin.messages(), identity)
        }
    }

    private val eventsHandler by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        scope("events") {
            ChatThreadEventHandlerLogging(origin.events(), identity)
        }
    }

    private val actionsHandler: ChatThreadActionHandler by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        scope("actions") {
            ChatThreadActionHandlerLogging(origin.actions(), identity)
        }
    }

    private val customFieldsHandler by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        scope("customFields") {
            ChatFieldHandlerLogging(origin.customFields(), identity)
        }
    }

    override val threadFlow: Flow<ChatThread> = origin.threadFlow.onEach { thread ->
        scope("onThreadUpdated") {
            verbose("thread=$thread")
        }
    }

    override fun get() = scope("get") {
        duration {
            origin.get()
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

    override suspend fun archive() = scope("archive") {
        duration {
            origin.archive()
        }
    }

    override fun messages() = messagesHandler

    override fun events() = eventsHandler

    override fun actions(): ChatThreadActionHandler = actionsHandler

    override fun customFields() = customFieldsHandler

    override fun endContact() = scope("endContact") {
        duration {
            origin.endContact()
        }
    }
}
