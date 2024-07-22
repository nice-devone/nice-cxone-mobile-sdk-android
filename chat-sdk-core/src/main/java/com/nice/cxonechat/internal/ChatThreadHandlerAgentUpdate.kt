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
import com.nice.cxonechat.internal.model.ChatThreadMutable
import com.nice.cxonechat.internal.model.network.EventContactInboxAssigneeChanged
import com.nice.cxonechat.internal.socket.EventCallback.Companion.addCallback

/**
 * This class wraps origin [ChatThreadHandler] and adds effect to it's [get] function, which
 * will update the mutable [thread] and also trigger [OnThreadUpdatedListener.onUpdated] callback
 * with updated thread, when [EventContactInboxAssigneeChanged] is received.
 *
 * [EventContactInboxAssigneeChanged] will always cause an update of thread agent.
 */
internal class ChatThreadHandlerAgentUpdate(
    private val origin: ChatThreadHandler,
    private val chat: ChatWithParameters,
    private val thread: ChatThreadMutable,
) : ChatThreadHandler by origin {

    override fun get(listener: OnThreadUpdatedListener): Cancellable {
        val cancellable = chat.socketListener
            .addCallback(EventContactInboxAssigneeChanged) { event ->
                if (!event.inThread(thread)) return@addCallback
                thread += thread.asCopyable().copy(
                    threadAgent = event.agent
                )
                listener.onUpdated(thread)
            }
        return Cancellable(
            cancellable,
            origin.get(listener)
        )
    }
}
