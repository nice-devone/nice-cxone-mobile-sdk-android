package com.nice.cxonechat.internal.copy

import com.nice.cxonechat.internal.model.ChatThreadInternal
import com.nice.cxonechat.internal.model.ChatThreadMutable
import com.nice.cxonechat.message.Message
import com.nice.cxonechat.thread.Agent
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.CustomField
import java.util.UUID

internal class ChatThreadCopyable(
    private val model: ChatThread,
) {

    @Suppress("LongParameterList")
    fun copy(
        id: UUID = model.id,
        threadName: String? = model.threadName,
        messages: List<Message> = model.messages,
        threadAgent: Agent? = model.threadAgent,
        canAddMoreMessages: Boolean = model.canAddMoreMessages,
        scrollToken: String = model.scrollToken,
        fields: List<CustomField> = model.fields,
    ) = ChatThreadInternal(
        id = id,
        threadName = threadName,
        messages = messages,
        threadAgent = threadAgent,
        canAddMoreMessages = canAddMoreMessages,
        scrollToken = scrollToken,
        fields = fields
    )

    companion object {

        fun ChatThread.asCopyable() = when (this) {
            is ChatThreadMutable -> ChatThreadCopyable(snapshot())
            else -> ChatThreadCopyable(this)
        }

    }

}
