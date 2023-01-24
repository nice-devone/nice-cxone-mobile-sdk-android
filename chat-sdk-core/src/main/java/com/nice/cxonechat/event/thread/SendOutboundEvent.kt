package com.nice.cxonechat.event.thread

import com.nice.cxonechat.internal.model.network.ActionMessage
import com.nice.cxonechat.message.MessageDirection.ToClient
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.thread.ChatThread
import java.util.UUID

internal class SendOutboundEvent(
    private val message: String,
    private val authToken: String?,
) : ChatThreadEvent() {

    override fun getModel(
        thread: ChatThread,
        connection: Connection,
    ) = ActionMessage(
        connection = connection,
        thread = thread,
        id = UUID.randomUUID(),
        message = message,
        attachments = emptyList(),
        fields = emptyList(), // SendOutboundEvent can't have customer data (for now).
        token = authToken,
        direction = ToClient,
    )
}
