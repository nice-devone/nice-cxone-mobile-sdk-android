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
import com.nice.cxonechat.ChatThreadsHandler
import com.nice.cxonechat.ChatThreadsHandler.OnThreadsUpdatedListener
import com.nice.cxonechat.enums.ErrorType.RecoveringThreadFailed
import com.nice.cxonechat.enums.EventType.ThreadRecovered
import com.nice.cxonechat.event.RecoverThreadEvent
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.ChatThreadMutable.Companion.asMutable
import com.nice.cxonechat.internal.model.network.EventThreadRecovered
import com.nice.cxonechat.internal.socket.ErrorCallback.Companion.addErrorCallback
import com.nice.cxonechat.internal.socket.EventCallback.Companion.addCallback
import com.nice.cxonechat.thread.ChatThreadState.Ready

internal class ChatThreadsHandlerSingle(
    private val chat: ChatWithParameters,
    private val origin: ChatThreadsHandler,
) : ChatThreadsHandler by origin {
    override fun refresh() {
        chat.events().trigger(RecoverThreadEvent(null))
        origin.refresh()
    }

    override fun threads(listener: OnThreadsUpdatedListener): Cancellable {
        val cancellable = origin.threads(listener)

        val onSuccess = chat.socketListener.addCallback<EventThreadRecovered>(ThreadRecovered) { event ->
            val thread = event.thread.asCopyable().copy(threadState = Ready).asMutable()
            listener.onThreadsUpdated(listOf(thread))
            chat.chatStateListener?.onReady()
        }

        val onFailure = chat.socketListener.addErrorCallback(RecoveringThreadFailed) {
            listener.onThreadsUpdated(listOf())
            chat.chatStateListener?.onReady()
        }

        refresh()

        return Cancellable(cancellable, onSuccess, onFailure)
    }
}
