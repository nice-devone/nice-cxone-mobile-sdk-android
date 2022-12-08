package com.nice.cxonechat.internal

import com.nice.cxonechat.ChatActionHandler
import com.nice.cxonechat.ChatActionHandler.OnPopupActionListener
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.duration
import com.nice.cxonechat.log.finest
import com.nice.cxonechat.log.scope

internal class ChatActionHandlerLogging(
    private val origin: ChatActionHandler,
    logger: Logger,
) : ChatActionHandler, LoggerScope by LoggerScope<ChatActionHandler>(logger) {

    init {
        finest("Initialized")
    }

    override fun onPopup(listener: OnPopupActionListener) = scope("onPopup") {
        finest("Registered")
        origin.onPopup { params, metadata ->
            scope("onShowPopup") {
                finest("params=$params, metadata=$metadata")
                duration { listener.onShowPopup(params, metadata) }
            }
        }
    }

    override fun close() = scope("close") {
        duration { origin.close() }
    }

}
