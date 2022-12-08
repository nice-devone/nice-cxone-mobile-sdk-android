package com.nice.cxonechat.event.thread

import com.nice.cxonechat.Public
import com.nice.cxonechat.internal.model.network.ActionArchiveThread
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.thread.ChatThread

/**
 * Event that archives the thread it was invoked upon.
 * */
@Public
object ArchiveThreadEvent : ChatThreadEvent() {

    override fun getModel(
        thread: ChatThread,
        connection: Connection,
    ): Any = ActionArchiveThread(
        connection = connection,
        thread = thread
    )

}
