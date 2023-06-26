package com.nice.cxonechat.event.thread

import com.nice.cxonechat.internal.model.AttachmentModel
import com.nice.cxonechat.internal.model.CustomFieldModel
import com.nice.cxonechat.internal.model.network.ActionMessage
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.thread.ChatThread
import java.util.UUID

internal class MessageEvent(
    private val message: String,
    private val attachments: Iterable<AttachmentModel>,
    private val fields: List<CustomFieldModel>,
    private val authToken: String?,
    private val postback: String?,
) : ChatThreadEvent() {

    val messageId: UUID = UUID.randomUUID()

    override fun getModel(
        thread: ChatThread,
        connection: Connection,
    ) = ActionMessage(
        connection = connection,
        thread = thread,
        id = messageId,
        message = message,
        attachments = attachments,
        fields = fields,
        token = authToken,
        postback = postback
    )
}
