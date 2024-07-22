/*
 * Copyright (c) 2021-2024. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.internal.model

import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.Cancellable.Companion.cancel
import com.nice.cxonechat.message.Message
import com.nice.cxonechat.thread.Agent
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState
import com.nice.cxonechat.thread.CustomField
import java.util.UUID

internal class ChatThreadMutable private constructor(
    initial: ChatThread,
) : ChatThread(), AutoCloseable {

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
    override val threadState: ChatThreadState
        get() = thread.threadState
    override val positionInQueue: Int?
        get() = thread.positionInQueue
    override val hasOnlineAgent: Boolean
        get() = thread.hasOnlineAgent
    override val contactId: String?
        get() = thread.contactId

    val resultCallbacks = mutableMapOf<UUID, Cancellable>()

    fun update(thread: ChatThread) {
        this.thread = thread
    }

    operator fun plusAssign(thread: ChatThread) = update(thread)

    fun snapshot() = thread

    override fun close() {
        with(resultCallbacks) {
            values.cancel()
            clear()
        }
    }

    override fun toString(): String = thread.toString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChatThread) return false
        if (other is ChatThreadMutable) return this.thread == other.thread
        if (thread != other) return false
        return true
    }

    override fun hashCode(): Int = thread.hashCode()

    companion object {

        fun from(thread: ChatThread) = when (thread) {
            is ChatThreadMutable -> thread
            else -> ChatThreadMutable(thread)
        }

        fun ChatThread.asMutable() = from(this)
    }
}
