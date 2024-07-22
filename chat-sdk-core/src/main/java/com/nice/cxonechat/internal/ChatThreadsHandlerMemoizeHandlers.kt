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
import com.nice.cxonechat.ChatThreadsHandler
import com.nice.cxonechat.prechat.PreChatSurveyResponse
import com.nice.cxonechat.state.FieldDefinition
import com.nice.cxonechat.thread.ChatThread
import java.util.Collections
import java.util.UUID

/**
 * Implementation of [ChatThreadsHandler] which prevents creation of multiple instances of [ChatThreadHandler]
 * for [ChatThread] with the same [ChatThread.id].
 */
internal class ChatThreadsHandlerMemoizeHandlers(
    private val origin: ChatThreadsHandler,
) : ChatThreadsHandler by origin {

    private val threadHandlersMemoized: MutableMap<UUID, ChatThreadHandler> by lazy { Collections.synchronizedMap(mutableMapOf()) }

    override fun create(
        customFields: Map<String, String>,
        preChatSurveyResponse: Sequence<PreChatSurveyResponse<out FieldDefinition, out Any>>,
    ): ChatThreadHandler = origin.create(customFields, preChatSurveyResponse).also(::memoizeThreadHandler)

    override fun thread(thread: ChatThread): ChatThreadHandler =
        threadHandlersMemoized[thread.id] ?: origin.thread(thread).also(::memoizeThreadHandler)

    private fun memoizeThreadHandler(threadHandler: ChatThreadHandler) {
        threadHandlersMemoized[threadHandler.get().id] = threadHandler
    }
}
