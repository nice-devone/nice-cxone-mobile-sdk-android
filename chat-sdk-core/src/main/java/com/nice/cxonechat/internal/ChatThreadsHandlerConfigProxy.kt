package com.nice.cxonechat.internal

import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadsHandler
import com.nice.cxonechat.ChatThreadsHandler.OnThreadsUpdatedListener
import com.nice.cxonechat.enums.CXOneChatError

internal class ChatThreadsHandlerConfigProxy(
    private val origin: ChatThreadsHandler,
    private val chat: ChatWithParameters,
) : ChatThreadsHandler by origin {

    private var threadCount: Int = -1

    override fun create(customFields: Map<String, String>): ChatThreadHandler {
        return checkAndRun { origin.create(customFields) }
    }

    private fun checkAndRun(block: () -> ChatThreadHandler): ChatThreadHandler {
        if (chat.configuration.hasMultipleThreadsPerEndUser)
            return block()

        check(threadCount >= 0) {
            "First you need to call threads {} method to fetch list of threads"
        }

        if (threadCount == 0)
            return block()

        throw CXOneChatError.UnsupportedChannelConfig.value
    }

    override fun threads(listener: OnThreadsUpdatedListener): Cancellable {
        return origin.threads {
            threadCount = it.size
            listener.onThreadsUpdated(it)
        }
    }

}
