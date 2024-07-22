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
import com.nice.cxonechat.prechat.PreChatSurveyResponse
import com.nice.cxonechat.state.FieldDefinition
import com.nice.cxonechat.thread.ChatThread

internal class ChatThreadsHandlerReplayLastEmpty(
    private val origin: ChatThreadsHandler,
) : ChatThreadsHandler by origin {

    private var latestThread: (() -> ChatThread)? = null

    override fun create(
        customFields: Map<String, String>,
        preChatSurveyResponse: Sequence<PreChatSurveyResponse<out FieldDefinition, out Any>>,
    ): ChatThreadHandler {
        return origin.create(customFields, preChatSurveyResponse).also {
            latestThread = it::get
        }
    }

    override fun threads(listener: OnThreadsUpdatedListener): Cancellable {
        val latest = latestThread?.invoke() ?: return origin.threads(listener)
        return origin.threads { threads ->
            if (threads.any { thread -> thread.id == latest.id }) {
                latestThread = null
                listener.onThreadsUpdated(threads)
            } else {
                listener.onThreadsUpdated(listOfNotNull(latest) + threads)
            }
        }
    }
}
