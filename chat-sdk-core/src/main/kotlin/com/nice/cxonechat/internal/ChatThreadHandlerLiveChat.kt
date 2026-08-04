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
import com.nice.cxonechat.enums.EventType.CaseInboxAssigneeChanged
import com.nice.cxonechat.enums.EventType.CaseStatusChanged
import com.nice.cxonechat.enums.EventType.LivechatRecovered
import com.nice.cxonechat.enums.EventType.SetPositionInQueue
import com.nice.cxonechat.event.RecoverLiveChatThreadEvent
import com.nice.cxonechat.event.thread.EndContactEvent
import com.nice.cxonechat.exceptions.InvalidStateException
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.updateWith
import com.nice.cxonechat.internal.model.ChatThreadMutable
import com.nice.cxonechat.internal.model.CustomFieldInternal.Companion.updateWith
import com.nice.cxonechat.internal.model.MessageText
import com.nice.cxonechat.internal.model.network.EventCaseStatusChanged
import com.nice.cxonechat.internal.model.network.EventContactInboxAssigneeChanged
import com.nice.cxonechat.internal.model.network.EventLiveChatThreadRecovered
import com.nice.cxonechat.internal.model.network.EventSetPositionInQueue
import com.nice.cxonechat.internal.model.network.Parameters.Object
import com.nice.cxonechat.internal.socket.EventCallback.Companion.eventFlow
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.warning
import com.nice.cxonechat.message.Message
import com.nice.cxonechat.message.Message.Text
import com.nice.cxonechat.message.MessageDirection
import com.nice.cxonechat.message.OutboundMessage.Companion.LiveChatBeginOutboundMessage
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState.Closed
import com.nice.cxonechat.thread.ChatThreadState.Pending
import com.nice.cxonechat.thread.ChatThreadState.Ready
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge

/**
 * This class wraps origin [ChatThreadHandler] and adds handling specific to Live Chat:
 *
 * * listens for [SetPositionInQueue] events, updating the position and agent availability.
 * * implements [endContact] to send [EndContactEvent].
 */
