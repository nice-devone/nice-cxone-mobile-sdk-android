package com.nice.cxonechat.event.thread

import com.nice.cxonechat.internal.model.network.ActionLoadMoreMessages
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.thread.ChatThread

internal object LoadMoreMessagesEvent : ChatThreadEvent() {

    override fun getModel(
        thread: ChatThread,
        connection: Connection,
    ) = ActionLoadMoreMessages(
        connection = connection,
        thread = thread
    )

}
