package com.nice.cxonechat.internal

import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadHandler.OnThreadUpdatedListener
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.duration
import com.nice.cxonechat.log.finest
import com.nice.cxonechat.log.scope

internal class ChatThreadHandlerLogging(
    private val origin: ChatThreadHandler,
    logger: Logger,
) : ChatThreadHandler, LoggerScope by LoggerScope<ChatThreadHandler>(logger) {

    override fun get() = scope("get") {
        duration {
            origin.get()
        }
    }

    override fun get(listener: OnThreadUpdatedListener) = scope("get") {
        finest("Registered")
        origin.get {
            duration {
                listener.onUpdated(it)
            }
        }
    }

    override fun setName(name: String) = scope("setName") {
        duration {
            origin.setName(name)
        }
    }

    override fun refresh() = scope("refresh") {
        duration {
            origin.refresh()
        }
    }

    override fun messages() = scope("messages") {
        duration {
            var handler = origin.messages()
            handler = ChatThreadMessageHandlerLogging(handler, identity)
            handler
        }
    }

    override fun events() = scope("events") {
        duration {
            var handler = origin.events()
            handler = ChatThreadEventHandlerLogging(handler, identity)
            handler
        }
    }

    override fun customFields() = scope("customFields") {
        duration {
            var handler = origin.customFields()
            handler = ChatFieldHandlerLogging(handler, identity)
            handler
        }
    }
}
