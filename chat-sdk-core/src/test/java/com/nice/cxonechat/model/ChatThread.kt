package com.nice.cxonechat.model

import com.nice.cxonechat.internal.model.ChatThreadInternal
import com.nice.cxonechat.message.Message
import com.nice.cxonechat.thread.Agent
import com.nice.cxonechat.thread.ChatThreadState
import com.nice.cxonechat.thread.ChatThreadState.Ready
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
    threadState: ChatThreadState = Ready,
    positionInQueue: Int? = null,
    hasOnlineAgent: Boolean = true,
) = ChatThreadInternal(
    id = id,
    threadName = threadName,
    messages = messages,
    threadAgent = threadAgent,
    canAddMoreMessages = canAddMoreMessages,
    scrollToken = scrollToken,
    fields = fields,
    threadState = threadState,
    positionInQueue = positionInQueue,
    hasOnlineAgent = hasOnlineAgent
)
