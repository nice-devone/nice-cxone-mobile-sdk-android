package com.nice.cxonechat.internal

import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadsHandler
import com.nice.cxonechat.ChatThreadsHandler.OnThreadsUpdatedListener
import com.nice.cxonechat.thread.ChatThread

internal class ChatThreadsHandlerReplayLastEmpty(
    private val origin: ChatThreadsHandlerImpl,
) : ChatThreadsHandler by origin {

    private var latestThread: (() -> ChatThread)? = null

    override fun create(customFields: Map<String, String>): ChatThreadHandler {
        return origin.create(customFields).also {
            latestThread = it::get
        }
    }

    override fun threads(listener: OnThreadsUpdatedListener): Cancellable {
        return origin.threads {
            listener.onThreadsUpdated(listOfNotNull(latestThread?.invoke()) + it)
        }
    }

}
