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

import com.nice.cxonechat.Chat
import com.nice.cxonechat.ChatThreadsHandler
import com.nice.cxonechat.internal.socket.EventLogger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.duration
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.verbose
import kotlinx.coroutines.runBlocking

internal class ChatLogging(
    private val origin: ChatWithParameters,
) : ChatWithParameters by origin, LoggerScope by LoggerScope<Chat>(origin.entrails.logger) {

    init {
        verbose("Initialized (config=$configuration,environment=$environment)")
        origin.socketListener.addListener(EventLogger(identity))
    }

    private val threadsHandler: ChatThreadsHandler by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        scope("threads") {
            ChatThreadsHandlerLogging(origin.threads(), identity)
        }
    }

    private val eventsHandler by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        scope("events") {
            ChatEventHandlerLogging(origin.events(), identity)
        }
    }

    private val customFieldsHandler by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        scope("customFields") {
            ChatFieldHandlerLogging(origin.customFields(), identity)
        }
    }

    private val actionsHandler by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        scope("actions") {
            ChatActionHandlerLogging(origin.actions(), identity)
        }
    }

    override fun setDeviceToken(token: String?) = scope("setDeviceToken") {
        duration {
            verbose("token=${token ?: "null"}")
            origin.setDeviceToken(token)
        }
    }

    override suspend fun signOut() = scope("signOut") {
        duration {
            origin.signOut()
        }
    }

    override fun threads(): ChatThreadsHandler = threadsHandler

    override fun events() = eventsHandler

    override fun customFields() = customFieldsHandler

    override fun actions() = actionsHandler

    override suspend fun connect() = scope("connect") {
        duration {
            origin.connect()
        }
    }

    override fun close(): Unit = runBlocking { closeSuspending() }

    override suspend fun closeSuspending() = scope("closeSuspending") {
        duration {
            origin.closeSuspending()
        }
    }
}
