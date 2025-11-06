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

package com.nice.cxonechat.internal

import com.nice.cxonechat.ChatThreadEventHandler
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadMessageHandler.OnMessageTransferListener
import com.nice.cxonechat.event.thread.ChatThreadEvent
import com.nice.cxonechat.event.thread.ReplyButtonEvent
import java.util.UUID

/**
 * This class is responsible for handling reply button events, which
 * should be sent as a message in the chat thread.
 */
internal class ChatThreadEventHandlerReplyEvent(
    private val handler: ChatThreadEventHandler,
    private val thread: ChatThreadHandler,
    private val chat: ChatWithParameters,
) : ChatThreadEventHandler by handler {

    override fun trigger(
        event: ChatThreadEvent,
        listener: ChatThreadEventHandler.OnEventSentListener?,
        errorListener: ChatThreadEventHandler.OnEventErrorListener?,
    ) {
        if (event is ReplyButtonEvent) {
            thread.messages().send(
                message = event.getModel(thread.get(), chat.connection),
                listener = listener?.let {
                    object : OnMessageTransferListener {
                        override fun onSent(id: UUID) = it.onSent()
                    }
                }
            )
        } else {
            handler.trigger(event, listener, errorListener)
        }
    }
}
