package com.nice.cxonechat.event.thread

import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.network.ActionUpdateThread
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.thread.ChatThread

internal class UpdateThreadEvent(
    private val name: String,
) : ChatThreadEvent() {

    override fun getModel(
        thread: ChatThread,
        connection: Connection,
    ) = ActionUpdateThread(
        connection = connection,
        thread = thread.asCopyable().copy(threadName = name)
    )

}
