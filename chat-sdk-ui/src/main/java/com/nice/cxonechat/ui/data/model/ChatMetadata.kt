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

package com.nice.cxonechat.ui.data.model

import com.nice.cxonechat.thread.Agent
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.ui.domain.model.threadOrAgentName

/**
 * Represents metadata for a chat, including the thread name and associated agent.
 *
 * @property threadName The name of the chat thread.
 * @property agent The agent associated with the chat thread, or `null` if no agent is assigned.
 */
internal data class ChatMetadata(
    val threadName: String?,
    val agent: Agent?,
) {
    companion object {
        /**
         * Converts a [ChatThread] instance into [ChatMetadata].
         *
         * @param isMultiThreadEnabled A flag indicating whether multi-threading is enabled.
         * @return A `ChatMetadata` instance containing the thread name and agent information.
         */
        fun ChatThread.asMetadata(isMultiThreadEnabled: Boolean) = ChatMetadata(
            threadName = threadOrAgentName(isMultiThreadEnabled),
            agent = threadAgent,
        )
    }
}
