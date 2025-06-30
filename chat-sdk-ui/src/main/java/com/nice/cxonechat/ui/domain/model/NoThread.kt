/*
 * Copyright (c) 2021-2025. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.ui.domain.model

import com.nice.cxonechat.message.Message
import com.nice.cxonechat.thread.Agent
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState
import com.nice.cxonechat.thread.CustomField
import java.util.UUID

/** Represents a placeholder for a state where thread is not yet loaded. */
internal object NoThread : ChatThread() {
    override val id: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    override val threadName: String? = null
    override val messages: List<Message> = emptyList()
    override val threadAgent: Agent? = null
    override val canAddMoreMessages: Boolean = true
    override val scrollToken: String = "no-scroll-token"
    override val threadState: ChatThreadState = ChatThreadState.Pending
    override val fields: List<CustomField> = emptyList()
    override val positionInQueue: Int? = null
    override val hasOnlineAgent: Boolean = false
}
