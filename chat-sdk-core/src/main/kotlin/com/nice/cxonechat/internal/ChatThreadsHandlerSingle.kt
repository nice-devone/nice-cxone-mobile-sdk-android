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

package com.nice.cxonechat.internal

import com.nice.cxonechat.ChatThreadsHandler
import com.nice.cxonechat.enums.ErrorType.RecoveringThreadFailed
import com.nice.cxonechat.enums.EventType.ThreadRecovered
import com.nice.cxonechat.event.RecoverThreadEvent
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.ChatThreadMutable.Companion.asMutable
import com.nice.cxonechat.internal.model.network.EventThreadRecovered
import com.nice.cxonechat.internal.socket.ErrorCallback.Companion.errorFlow
import com.nice.cxonechat.internal.socket.EventCallback.Companion.eventFlow
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.warning
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState.Ready
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn

internal class ChatThreadsHandlerSingle(
    private val chat: ChatWithParameters,
    private val origin: ChatThreadsHandler,
) : ChatThreadsHandler by origin, LoggerScope by LoggerScope("ChatThreadsHandlerSingle", chat.entrails.logger) {

    private val onSuccess =
        chat.socketListener.eventFlow<EventThreadRecovered>(ThreadRecovered).mapNotNull { (_, event) ->
            try {
                val thread = listOf(event.thread.asCopyable().copy(threadState = Ready).asMutable())
                chat.chatStateListener?.onReady()
                thread
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (logged: Exception) {
                warning("Failed to handle thread-recovered event", logged)
                null
            }
        }

    private val onFailure =
        chat.socketListener.errorFlow(RecoveringThreadFailed).mapNotNull {
            try {
                chat.chatStateListener?.onReady()
                emptyList<ChatThread>()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (logged: Exception) {
                warning("Failed to handle thread-recovery-failed event", logged)
                null
            }
        }

    private val authoritativeFlow: SharedFlow<List<ChatThread>> = merge(origin.threadsFlow, onSuccess)
        .onStart { refresh() }
        .shareIn(chat.entrails.threading.coroutineScope, SharingStarted.Lazily, replay = 1)

    private val onFailureShared: SharedFlow<List<ChatThread>> = onFailure
        .shareIn(chat.entrails.threading.coroutineScope, SharingStarted.Lazily, replay = 0)

    override val threadsFlow: Flow<List<ChatThread>> = merge(authoritativeFlow, onFailureShared)

    override fun refresh(): Unit = scope("refresh") {
        chat.entrails.threading.coroutineScope.safeLaunch(childScope("triggerRecoverThread")) {
            chat.events().trigger(RecoverThreadEvent(null))
        }
        origin.refresh()
    }
}
