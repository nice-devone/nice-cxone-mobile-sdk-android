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

import com.nice.cxonechat.ChatThreadsHandler
import com.nice.cxonechat.ChatThreadsHandler.OnThreadsUpdatedListener
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.duration
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.verbose
import com.nice.cxonechat.prechat.PreChatSurvey
import com.nice.cxonechat.prechat.PreChatSurveyResponse
import com.nice.cxonechat.state.FieldDefinition
import com.nice.cxonechat.thread.ChatThread

internal class ChatThreadsHandlerLogging(
    private val origin: ChatThreadsHandler,
    logger: Logger,
) : ChatThreadsHandler, LoggerScope by LoggerScope<ChatThreadsHandler>(logger) {
    override val preChatSurvey: PreChatSurvey?
        get() = scope("preChatSurvey") {
            duration {
                origin.preChatSurvey
            }
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
            var handler = origin.create(customFields, preChatSurveyResponse)
            handler = ChatThreadHandlerLogging(handler, identity)
            handler
        }
    }

    override fun threads(listener: OnThreadsUpdatedListener) = scope("threads") {
        verbose("Registered")
        origin.threads {
            scope("onThreadsUpdated") {
                duration {
                    listener.onThreadsUpdated(it)
                }
            }
        }
    }

    override fun thread(thread: ChatThread) = scope("thread") {
        duration {
            var handler = origin.thread(thread)
            handler = ChatThreadHandlerLogging(handler, identity)
            handler
        }
    }
}
