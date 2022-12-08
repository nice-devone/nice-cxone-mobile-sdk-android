package com.nice.cxonechat.internal

import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadHandler.OnThreadUpdatedListener
import com.nice.cxonechat.enums.EventType.ThreadMetadataLoaded
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.ChatThreadMutable
import com.nice.cxonechat.internal.model.network.EventThreadMetadataLoaded
import com.nice.cxonechat.socket.EventCallback.Companion.addCallback

internal class ChatThreadHandlerMetadata(
    private val origin: ChatThreadHandler,
    private val chat: ChatWithParameters,
    private val thread: ChatThreadMutable,
) : ChatThreadHandler by origin {

    override fun get(listener: OnThreadUpdatedListener): Cancellable {
        val cancellable = chat.socket.addCallback<EventThreadMetadataLoaded>(ThreadMetadataLoaded) { event ->
            if (!event.inThread(thread))
                return@addCallback
            thread += thread.asCopyable().copy(
                messages = thread.messages.ifEmpty { listOfNotNull(event.message) },
                threadAgent = event.agent ?: thread.threadAgent
            )
            listener.onUpdated(thread)
        }
        return Cancellable(
            cancellable,
            origin.get(listener)
        )
    }

}
