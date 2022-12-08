package com.nice.cxonechat.internal

import com.nice.cxonechat.ChatThreadsHandler
import com.nice.cxonechat.ChatThreadsHandler.OnThreadsUpdatedListener
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.duration
import com.nice.cxonechat.log.finest
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.thread.ChatThread

internal class ChatThreadsHandlerLogging(
    private val origin: ChatThreadsHandler,
    logger: Logger,
) : ChatThreadsHandler, LoggerScope by LoggerScope<ChatThreadsHandler>(logger) {

    override fun refresh() = scope("refresh") {
        duration {
            origin.refresh()
        }
    }

    override fun create(customFields: Map<String, String>) = scope("create") {
        duration {
            var handler = origin.create(customFields)
            handler = ChatThreadHandlerLogging(handler, identity)
            handler
        }
    }

    override fun threads(listener: OnThreadsUpdatedListener) = scope("threads") {
        finest("Registered")
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
