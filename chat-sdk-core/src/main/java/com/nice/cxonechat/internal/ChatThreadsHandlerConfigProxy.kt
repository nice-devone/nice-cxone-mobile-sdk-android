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

import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadsHandler
import com.nice.cxonechat.ChatThreadsHandler.OnThreadsUpdatedListener
import com.nice.cxonechat.exceptions.MissingThreadListFetchException
import com.nice.cxonechat.exceptions.UnsupportedChannelConfigException
import com.nice.cxonechat.prechat.PreChatSurveyResponse
import com.nice.cxonechat.state.FieldDefinition

/**
 * Class responsible for checking that SDK usage adheres to the chat configuration.
 *
 * Current sole responsibility is to enforce the requirement that single thread channel creates
 * at most one thread.
 */
internal class ChatThreadsHandlerConfigProxy(
    private val origin: ChatThreadsHandler,
    private val chat: ChatWithParameters,
) : ChatThreadsHandler by origin {

    private var threadCount: Int = -1

    override fun create(
        customFields: Map<String, String>,
        preChatSurveyResponse: Sequence<PreChatSurveyResponse<out FieldDefinition, out Any>>,
    ): ChatThreadHandler = checkAndRun { origin.create(customFields, preChatSurveyResponse) }

    private fun checkAndRun(block: () -> ChatThreadHandler): ChatThreadHandler {
        if (chat.configuration.hasMultipleThreadsPerEndUser) return block()

        val count = threadCount
        return when {
            count < 0 -> throw MissingThreadListFetchException()
            count == 0 -> block()
            else -> throw UnsupportedChannelConfigException()
        }
    }

    override fun threads(listener: OnThreadsUpdatedListener): Cancellable {
        return origin.threads {
            threadCount = it.size
            listener.onThreadsUpdated(it)
        }
    }
}
