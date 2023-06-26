package com.nice.cxonechat.internal

import com.nice.cxonechat.ChatEventHandler
import com.nice.cxonechat.ChatEventHandler.OnEventSentListener
import com.nice.cxonechat.event.ChatEvent
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.duration
import com.nice.cxonechat.log.finest
import com.nice.cxonechat.log.scope

internal class ChatEventHandlerLogging(
    private val origin: ChatEventHandler,
    private val logger: Logger,
) : ChatEventHandler, LoggerScope by LoggerScope<ChatEventHandlerLogging>(logger) {

    override fun trigger(event: ChatEvent, listener: OnEventSentListener?) = scope("trigger") {
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
