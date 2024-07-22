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

import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadHandler.OnThreadUpdatedListener
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.updateWith
import com.nice.cxonechat.internal.model.ChatThreadMutable
import com.nice.cxonechat.internal.model.network.EventMessageCreated
import com.nice.cxonechat.internal.model.network.EventMoreMessagesLoaded
import com.nice.cxonechat.internal.socket.EventCallback.Companion.addCallback

internal class ChatThreadHandlerMessages(
    private val origin: ChatThreadHandler,
    private val chat: ChatWithParameters,
    private val thread: ChatThreadMutable,
) : ChatThreadHandler by origin {

    override fun get(listener: OnThreadUpdatedListener): Cancellable {
        val moreMessagesListener = chat.socketListener
            .addCallback(EventMoreMessagesLoaded) { event ->
                if (!event.inThread(thread)) return@addCallback
                thread += thread.asCopyable().copy(
                    messages = thread.messages.updateWith(event.messages),
                    scrollToken = event.scrollToken
                )
                listener.onUpdated(thread)
            }
        val messageCreated = chat.socketListener.addCallback(EventMessageCreated) { event ->
            val message = event.message
            if (!event.inThread(thread) || thread.messages.contains(message)) return@addCallback
            thread += thread.asCopyable().copy(
                messages = thread.messages.updateWith(listOfNotNull(message))
            )
            listener.onUpdated(thread)
        }
        return Cancellable(
            moreMessagesListener,
            messageCreated,
            origin.get(listener)
        )
    }
}
