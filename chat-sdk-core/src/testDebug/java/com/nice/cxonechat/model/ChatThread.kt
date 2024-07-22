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
