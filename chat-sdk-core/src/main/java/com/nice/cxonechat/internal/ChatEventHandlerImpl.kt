package com.nice.cxonechat.internal

import com.nice.cxonechat.ChatEventHandler
import com.nice.cxonechat.ChatEventHandler.OnEventSentListener
import com.nice.cxonechat.event.ChatEvent
import com.nice.cxonechat.internal.socket.send

internal class ChatEventHandlerImpl(
    private val chat: ChatWithParameters,
) : ChatEventHandler {

    override fun trigger(event: ChatEvent, listener: OnEventSentListener?) {
        val model = event.getModel(chat.connection, chat.storage)
        when (listener) {
            null -> chat.socket.send(model)
            else -> chat.socket.send(model) { listener.onSent() }
        }
    }
}
