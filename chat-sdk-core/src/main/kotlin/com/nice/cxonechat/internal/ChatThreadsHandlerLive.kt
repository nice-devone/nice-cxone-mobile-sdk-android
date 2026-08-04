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

import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadsHandler
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
import com.nice.cxonechat.internal.socket.ErrorCallback.Companion.errorFlow
import com.nice.cxonechat.internal.socket.EventCallback.Companion.eventFlow
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.warning
import com.nice.cxonechat.prechat.PreChatSurveyResponse
import com.nice.cxonechat.state.FieldDefinition
import com.nice.cxonechat.thread.ChatThread
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn

internal class ChatThreadsHandlerLive(
    private val chat: ChatWithParameters,
    private val origin: ChatThreadsHandler,
) : ChatThreadsHandler by origin, LoggerScope by LoggerScope("ChatThreadsHandlerLive", chat.entrails.logger) {

    private val stateLock = Any()

    @Volatile
    private var tmpThreadHandlerRef: ChatThreadHandler? = null

    @Volatile
    private var thread: ChatThreadMutable? = null

    private val onSuccess =
        chat.socketListener.eventFlow<EventLiveChatThreadRecovered>(LivechatRecovered).mapNotNull { (_, event) ->
            try {
                val recovered = handleLiveChatRecovered(event)
                synchronized(stateLock) { thread = recovered?.asMutable() }
                listOfNotNull(recovered)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (logged: Exception) {
                warning("Failed to handle livechat-recovered event", logged)
                null
            }
        }

    private val onFailure =
        chat.socketListener.errorFlow(RecoveringLivechatFailed).mapNotNull {
            try {
                if (chat.configuration.hasRecoverLiveChatDoesNotFail()) {
                    warning("Live chat recovery failed; configuration suppresses fallback thread creation")
                    chat.chatStateListener?.onChatRuntimeException(ServerCommunicationError(RecoveringLivechatFailed.value))
                    null
                } else {
                    val createdThread = createThreadIfPossible()
                    if (createdThread == null) {
                        warning("Live chat recovery failed and no pre-chat survey thread could be created")
                        chat.chatStateListener?.onChatRuntimeException(
                            ServerCommunicationError(RecoveringLivechatFailed.value)
                        )
                    }
                    synchronized(stateLock) { thread = createdThread?.asMutable() }
                    listOfNotNull(createdThread)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (logged: Exception) {
                warning("Failed to handle livechat-recovery-failed event", logged)
                null
            }
        }

    private val threadArchived =
        chat.socketListener.eventFlow<EventCaseStatusChanged>(EventType.CaseStatusChanged).mapNotNull { (_, event) ->
            try {
                val currentThread = synchronized(stateLock) { thread } ?: return@mapNotNull null
                var result: List<ChatThread>? = null
                CaseStatusChangedHandlerActions.handleCaseClosed(currentThread, event) { updated ->
                    result = listOf(updated)
                    synchronized(stateLock) {
                        if (updated.id == tmpThreadHandlerRef?.get()?.id) tmpThreadHandlerRef = null
                    }
                }
                result
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (logged: Exception) {
                warning("Failed to handle case-status-changed event", logged)
                null
            }
        }

    private val authoritativeFlow: SharedFlow<List<ChatThread>> = merge(origin.threadsFlow, onSuccess, threadArchived)
        .onStart { refresh() }
        .shareIn(chat.entrails.threading.coroutineScope, SharingStarted.Lazily, replay = 1)

    private val onFailureShared: SharedFlow<List<ChatThread>> = onFailure
        .shareIn(chat.entrails.threading.coroutineScope, SharingStarted.Lazily, replay = 0)

    override val threadsFlow: Flow<List<ChatThread>> = merge(authoritativeFlow, onFailureShared)

    override fun refresh(): Unit = scope("refresh") {
        chat.entrails.threading.coroutineScope.safeLaunch(childScope("triggerRecoverLiveChat")) {
            chat.events().trigger(RecoverLiveChatThreadEvent())
        }
        origin.refresh()
    }

    override fun create(): ChatThreadHandler = create(emptyMap(), emptySequence())

    override fun create(customFields: Map<String, String>): ChatThreadHandler =
        create(customFields, emptySequence())

    override fun create(
        preChatSurveyResponse: Sequence<PreChatSurveyResponse<out FieldDefinition, out Any>>,
    ): ChatThreadHandler = create(emptyMap(), preChatSurveyResponse)

    override fun create(
        customFields: Map<String, String>,
        preChatSurveyResponse: Sequence<PreChatSurveyResponse<out FieldDefinition, out Any>>,
    ): ChatThreadHandler = origin.create(customFields, preChatSurveyResponse).also { handler ->
        synchronized(stateLock) { thread = handler.get().asMutable() }
    }

    private fun handleLiveChatRecovered(event: EventLiveChatThreadRecovered): ChatThread? {
        val eventThread = event.thread
        synchronized(stateLock) {
            if (eventThread != null && eventThread.id == tmpThreadHandlerRef?.get()?.id) {
                tmpThreadHandlerRef = null
            }
        }
        return if (eventThread == null || !eventThread.canAddMoreMessages || event.lastContactStatus === ContactStatus.Closed) {
            createThreadIfPossible()
        } else {
            eventThread.asCopyable().copy(
                messages = event.messages.removeConversationStarter(),
                threadState = event.threadState
            )
        }
    }

    private fun createThreadIfPossible() =
        if (preChatSurvey == null) {
            synchronized(stateLock) {
                tmpThreadHandlerRef?.get() ?: create().also { tmpThreadHandlerRef = it }.get()
            }
        } else {
            null
        }
}
