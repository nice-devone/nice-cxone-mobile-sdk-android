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

package com.nice.cxonechat.internal

import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.enums.ErrorType.ArchivingThreadFailed
import com.nice.cxonechat.event.thread.ArchiveThreadEventImpl
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.ChatThreadMutable
import com.nice.cxonechat.internal.model.network.EventThreadArchived
import com.nice.cxonechat.internal.model.network.EventThreadUpdated
import com.nice.cxonechat.internal.serializer.Default
import com.nice.cxonechat.internal.socket.EventCallback.Companion.acceptResponse

internal class ChatThreadHandlerMulti(
    private val chat: ChatWithParameters,
    private val thread: ChatThreadMutable,
    private val origin: ChatThreadHandler,
) : ChatThreadHandler by origin {
    override fun archive(onComplete: (Boolean) -> Unit) {
        val event = ArchiveThreadEventImpl()

        // Mark the thread as archived until we get a response
        val wasArchived = markArchived(archived = true)

        fun result(success: Boolean) {
            markArchived(archived = if (success) true else wasArchived)
            thread.resultCallbacks.remove(event.eventId)?.cancel()
            onComplete(success)
        }

        thread.resultCallbacks[event.eventId] = chat.socketListener.acceptResponse(
            event,
            EventThreadArchived,
            ArchivingThreadFailed,
            { result(success = false) }
        ) { result(success = true) }

        events().trigger(event, errorListener = { result(success = false) })
    }

    private fun markArchived(archived: Boolean) = !thread.canAddMoreMessages.also {
        thread += thread.asCopyable().copy(canAddMoreMessages = !archived)
        sendThreadUpdated()
    }

    private fun sendThreadUpdated() {
        // send thread updated to host application
        chat.socket?.let { socket ->
            chat.socketListener.onMessage(
                socket,
                Default.serializer.toJson(EventThreadUpdated(thread))
            )
        }
    }
}
