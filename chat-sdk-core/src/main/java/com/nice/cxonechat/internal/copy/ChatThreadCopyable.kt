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

package com.nice.cxonechat.internal.copy

import com.nice.cxonechat.internal.model.ChatThreadInternal
import com.nice.cxonechat.internal.model.ChatThreadMutable
import com.nice.cxonechat.message.Message
import com.nice.cxonechat.thread.Agent
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState
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
        threadState: ChatThreadState = model.threadState,
        positionInQueue: Int? = model.positionInQueue,
        hasOnlineAgent: Boolean = model.hasOnlineAgent,
        contactId: String? = model.contactId,
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
        hasOnlineAgent = hasOnlineAgent,
        contactId = contactId,
    )

    companion object {

        fun ChatThread.asCopyable() = when (this) {
            is ChatThreadMutable -> ChatThreadCopyable(snapshot())
            else -> ChatThreadCopyable(this)
        }

        /**
         * Merge two collections of [Message], by using [Message.id] as unique key for updating of existing messages.
         * [Message]s from [newMessages] with matching keys will overwrite messages in [this] collection.
         *
         * @param newMessages collection used for updating of [this] collection.
         * @return Union of the collections where elements with clashing keys were replaced with elements from
         * [newMessages] collection.
         */
        fun Iterable<Message>.updateWith(newMessages: Iterable<Message>) = this
            .associateBy(Message::id)
            .plus(
                newMessages.associateBy(Message::id)
            )
            .values
            .sortedBy(Message::createdAt)
            .toList()
    }
}
