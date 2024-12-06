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
import com.nice.cxonechat.ChatThreadsHandler
import com.nice.cxonechat.ChatThreadsHandler.OnThreadsUpdatedListener
import com.nice.cxonechat.enums.ContactStatus
import com.nice.cxonechat.enums.ErrorType.RecoveringLivechatFailed
import com.nice.cxonechat.enums.EventType
import com.nice.cxonechat.enums.EventType.LivechatRecovered
import com.nice.cxonechat.event.RecoverLiveChatThreadEvent
import com.nice.cxonechat.exceptions.RuntimeChatException.ServerCommunicationError
import com.nice.cxonechat.internal.ChatThreadHandlerLiveChat.Companion.removeConversationStarter
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.ChatThreadMutable
import com.nice.cxonechat.internal.model.ChatThreadMutable.Companion.asMutable
import com.nice.cxonechat.internal.model.network.EventCaseStatusChanged
import com.nice.cxonechat.internal.model.network.EventLiveChatThreadRecovered
import com.nice.cxonechat.internal.socket.ErrorCallback.Companion.addErrorCallback
import com.nice.cxonechat.internal.socket.EventCallback.Companion.addCallback
import com.nice.cxonechat.thread.ChatThread

internal class ChatThreadsHandlerLive(
    private val chat: ChatWithParameters,
    private val origin: ChatThreadsHandler,
) : ChatThreadsHandler by origin {

    private val tmpHandlerLock = Any()

    @Volatile
    private var tmpThreadHandlerRef: ChatThreadHandler? = null

    override fun refresh() {
        chat.events().trigger(RecoverLiveChatThreadEvent())
        origin.refresh()
    }

    override fun threads(listener: OnThreadsUpdatedListener): Cancellable {
        val cancellable = origin.threads(listener)
        var thread: ChatThreadMutable? = null
        val onSuccess = chat.socketListener.addCallback<EventLiveChatThreadRecovered>(LivechatRecovered) { event ->
            val recovered = handleLiveChatRecovered(event)
            thread = recovered?.asMutable()
            listener.onThreadsUpdated(listOfNotNull(recovered))
        }

        val onFailure = chat.socketListener.addErrorCallback(RecoveringLivechatFailed) {
            if (chat.configuration.hasRecoverLiveChatDoesNotFail()) {
                chat.chatStateListener?.onChatRuntimeException(ServerCommunicationError(RecoveringLivechatFailed.value))
            } else {
                val createdThread = createThreadIfPossible()
                thread = createdThread?.asMutable()
                listener.onThreadsUpdated(listOfNotNull(createdThread))
            }
        }

        val threadArchived = chat.socketListener.addCallback<EventCaseStatusChanged>(EventType.CaseStatusChanged) { event ->
            val currentThread = thread ?: return@addCallback
            CaseStatusChangedHandlerActions.handleCaseClosed(currentThread, event) {
                listener.onThreadsUpdated(listOf(it))
                synchronized(tmpHandlerLock) {
                    if (it.id == tmpThreadHandlerRef?.get()?.id) tmpThreadHandlerRef = null
                }
            }
        }

        refresh()

        return Cancellable(
            cancellable,
            onSuccess,
            onFailure,
            threadArchived
        )
    }

    private fun handleLiveChatRecovered(event: EventLiveChatThreadRecovered): ChatThread? {
        val eventThread = event.thread
        synchronized(tmpHandlerLock) {
            if (eventThread != null && eventThread.id == tmpThreadHandlerRef?.get()?.id) {
                tmpThreadHandlerRef = null
            }
        }
        val recovered = if (eventThread == null || !eventThread.canAddMoreMessages || event.lastContactStatus === ContactStatus.Closed) {
            createThreadIfPossible()
        } else {
            eventThread.asCopyable().copy(
                messages = event.messages.removeConversationStarter(),
                threadState = event.threadState
            )
        }
        return recovered
    }

    private fun createThreadIfPossible() =
        if (preChatSurvey == null) {
            synchronized(tmpHandlerLock) {
                tmpThreadHandlerRef?.get() ?: create().also { tmpThreadHandlerRef = it }.get()
            }
        } else {
            null
        }
}
