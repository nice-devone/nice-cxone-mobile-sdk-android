package com.nice.cxonechat.internal

import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadHandler.OnThreadUpdatedListener
import com.nice.cxonechat.enums.EventType.SenderTypingEnded
import com.nice.cxonechat.enums.EventType.SenderTypingStarted
import com.nice.cxonechat.internal.copy.AgentCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.network.EventAgentTyping
import com.nice.cxonechat.socket.EventCallback.Companion.addCallback
import com.nice.cxonechat.thread.ChatThread

internal class ChatThreadHandlerAgentTyping(
    private val origin: ChatThreadHandler,
    private val chat: ChatWithParameters,
) : ChatThreadHandler by origin {

    @Suppress("KotlinConstantConditions")
    override fun get(listener: OnThreadUpdatedListener): Cancellable {
        var isTyping = false
        val cancellableStarted = chat.socket.addCallback<EventAgentTyping>(SenderTypingStarted) {
            if (it.inThread(get())) {
                isTyping = true
                listener.onUpdated(updateThread(typing = isTyping))
            }
        }
        val cancellableEnded = chat.socket.addCallback<EventAgentTyping>(SenderTypingEnded) {
            if (it.inThread(get())) {
                isTyping = false
                listener.onUpdated(updateThread(typing = isTyping))
            }
        }
        val cancellableOrigin = origin.get {
            listener.onUpdated(updateThread(isTyping, it))
        }
        return Cancellable(
            cancellableStarted,
            cancellableEnded,
            cancellableOrigin
        )
    }

    private fun updateThread(typing: Boolean, thread: ChatThread = get()): ChatThread {
        val agent = thread.threadAgent?.asCopyable()?.copy(isTyping = typing)
        return thread.asCopyable().copy(threadAgent = agent)
    }

}
