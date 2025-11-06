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

import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.ChatFieldHandler
import com.nice.cxonechat.ChatThreadActionHandler
import com.nice.cxonechat.ChatThreadEventHandler
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadMessageHandler
import com.nice.cxonechat.event.thread.ChatThreadEvent
import com.nice.cxonechat.message.OutboundMessage
import com.nice.cxonechat.thread.ChatThread

/** Represents a handler for a chat thread that does not exist or is not yet resolved. */
internal object NoThreadHandler : ChatThreadHandler {
    override fun get(): ChatThread = NoThread

    override fun get(listener: ChatThreadHandler.OnThreadUpdatedListener) = Cancellable { listener.onUpdated(NoThread) }

    override fun setName(name: String) {
        // No operation, as there is no thread to set the name for.
    }

    override fun refresh() {
        // No operation, as there is no thread to refresh.
    }

    override fun archive(onComplete: (Boolean) -> Unit) {
        onComplete(false)
    }

    override fun messages(): ChatThreadMessageHandler = object : ChatThreadMessageHandler {
        override fun loadMore() {
            // No operation, as there are no messages to load.
        }

        override fun send(message: OutboundMessage, listener: ChatThreadMessageHandler.OnMessageTransferListener?) {
            // No operation, as there is no thread to send messages to.
        }
    }

    override fun events(): ChatThreadEventHandler = object : ChatThreadEventHandler {
        override fun trigger(
            event: ChatThreadEvent,
            listener: ChatThreadEventHandler.OnEventSentListener?,
            errorListener: ChatThreadEventHandler.OnEventErrorListener?,
        ) {
            // No operation, as there is no thread to trigger events for.
        }
    }

    override fun actions(): ChatThreadActionHandler = object : ChatThreadActionHandler {
        override fun onPopup(listener: ChatThreadActionHandler.OnPopup) {
            // No operation, as there are no popups to show.
        }

        override fun close() {
            // No operation, as there is no listener to remove.
        }
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
