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

import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.updateWith
import com.nice.cxonechat.internal.model.ChatThreadMutable
import com.nice.cxonechat.internal.model.network.EventMessageSeen
import com.nice.cxonechat.internal.socket.EventCallback.Companion.addCallback

/**
 * Update thread message with the message seen metadata.
 */
internal class ChatThreadHandlerMessageSeenChanged(
    private val origin: ChatThreadHandler,
    private val chat: ChatWithParameters,
    private val thread: ChatThreadMutable,
) : ChatThreadHandler by origin {

    override fun get(listener: ChatThreadHandler.OnThreadUpdatedListener): Cancellable {
        val cancellable = chat.socketListener.addCallback(EventMessageSeen) {
            val message = it.message
            if (message?.threadId == thread.id) {
                thread += thread.asCopyable().copy(
                    messages = thread.messages.updateWith(listOf(message))
                )
                listener.onUpdated(thread)
            }
        }
        return Cancellable(
            cancellable,
            origin.get(listener),
        )
    }
}