internal class ChatThreadHandlerLiveChat(
    private val origin: ChatThreadHandler,
    private val chat: ChatWithParameters,
    private val thread: ChatThreadMutable,
    isThreadCreated: Boolean,
) : ChatThreadHandler by origin, LoggerScope by LoggerScope("ChatThreadHandlerLiveChat", chat.entrails.logger) {

    private val _recoveryNotification = MutableSharedFlow<ChatThread>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    init {
        scope("init") {
            eagerLiveChatRecovery()
            triggerInitialRecovery(isThreadCreated)
            sendBeginConversation(isThreadCreated)
        }
    }

    override val threadFlow: Flow<ChatThread> = merge(
        origin.threadFlow
            .mapNotNull { updated ->
                try {
                    thread += updated.asCopyable().copy(
                        messages = thread.messages.removeConversationStarter(),
                        scrollToken = thread.getScrollTokenForStart()
                    )
                    get()
                } catch (expected: CancellationException) {
                    throw expected
                } catch (expected: Exception) {
                    warning("Failed to handle origin thread update in threadFlow", expected)
                    null
                }
            },
        chat.socketListener.eventFlow<EventSetPositionInQueue>(SetPositionInQueue)
            .mapNotNull { (_, event) ->
                try {
                    thread += thread.asCopyable().copy(
                        contactId = event.consumerContact,
                        positionInQueue = event.positionInQueue,
                        hasOnlineAgent = event.hasOnlineAgent,
                        messages = thread.messages.removeConversationStarter(),
                        scrollToken = thread.getScrollTokenForStart(),
                    )
                    get()
                } catch (expected: CancellationException) {
                    throw expected
                } catch (expected: Exception) {
                    warning("Failed to handle set-position-in-queue event in threadFlow", expected)
                    null
                }
            },
        chat.socketListener.eventFlow<EventContactInboxAssigneeChanged>(CaseInboxAssigneeChanged)
            .mapNotNull { (_, event) ->
                if (!event.inThread(thread)) return@mapNotNull null
                try {
                    thread += thread.asCopyable().copy(
                        contactId = event.case.id,
                        positionInQueue = null,
                        threadState = Ready,
                        messages = thread.messages.removeConversationStarter(),
                        scrollToken = thread.getScrollTokenForStart(),
                    )
                    get()
                } catch (expected: CancellationException) {
                    throw expected
                } catch (expected: Exception) {
                    warning("Failed to handle case-inbox-assignee-changed event in threadFlow", expected)
                    null
                }
            },
        _recoveryNotification,
        chat.socketListener.eventFlow<EventCaseStatusChanged>(CaseStatusChanged)
            .mapNotNull { (_, event) ->
                try {
                    var updated = false
                    CaseStatusChangedHandlerActions.handleCaseClosed(thread, event) { _ -> updated = true }
                    if (!updated) return@mapNotNull null
                    thread += thread.asCopyable().copy(
                        messages = thread.messages.removeConversationStarter(),
                        scrollToken = thread.getScrollTokenForStart()
                    )
                    get()
                } catch (expected: CancellationException) {
                    throw expected
                } catch (expected: Exception) {
                    warning("Failed to handle case-status-changed event in threadFlow", expected)
                    null
                }
            },
    )

    /**
     * Eagerly subscribe to recovery events so the state update is never missed,
     * regardless of when downstream collectors attach to threadFlow.
     */
    private fun eagerLiveChatRecovery() {
        chat.entrails.threading.coroutineScope.safeLaunch(childScope("eagerLiveChatRecovery")) {
            chat.socketListener.eventFlow<EventLiveChatThreadRecovered>(LivechatRecovered)
                .filter { (_, event) -> event.thread?.id == thread.id }
                .safeCollect(childScope("eagerLiveChatRecovery")) { (_, event) ->
                    updateFromEvent(event)
                    thread += thread.asCopyable().copy(
                        messages = thread.messages.removeConversationStarter(),
                        scrollToken = thread.getScrollTokenForStart()
                    )
                    _recoveryNotification.tryEmit(get())
                }
        }
    }

    /**
     * For an existing thread still in Pending state, the handler may be constructed after
     * the initial recovery event was already consumed by connect() or the thread list.
     * Re-trigger recovery so eagerLiveChatRecovery above can apply the real server state.
     */
    private fun triggerInitialRecovery(isThreadCreated: Boolean) {
        if (thread.threadState === Pending && !isThreadCreated) {
            chat.entrails.threading.coroutineScope.safeLaunch(childScope("triggerInitialRecovery")) {
                chat.events().trigger(RecoverLiveChatThreadEvent(thread.id))
            }
        }
    }

    private fun sendBeginConversation(isThreadCreated: Boolean) {
        if (isThreadCreated) {
            chat.entrails.threading.coroutineScope.safeLaunch(childScope("sendBeginConversation")) {
                if (thread.messages.none { it.direction === MessageDirection.ToAgent } &&
                    thread.threadState === Pending
                ) {
                    origin.messages().send(LiveChatBeginOutboundMessage(BEGIN_CONVERSATION_MESSAGE))
                }
            }
        }
    }

    override fun endContact(): Unit = scope("endContact") {
        when (thread.threadState) {
            Closed -> warning("Unable to endContact for a thread that is already closed.")
            Ready -> chat.entrails.threading.coroutineScope.safeLaunch(childScope("triggerEndContact")) {
                events().trigger(EndContactEvent)
            }

            else -> throw InvalidStateException("Unable to end contact before the thread is in the Ready state")
        }
    }

    override fun refresh(): Unit = scope("refresh") {
        if (thread.threadState != Pending) {
            chat.entrails.threading.coroutineScope.safeLaunch(childScope("triggerRecoverLiveChat")) {
                chat.events().trigger(RecoverLiveChatThreadEvent(thread.id))
            }
        }
    }

    private fun updateFromEvent(event: EventLiveChatThreadRecovered) {
        if (event.threadState === Closed) return
        val messages = event.messages.sortedBy(Message::createdAt)
        val eventThread = event.thread
        thread += thread.asCopyable().copy(
            contactId = if (eventThread != null) eventThread.contactId else thread.contactId,
            threadName = eventThread?.threadName,
            messages = thread.messages.updateWith(messages),
            scrollToken = event.scrollToken,
            threadAgent = event.agent ?: thread.threadAgent,
            fields = thread.fields.updateWith(
                eventThread?.fields.orEmpty()
            ),
            threadState = event.threadState
        )
        chat.fields = chat.fields.updateWith(
            event.customerCustomFields
        )
    }

    internal companion object {
        internal const val BEGIN_CONVERSATION_MESSAGE = "Begin Conversation"

        internal fun List<Message>.removeConversationStarter(): List<Message> {
            return this.filterNot { message ->
                (message as? MessageText)?.let {
                    val isBeginLiveChat = when (val parameters = message.parameters) {
                        is Object -> parameters.isInitialMessage == true
                        else -> false
                    }

                    it.text == BEGIN_CONVERSATION_MESSAGE && isBeginLiveChat
                } == true
            }
        }

        private fun isMessageConversationStart(it: Message) = (it as? Text)?.text == BEGIN_CONVERSATION_MESSAGE

        private fun ChatThread.getScrollTokenForStart() =
            if (messages.size == 1 && isMessageConversationStart(messages[0])) "" else scrollToken
    }
}
