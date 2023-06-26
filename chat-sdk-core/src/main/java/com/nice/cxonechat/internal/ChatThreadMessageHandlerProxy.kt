package com.nice.cxonechat.internal

import com.nice.cxonechat.ChatThreadMessageHandler
import com.nice.cxonechat.internal.model.ChatThreadMutable

internal class ChatThreadMessageHandlerProxy(
    private val origin: ChatThreadMessageHandler,
    private val thread: ChatThreadMutable,
) : ChatThreadMessageHandler by origin {

    override fun loadMore() {
        if (thread.messages.isEmpty()) return
        origin.loadMore()
    }
}
