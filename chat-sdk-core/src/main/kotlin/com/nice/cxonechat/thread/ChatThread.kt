/*
 * Copyright (c) 2021-2026. NICE Ltd. All rights reserved.
 *
 * Licensed under the NICE License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/nice-devone/nice-cxone-mobile-sdk-android/blob/main/LICENSE
 *
 * TO THE EXTENT PERMITTED BY APPLICABLE LAW, THE CXONE MOBILE SDK IS PROVIDED ON
 * AN “AS IS” BASIS. NICE HEREBY DISCLAIMS ALL WARRANTIES AND CONDITIONS, EXPRESS
 * OR IMPLIED, INCLUDING (WITHOUT LIMITATION) WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT, AND TITLE.
 */

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

    /**
     * Current state of the thread.
     */
    abstract val threadState: ChatThreadState

    /** Custom fields attached to this thread. */
    abstract val fields: List<CustomField>

    /** Position in queue if this is a Live Chat. Always zero if this is not a live chat. */
    abstract val positionInQueue: Int?

    /** Is any agent online? Always true if this is not a live chat. */
    abstract val hasOnlineAgent: Boolean

    /** Whether there are more messages to load in the thread. */
    val hasMoreMessagesToLoad: Boolean
        get() = scrollToken.isNotEmpty()

    /** Id of the current active contact for the thread. Null if no contact has been established yet. */
    abstract val contactId: String?

    /**
     * Who currently handles the contact: CXone routing or an automated bot.
     * `null` when the platform reports no handler for this channel.
     * Treat [ContactHandler.Cognigy] and [ContactHandler.Unknown] as "not handled by CXone".
     *
     * Read it from the thread handler ([com.nice.cxonechat.ChatThreadHandler.get] or `threadFlow`); that is the
     * reliable source. It is updated from thread recovery, message, assignee, case-status and handover events.
     *
     * Entries in the thread list may report `null` or lag behind the handler, and a list refresh reports `null`.
     * Do not read `null` there as "no handler".
     */
    open val contactHandler: ContactHandler? = null
}
