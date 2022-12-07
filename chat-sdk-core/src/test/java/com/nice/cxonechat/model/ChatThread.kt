package com.nice.cxonechat.model

import com.nice.cxonechat.internal.model.ChatThreadInternal
import com.nice.cxonechat.message.Message
import com.nice.cxonechat.thread.Agent
import com.nice.cxonechat.thread.CustomField
import java.util.UUID

@Suppress("LongParameterList")
internal fun makeChatThread(
    id: UUID = UUID.randomUUID(),
    threadName: String? = "",
    messages: List<Message> = emptyList(),
    threadAgent: Agent? = null,
    canAddMoreMessages: Boolean = true,
    scrollToken: String = "",
    fields: List<CustomField> = emptyList(),
) = ChatThreadInternal(
    id = id,
    threadName = threadName,
    messages = messages,
    threadAgent = threadAgent,
    canAddMoreMessages = canAddMoreMessages,
    scrollToken = scrollToken,
    fields = fields,
)
