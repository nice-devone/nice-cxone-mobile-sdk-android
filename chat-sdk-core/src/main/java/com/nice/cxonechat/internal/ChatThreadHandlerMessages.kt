package com.nice.cxonechat.internal

import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadHandler.OnThreadUpdatedListener
import com.nice.cxonechat.enums.EventType.MessageCreated
import com.nice.cxonechat.enums.EventType.MoreMessagesLoaded
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.ChatThreadMutable
import com.nice.cxonechat.internal.model.network.EventMessageCreated
import com.nice.cxonechat.internal.model.network.EventMoreMessagesLoaded
import com.nice.cxonechat.socket.EventCallback.Companion.addCallback

internal class ChatThreadHandlerMessages(
    private val origin: ChatThreadHandler,
    private val chat: ChatWithParameters,
    private val thread: ChatThreadMutable,
) : ChatThreadHandler by origin {

    override fun get(listener: OnThreadUpdatedListener): Cancellable {
        val moreMessagesListener = chat.socket.addCallback<EventMoreMessagesLoaded>(MoreMessagesLoaded) { event ->
            if (!event.inThread(thread))
                return@addCallback
            thread += thread.asCopyable().copy(
                messages = event.messages + thread.messages,
                scrollToken = event.scrollToken
            )
            listener.onUpdated(thread)
        }
        val messageCreated = chat.socket.addCallback<EventMessageCreated>(MessageCreated) { event ->
            if (!event.inThread(thread))
                return@addCallback
            thread += thread.asCopyable().copy(
                messages = listOfNotNull(event.message) + thread.messages
            )
            listener.onUpdated(thread)
        }
        return Cancellable(
            moreMessagesListener,
            messageCreated,
            origin.get(listener)
        )
    }

}
