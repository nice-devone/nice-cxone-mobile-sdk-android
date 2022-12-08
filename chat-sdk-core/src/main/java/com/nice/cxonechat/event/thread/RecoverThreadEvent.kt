package com.nice.cxonechat.event.thread

import com.nice.cxonechat.internal.model.network.ActionRecoverThread
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.thread.ChatThread

internal object RecoverThreadEvent : ChatThreadEvent() {

    override fun getModel(
        thread: ChatThread,
        connection: Connection,
    ) = ActionRecoverThread(
        connection = connection,
        thread = thread
    )

}
