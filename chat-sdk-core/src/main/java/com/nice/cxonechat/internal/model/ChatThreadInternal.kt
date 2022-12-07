package com.nice.cxonechat.internal.model

import com.nice.cxonechat.message.Message
import com.nice.cxonechat.thread.Agent
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.CustomField
import java.util.UUID

internal data class ChatThreadInternal(
    override val id: UUID,
    override val threadName: String? = "",
    override val messages: List<Message> = emptyList(),
    override val threadAgent: Agent? = null,
    override val canAddMoreMessages: Boolean = true,
    override val scrollToken: String = "",
    override val fields: List<CustomField> = emptyList(),
) : ChatThread() {

    override fun toString() = buildString {
        append("ChatThread(id=")
        append(id)
        append(", threadName=")
        append(threadName)
        append(", messages=")
        append(messages)
        append(", threadAgent=")
        append(threadAgent)
        append(", canAddMoreMessages=")
        append(canAddMoreMessages)
        append(", scrollToken='")
        append(scrollToken)
        append("', fields=")
        append(fields)
        append(")")
    }

}
