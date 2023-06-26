package com.nice.cxonechat.internal

import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadHandler.OnThreadUpdatedListener
import com.nice.cxonechat.enums.EventType.ContactInboxAssigneeChanged
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.ChatThreadMutable
import com.nice.cxonechat.internal.model.network.EventContactInboxAssigneeChanged
import com.nice.cxonechat.internal.socket.EventCallback.Companion.addCallback

/**
 * This class wraps origin [ChatThreadHandler] and adds effect to it's [get] function, which
 * will update the mutable [thread] and also trigger [OnThreadUpdatedListener.onUpdated] callback
 * with updated thread, when [EventContactInboxAssigneeChanged] is received.
 *
 * [EventContactInboxAssigneeChanged] will always cause an update of thread agent.
 */
internal class ChatThreadHandlerAgentUpdate(
    private val origin: ChatThreadHandler,
    private val chat: ChatWithParameters,
    private val thread: ChatThreadMutable,
) : ChatThreadHandler by origin {

    override fun get(listener: OnThreadUpdatedListener): Cancellable {
        val cancellable = chat.socketListener
            .addCallback<EventContactInboxAssigneeChanged>(ContactInboxAssigneeChanged) { event ->
                if (!event.inThread(thread)) return@addCallback
                thread += thread.asCopyable().copy(
                    threadAgent = event.agent
                )
                listener.onUpdated(thread)
            }
        return Cancellable(
            cancellable,
            origin.get(listener)
        )
    }
}
