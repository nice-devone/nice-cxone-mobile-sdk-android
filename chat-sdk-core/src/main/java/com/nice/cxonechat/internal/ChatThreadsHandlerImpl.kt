package com.nice.cxonechat.internal

import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadsHandler
import com.nice.cxonechat.enums.EventType.ThreadListFetched
import com.nice.cxonechat.event.FetchThreadEvent
import com.nice.cxonechat.internal.model.ChatThreadInternal
import com.nice.cxonechat.internal.model.ChatThreadMutable
import com.nice.cxonechat.internal.model.CustomFieldInternal
import com.nice.cxonechat.internal.model.network.EventThreadListFetched
import com.nice.cxonechat.internal.model.network.ReceivedThreadData
import com.nice.cxonechat.socket.EventCallback.Companion.addCallback
import com.nice.cxonechat.thread.ChatThread
import java.util.UUID

internal class ChatThreadsHandlerImpl(
    private val chat: ChatWithParameters,
) : ChatThreadsHandler {

    override fun refresh() {
        chat.events().trigger(FetchThreadEvent)
    }

    override fun create(customFields: Map<String, String>): ChatThreadHandler {
        val uuid = UUID.randomUUID()
        val thread = ChatThreadInternal(
            id = uuid,
            fields = customFields.map(::CustomFieldInternal)
        )
        return createHandler(thread)
    }

    override fun threads(listener: ChatThreadsHandler.OnThreadsUpdatedListener): Cancellable {
        return chat.socket.addCallback<EventThreadListFetched>(ThreadListFetched) { event ->
            listener.onThreadsUpdated(event.threads.map(ReceivedThreadData::toChatThread))
        }
    }

    override fun thread(thread: ChatThread): ChatThreadHandler {
        return createHandler(ChatThreadMutable.from(thread))
    }

    // ---

    private fun createHandler(
        thread: ChatThread,
    ): ChatThreadHandler {
        val mutableThread = ChatThreadMutable.from(thread)
        var handler: ChatThreadHandler
        handler = ChatThreadHandlerImpl(chat, mutableThread)
        handler = ChatThreadHandlerMetadata(handler, chat, mutableThread)
        handler = ChatThreadHandlerMessages(handler, chat, mutableThread)
        handler = ChatThreadHandlerAgentTyping(handler, chat)
        return handler
    }

}
