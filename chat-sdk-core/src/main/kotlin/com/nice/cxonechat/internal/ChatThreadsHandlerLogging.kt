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

import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadsHandler
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.duration
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.verbose
import com.nice.cxonechat.prechat.PreChatSurvey
import com.nice.cxonechat.prechat.PreChatSurveyResponse
import com.nice.cxonechat.state.FieldDefinition
import com.nice.cxonechat.thread.ChatThread
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

internal class ChatThreadsHandlerLogging(
    private val origin: ChatThreadsHandler,
    logger: Logger,
) : ChatThreadsHandler, LoggerScope by LoggerScope<ChatThreadsHandler>(logger) {

    private val threadHandlersMemoized = ConcurrentHashMap<UUID, ChatThreadHandler>()

    override val preChatSurvey: PreChatSurvey?
        get() = scope("preChatSurvey") {
            duration {
                origin.preChatSurvey
            }
        }

    override val threadsFlow: Flow<List<ChatThread>> = origin.threadsFlow.onEach { threads ->
        scope("onThreadsUpdated") {
            verbose("threads(${threads.size})")
        }
        threadHandlersMemoized.keys.retainAll(
            threads.mapTo(HashSet(), ChatThread::id)
        )
    }

    override fun refresh() = scope("refresh") {
        duration {
            origin.refresh()
        }
    }

    override fun create(
        customFields: Map<String, String>,
        preChatSurveyResponse: Sequence<PreChatSurveyResponse<out FieldDefinition, out Any>>,
    ) = scope("create") {
        duration {
            val originHandler = origin.create(customFields, preChatSurveyResponse)
            // Read the id from the undecorated origin to avoid an extra logged get() on the wrapper.
            ChatThreadHandlerLogging(originHandler, identity)
                .also { threadHandlersMemoized[originHandler.get().id] = it }
        }
    }

    override fun thread(thread: ChatThread) = scope("thread") {
        duration {
            threadHandlersMemoized.computeIfAbsent(thread.id) {
                ChatThreadHandlerLogging(origin.thread(thread), identity)
            }
        }
    }
}
