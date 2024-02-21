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
