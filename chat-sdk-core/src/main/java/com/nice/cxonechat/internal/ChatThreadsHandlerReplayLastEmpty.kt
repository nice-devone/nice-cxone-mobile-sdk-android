package com.nice.cxonechat.internal

import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadsHandler
import com.nice.cxonechat.ChatThreadsHandler.OnThreadsUpdatedListener
import com.nice.cxonechat.prechat.PreChatSurveyResponse
import com.nice.cxonechat.state.FieldDefinition
import com.nice.cxonechat.thread.ChatThread

internal class ChatThreadsHandlerReplayLastEmpty(
    private val origin: ChatThreadsHandlerImpl,
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
