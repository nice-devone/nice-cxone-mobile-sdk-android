package com.nice.cxonechat.internal

import com.nice.cxonechat.ChatThreadEventHandler
import com.nice.cxonechat.ChatThreadEventHandler.OnEventSentListener
import com.nice.cxonechat.event.thread.ChatThreadEvent
import com.nice.cxonechat.socket.SendSelfRemovingCallback.Companion.send
import com.nice.cxonechat.thread.ChatThread

internal class ChatThreadEventHandlerImpl(
    private val chat: ChatWithParameters,
    private val thread: ChatThread,
) : ChatThreadEventHandler {

    override fun trigger(event: ChatThreadEvent, listener: OnEventSentListener?) {
        val model = event.getModel(thread, chat.connection)
        when (listener) {
            null -> chat.socket.send(model)
            else -> chat.socket.send(model) { listener.onSent() }
        }
    }

}
