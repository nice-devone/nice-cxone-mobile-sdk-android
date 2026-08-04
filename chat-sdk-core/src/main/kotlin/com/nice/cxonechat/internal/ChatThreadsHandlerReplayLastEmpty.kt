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
import com.nice.cxonechat.prechat.PreChatSurveyResponse
import com.nice.cxonechat.state.FieldDefinition
import com.nice.cxonechat.thread.ChatThread
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform

/**
 * Wraps a [ChatThreadsHandler] to ensure a locally-created thread remains visible in
 * [ChatThreadsHandler.threadsFlow] until the server confirms it. After [create] is called, the new
 * thread is prepended to every emission until one emission contains a thread with the same ID,
 * at which point the local copy is discarded.
 */
internal class ChatThreadsHandlerReplayLastEmpty(
    private val origin: ChatThreadsHandler,
) : ChatThreadsHandler by origin {

    @Volatile private var latestThread: (() -> ChatThread)? = null

    override val threadsFlow: Flow<List<ChatThread>> = origin.threadsFlow.transform { threads ->
        val latest = latestThread?.invoke()
        when {
            latest == null -> emit(threads)
            threads.any { it.id == latest.id } -> {
                latestThread = null
                emit(threads)
            }
            else -> emit(listOf(latest) + threads)
        }
    }

    override fun create(): ChatThreadHandler = create(emptyMap(), emptySequence())

    override fun create(customFields: Map<String, String>): ChatThreadHandler = create(customFields, emptySequence())

    override fun create(
        preChatSurveyResponse: Sequence<PreChatSurveyResponse<out FieldDefinition, out Any>>,
    ): ChatThreadHandler = create(emptyMap(), preChatSurveyResponse)

    override fun create(
        customFields: Map<String, String>,
        preChatSurveyResponse: Sequence<PreChatSurveyResponse<out FieldDefinition, out Any>>,
    ): ChatThreadHandler {
        return origin.create(customFields, preChatSurveyResponse).also {
            latestThread = it::get
        }
    }
}
