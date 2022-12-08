package com.nice.cxonechat.event.thread

import com.nice.cxonechat.Public
import com.nice.cxonechat.internal.model.network.ActionCustomerTyping
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.thread.ChatThread

/**
 * Event notifying the user that user has started typing.
 * */
@Public
object TypingStartEvent : ChatThreadEvent() {

    override fun getModel(
        thread: ChatThread,
        connection: Connection,
    ): Any = ActionCustomerTyping.started(
        connection = connection,
        thread = thread
    )

}
