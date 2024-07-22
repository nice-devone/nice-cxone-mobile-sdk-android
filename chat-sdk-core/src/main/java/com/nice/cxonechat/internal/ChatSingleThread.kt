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
import com.nice.cxonechat.enums.ErrorType.RecoveringThreadFailed
import com.nice.cxonechat.enums.EventType.ThreadRecovered
import com.nice.cxonechat.event.RecoverThreadEvent
import com.nice.cxonechat.internal.model.network.EventThreadRecovered
import com.nice.cxonechat.internal.socket.ErrorCallback.Companion.addErrorCallback
import com.nice.cxonechat.internal.socket.EventCallback.Companion.addCallback
import com.nice.cxonechat.internal.socket.SocketConnectionListener
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState.Ready
import okhttp3.WebSocketListener

/**
 * This implementation of [com.nice.cxonechat.Chat] adds behavior which triggers early thread recovery if there is an
 * existing thread.
 * Once the [origin] [com.nice.cxonechat.Chat.connect] is finished, the mandatory [com.nice.cxonechat.Chat.threads] call
 * is performed and the first existing thread is then `refreshed` once it's metadata are loaded.
 * In order for these tasks to be of any use to the [com.nice.cxonechat.Chat] user, the [com.nice.cxonechat.Chat] has to
 * memoize both the [com.nice.cxonechat.ChatThreadsHandler] and the [com.nice.cxonechat.ChatThreadHandler].
 *
 * @param origin Existing implementation of [ChatWithParameters] used for delegation.
 */
internal class ChatSingleThread(
    private val origin: ChatWithParameters,
) : ChatWithParameters by origin,
    WebSocketListener() {
    private var thread: ChatThread? = null

    init {
        origin.socketListener.addListener(SocketConnectionListener(listener = origin.chatStateListener, onConnected = ::recoverThread))
    }

    override fun connect(): Cancellable {
        val cancellable = origin.connect()
        val onSuccess = socketListener.addCallback<EventThreadRecovered>(ThreadRecovered) { event ->
            thread = event.thread.copy(threadState = Ready)
            chatStateListener?.onReady()
        }
        val onFailure = socketListener.addErrorCallback(RecoveringThreadFailed) {
            thread = null
            chatStateListener?.onReady()
        }

        return Cancellable(cancellable, onSuccess, onFailure)
    }

    private fun recoverThread() {
        origin.chatStateListener?.onConnected()
        origin.events().trigger(RecoverThreadEvent(null))
    }
}
