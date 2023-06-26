package com.nice.cxonechat.thread

import com.nice.cxonechat.Public
import com.nice.cxonechat.message.Message
import java.util.UUID

/**
 * All information about a chat thread as well as the messages for the thread.
 */
@Public
abstract class ChatThread {
    /** The unique id of the thread. */
    abstract val id: UUID

    /** The name given to the thread (for multi-thread channels only). */
    abstract val threadName: String?

    /** The list of messages on the thread. */
    abstract val messages: List<Message>

    /** The agent assigned in the thread. */
    abstract val threadAgent: Agent?

    /** Whether more messages can be added to the thread (not archived) or otherwise (archived). */
    abstract val canAddMoreMessages: Boolean

    /** The token for the scroll position used to load more messages. */
    abstract val scrollToken: String

    /** Custom fields attached to this thread. */
    abstract val fields: List<CustomField>

    /** Whether there are more messages to load in the thread. */
    val hasMoreMessagesToLoad: Boolean
        get() = scrollToken.isNotEmpty()
}
