package com.nice.cxonechat.internal

import com.nice.cxonechat.ChatFieldHandler
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.duration
import com.nice.cxonechat.log.finest
import com.nice.cxonechat.log.scope

internal class ChatFieldHandlerLogging(
    private val origin: ChatFieldHandler,
    logger: Logger,
) : ChatFieldHandler, LoggerScope by LoggerScope<ChatFieldHandler>(logger) {

    override fun add(fields: Map<String, String>): Unit = scope("add") {
        finest("fields=$fields")
        duration {
            origin.add(fields)
        }
    }

}
