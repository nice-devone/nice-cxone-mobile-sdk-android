package com.nice.cxonechat.internal

import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.ChatFieldHandler
import com.nice.cxonechat.ChatThreadEventHandler
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadHandler.OnThreadUpdatedListener
import com.nice.cxonechat.ChatThreadMessageHandler
import com.nice.cxonechat.enums.EventType.ThreadRecovered
import com.nice.cxonechat.event.thread.RecoverThreadEvent
import com.nice.cxonechat.event.thread.UpdateThreadEvent
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.ChatThreadMutable
import com.nice.cxonechat.internal.model.network.EventThreadRecovered
import com.nice.cxonechat.message.Message
import com.nice.cxonechat.socket.EventCallback.Companion.addCallback
import com.nice.cxonechat.thread.ChatThread

internal class ChatThreadHandlerImpl(
    private val chat: ChatWithParameters,
    private val thread: ChatThreadMutable,
) : ChatThreadHandler {

    override fun get(): ChatThread = thread.snapshot()

    override fun get(listener: OnThreadUpdatedListener): Cancellable {
        return chat.socket.addCallback<EventThreadRecovered>(ThreadRecovered) { event ->
            if (!event.inThread(thread)) {
                return@addCallback
            }
            val messages = event.messages.sortedBy(Message::createdAt)
            thread += thread.asCopyable().copy(
                threadName = event.thread.threadName,
                messages = messages + thread.messages,
                scrollToken = event.scrollToken
                    .takeUnless { event.messages.isEmpty() }
                    ?: thread.scrollToken,
                threadAgent = event.agent
            )
            listener.onUpdated(thread)
        }
    }

    override fun refresh() {
        events().trigger(RecoverThreadEvent)
    }

    override fun setName(name: String) {
        events().trigger(UpdateThreadEvent(name)) {
            thread += thread.asCopyable().copy(threadName = name)
        }
    }

    override fun messages(): ChatThreadMessageHandler {
        var handler: ChatThreadMessageHandler
        handler = ChatThreadMessageHandlerImpl(chat, this)
        handler = ChatThreadMessageHandlerProxy(handler, thread)
        handler = ChatThreadMessageHandlerThreading(handler, chat)
        return handler
    }

    override fun events(): ChatThreadEventHandler {
        var handler: ChatThreadEventHandler
        handler = ChatThreadEventHandlerImpl(chat, thread)
        handler = ChatThreadEventHandlerTokenGuard(handler, chat)
        return handler
    }

    override fun customFields(): ChatFieldHandler {
        return ChatFieldHandlerThread(this, thread)
    }

}
