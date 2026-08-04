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

package com.nice.cxonechat.ui.domain.model

import com.nice.cxonechat.ChatFieldHandler
import com.nice.cxonechat.ChatThreadActionHandler
import com.nice.cxonechat.ChatThreadEventHandler
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadMessageHandler
import com.nice.cxonechat.EventResponse
import com.nice.cxonechat.Popup
import com.nice.cxonechat.event.thread.ChatThreadEvent
import com.nice.cxonechat.message.OutboundMessage
import com.nice.cxonechat.thread.ChatThread
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf

/** Represents a handler for a chat thread that does not exist or is not yet resolved. */
internal object NoThreadHandler : ChatThreadHandler {

    override val threadFlow: Flow<ChatThread>
        get() = flowOf(NoThread)

    override fun get(): ChatThread = NoThread

    override fun setName(name: String) {
        // No operation, as there is no thread to set the name for.
    }

    override fun refresh() {
        // No operation, as there is no thread to refresh.
    }

    override suspend fun archive(): Boolean = false

    override fun messages(): ChatThreadMessageHandler = object : ChatThreadMessageHandler {
        override fun loadMore() {
            // No operation, as there are no messages to load.
        }

        // Deliberately fails loud instead of silently no-oping: send() returns the new message id
        // that the UI renders optimistically, so a no-op would have to fabricate an id and show a
        // phantom "sent" message that is never delivered. Callers (ChatThreadViewModel send paths)
        // catch this and drop the message with a log; do NOT change this to return a fake id.
        override suspend fun send(message: OutboundMessage): String =
            error("Cannot send message: no active chat thread")
    }

    override fun events(): ChatThreadEventHandler = object : ChatThreadEventHandler {
        override suspend fun trigger(event: ChatThreadEvent): EventResponse? = null
    }

    override fun actions(): ChatThreadActionHandler = object : ChatThreadActionHandler {
        override val popupFlow: Flow<Popup>
            get() = emptyFlow()
    }

    override fun customFields(): ChatFieldHandler {
        return object : ChatFieldHandler {
            override fun add(fields: Map<String, String>) {
                // No operation, as there are no custom fields to add.
            }
        }
    }

    override fun endContact() {
        // No operation, as there is no contact to end.
    }
}
