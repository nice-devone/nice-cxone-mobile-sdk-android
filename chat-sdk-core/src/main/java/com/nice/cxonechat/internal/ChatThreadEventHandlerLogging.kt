package com.nice.cxonechat.internal

import com.nice.cxonechat.ChatThreadEventHandler
import com.nice.cxonechat.ChatThreadEventHandler.OnEventSentListener
import com.nice.cxonechat.event.thread.ChatThreadEvent
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.duration
import com.nice.cxonechat.log.finest
import com.nice.cxonechat.log.scope

internal class ChatThreadEventHandlerLogging(
    private val origin: ChatThreadEventHandler,
    private val logger: Logger,
) : ChatThreadEventHandler, LoggerScope by LoggerScope<ChatThreadEventHandler>(logger) {

    init {
        finest("Initialized")
    }

    override fun trigger(
        event: ChatThreadEvent,
        listener: OnEventSentListener?,
    ) = scope("trigger") {
        finest("Dispatching (event=$event)")
        origin.trigger(event) {
            scope("onSent") {
                duration {
                    listener?.onSent()
                }
            }
        }
    }
}
