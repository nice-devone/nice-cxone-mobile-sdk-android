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

package com.nice.cxonechat.ui.model

import com.nice.cxonechat.message.Message
import com.nice.cxonechat.thread.Agent
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState
import com.nice.cxonechat.thread.CustomField
import java.util.UUID

/**
 * Internal version of [ChatThread] which allows us to create modified copies of [ChatThread].
 */
internal data class ChatThreadCopy(
    override val canAddMoreMessages: Boolean,
    override val fields: List<CustomField>,
    override val id: UUID,
    override val messages: List<Message>,
    override val scrollToken: String,
    override val threadAgent: Agent?,
    override val threadName: String?,
    override val threadState: ChatThreadState,
    override val positionInQueue: Int?,
    override val hasOnlineAgent: Boolean,
) : ChatThread() {
    companion object {
        @Suppress("LongParameterList")
        fun ChatThread.copy(
            canAddMoreMessages: Boolean = this.canAddMoreMessages,
            fields: List<CustomField> = this.fields,
            id: UUID = this.id,
            messages: List<Message> = this.messages,
            scrollToken: String = this.scrollToken,
            threadAgent: Agent? = this.threadAgent,
            threadName: String? = this.threadName,
            threadState: ChatThreadState = this.threadState,
            positionInQueue: Int? = this.positionInQueue,
            hasOnlineAgent: Boolean = this.hasOnlineAgent,
        ): ChatThreadCopy = ChatThreadCopy(
            canAddMoreMessages = canAddMoreMessages,
            fields = fields,
            id = id,
            messages = messages,
            scrollToken = scrollToken,
            threadAgent = threadAgent,
            threadName = threadName,
            threadState = threadState,
            positionInQueue = positionInQueue,
            hasOnlineAgent = hasOnlineAgent,
        )
    }
}
