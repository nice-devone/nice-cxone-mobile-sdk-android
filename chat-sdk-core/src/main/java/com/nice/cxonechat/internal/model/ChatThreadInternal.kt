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

import com.nice.cxonechat.message.Message
import com.nice.cxonechat.thread.Agent
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState
import com.nice.cxonechat.thread.CustomField
import java.util.UUID

internal data class ChatThreadInternal(
    override val id: UUID,
    override val threadName: String? = "",
    override val messages: List<Message> = emptyList(),
    override val threadAgent: Agent? = null,
    override val canAddMoreMessages: Boolean = true,
    override val scrollToken: String = "",
    override val fields: List<CustomField> = emptyList(),
    override val threadState: ChatThreadState,
    override val positionInQueue: Int? = null,
    override val hasOnlineAgent: Boolean = true,
    override val contactId: String? = null,
) : ChatThread() {
    override fun toString() = buildString {
        append("ChatThread(id=")
        append(id)
        append(", threadName=")
        append(threadName)
        append(", messages=")
        append(messages)
        append(", threadAgent=")
        append(threadAgent)
        append(", canAddMoreMessages=")
        append(canAddMoreMessages)
        append(", scrollToken='")
        append(scrollToken)
        append("', fields=")
        append(fields)
        append("', state=")
        append(threadState)
        append(", positionInQueue=")
        append(positionInQueue)
        append(", hasOnlineAgent=")
        append(hasOnlineAgent)
        append(", contactId=")
        append(contactId)
        append(")")
    }
}
