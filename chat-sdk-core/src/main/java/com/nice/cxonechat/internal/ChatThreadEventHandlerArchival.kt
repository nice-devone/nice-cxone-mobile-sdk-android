/*
 * Copyright (c) 2021-2023. NICE Ltd. All rights reserved.
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
import com.nice.cxonechat.ChatThreadEventHandler.OnEventSentListener
import com.nice.cxonechat.event.thread.ArchiveThreadEvent
import com.nice.cxonechat.event.thread.ChatThreadEvent
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.ChatThreadMutable
import com.nice.cxonechat.internal.model.network.EventThreadUpdated
import com.nice.cxonechat.internal.serializer.Default.serializer

internal class ChatThreadEventHandlerArchival(
    private val origin: ChatThreadEventHandler,
    private val chat: ChatWithParameters,
    private val thread: ChatThreadMutable
): ChatThreadEventHandler by origin {
    override fun trigger(event: ChatThreadEvent, listener: OnEventSentListener?) {
        origin.trigger(event) {
            if(event is ArchiveThreadEvent) {
                handleArchiveThread()
            }
            listener?.onSent()
        }
    }

    private fun handleArchiveThread() {
        // mark this thread as archived
        thread += thread.asCopyable().copy(canAddMoreMessages = false)

        // send thread updated to host application
        chat.socketListener.onMessage(
            chat.socket,
            serializer.toJson(EventThreadUpdated(thread))
        )
    }
}
