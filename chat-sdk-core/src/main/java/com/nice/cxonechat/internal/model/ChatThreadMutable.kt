package com.nice.cxonechat.internal.model

import com.nice.cxonechat.message.Message
import com.nice.cxonechat.thread.Agent
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.CustomField
import java.util.UUID

internal class ChatThreadMutable private constructor(
    initial: ChatThread,
) : ChatThread() {

    private var thread = initial

    override val id: UUID
        get() = thread.id
    override val threadName: String?
        get() = thread.threadName
    override val messages: List<Message>
        get() = thread.messages
    override val threadAgent: Agent?
        get() = thread.threadAgent
    override val canAddMoreMessages: Boolean
        get() = thread.canAddMoreMessages
    override val scrollToken: String
        get() = thread.scrollToken
    override val fields: List<CustomField>
        get() = thread.fields

    fun update(thread: ChatThread) {
        this.thread = thread
    }

    operator fun plusAssign(thread: ChatThread) = update(thread)

    fun snapshot() = thread

    override fun toString(): String = thread.toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChatThread) return false
        if (thread != other) return false
        return true
    }

    override fun hashCode(): Int = thread.hashCode()

    companion object {

        fun from(thread: ChatThread) = when (thread) {
            is ChatThreadMutable -> thread
            else -> ChatThreadMutable(thread)
        }
    }
}
