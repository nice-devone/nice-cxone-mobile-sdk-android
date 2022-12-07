package com.nice.cxonechat.event.thread

import com.nice.cxonechat.internal.model.CustomFieldModel
import com.nice.cxonechat.internal.model.network.ActionMessage
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.thread.ChatThread
import java.util.UUID

internal class SendOutboundEvent(
    private val message: String,
    private val authToken: String?,
    private val customerFields: List<CustomFieldModel>,
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
        fields = customerFields,
        token = authToken
    )

}
