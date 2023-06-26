package com.nice.cxonechat.event.thread

import com.nice.cxonechat.Public
import com.nice.cxonechat.internal.model.network.ActionMessageSeenByCustomer
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.thread.ChatThread

/**
 * Event that marks a thread as read. This event should be triggered every time
 * user visits or interacts with any given thread.
 * */
@Public
object MarkThreadReadEvent : ChatThreadEvent() {

    override fun getModel(
        thread: ChatThread,
        connection: Connection,
    ): Any = ActionMessageSeenByCustomer(
        connection = connection,
        thread = thread
    )
}
