package com.nice.cxonechat.event.thread

import com.nice.cxonechat.Public
import com.nice.cxonechat.internal.model.network.ActionLoadThreadMetadata
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.thread.ChatThread

/**
 * request thread metadata including name and last message.
 */
@Public
object LoadThreadMetadataEvent : ChatThreadEvent() {

    override fun getModel(
        thread: ChatThread,
        connection: Connection,
    ) = ActionLoadThreadMetadata(
        connection = connection,
        thread = thread
    )
}
