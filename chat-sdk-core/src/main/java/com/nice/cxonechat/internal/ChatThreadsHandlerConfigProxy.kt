package com.nice.cxonechat.internal

import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadsHandler
import com.nice.cxonechat.ChatThreadsHandler.OnThreadsUpdatedListener
import com.nice.cxonechat.exceptions.MissingThreadListFetchException
import com.nice.cxonechat.exceptions.UnsupportedChannelConfigException

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

    override fun create(customFields: Map<String, String>): ChatThreadHandler {
        return checkAndRun { origin.create(customFields) }
    }

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
