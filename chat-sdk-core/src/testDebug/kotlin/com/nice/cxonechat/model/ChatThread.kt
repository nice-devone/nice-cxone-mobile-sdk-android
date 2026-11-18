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

package com.nice.cxonechat.model

import com.nice.cxonechat.AbstractChatTestSubstrate.Companion.TestContactId
import com.nice.cxonechat.internal.model.ChatThreadInternal
import com.nice.cxonechat.message.Message
import com.nice.cxonechat.thread.Agent
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState
import com.nice.cxonechat.thread.ChatThreadState.Ready
import com.nice.cxonechat.thread.ContactHandler
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
    contactId: String? = TestContactId,
    contactHandler: ContactHandler? = null,
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
    contactHandler = contactHandler,
)

/**
 * A [ChatThread] written the way an integrator would: it implements the abstract members and nothing else,
 * so it inherits every default [ChatThread] declares.
 */
internal open class MinimalChatThread(private val origin: ChatThread = makeChatThread()) : ChatThread() {
    override val id: UUID get() = origin.id
    override val threadName: String? get() = origin.threadName
    override val messages: List<Message> get() = origin.messages
    override val threadAgent: Agent? get() = origin.threadAgent
    override val canAddMoreMessages: Boolean get() = origin.canAddMoreMessages
    override val scrollToken: String get() = origin.scrollToken
    override val threadState: ChatThreadState get() = origin.threadState
    override val fields: List<CustomField> get() = origin.fields
    override val positionInQueue: Int? get() = origin.positionInQueue
    override val hasOnlineAgent: Boolean get() = origin.hasOnlineAgent
    override val contactId: String? get() = origin.contactId
}
