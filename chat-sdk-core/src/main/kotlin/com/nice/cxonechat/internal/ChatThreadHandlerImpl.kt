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

import com.nice.cxonechat.ChatFieldHandler
import com.nice.cxonechat.ChatMode
import com.nice.cxonechat.ChatThreadActionHandler
import com.nice.cxonechat.ChatThreadEventHandler
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadMessageHandler
import com.nice.cxonechat.event.RecoverThreadEvent
import com.nice.cxonechat.event.thread.UpdateThreadEvent
import com.nice.cxonechat.exceptions.InvalidStateException
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.updateWith
import com.nice.cxonechat.internal.model.ChatThreadMutable
import com.nice.cxonechat.internal.model.CustomFieldInternal.Companion.updateWith
import com.nice.cxonechat.internal.model.MessageText
import com.nice.cxonechat.internal.model.network.EventThreadRecovered
import com.nice.cxonechat.internal.model.network.EventThreadUpdated
import com.nice.cxonechat.internal.model.network.Parameters
import com.nice.cxonechat.internal.serializer.Default
import com.nice.cxonechat.internal.socket.EventCallback.Companion.eventFlow
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.debug
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.warning
import com.nice.cxonechat.message.Message
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState.Pending
import com.nice.cxonechat.thread.ChatThreadState.Ready
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart

@Suppress("TooManyFunctions")
internal class ChatThreadHandlerImpl(
    private val chat: ChatWithParameters,
    private val thread: ChatThreadMutable,
) : ChatThreadHandler, LoggerScope by LoggerScope("ChatThreadHandlerImpl", chat.entrails.logger) {

    override val threadFlow: Flow<ChatThread> = merge(
        chat.socketListener.eventFlow(EventThreadRecovered)
            .mapNotNull { (_, event) ->
                if (!event.inThread(thread)) return@mapNotNull null
                try {
                    updateFromEvent(event)
                    get()
                } catch (expected: CancellationException) {
                    throw expected
                } catch (expected: Exception) {
                    warning("Failed to handle thread-recovered event in threadFlow", expected)
                    null
                }
            },
        chat.socketListener.eventFlow(EventThreadUpdated)
            .mapNotNull { (_, event) ->
                if (!event.inThread(thread)) return@mapNotNull null
                try {
                    get()
                } catch (expected: CancellationException) {
                    throw expected
                } catch (expected: Exception) {
                    warning("Failed to handle thread-updated event in threadFlow", expected)
                    null
                }
            },
    ).onStart { emit(get()) }

    override fun get(): ChatThread = thread.snapshot()

    override fun refresh(): Unit = scope("refresh") {
        if (thread.threadState != Pending) {
            // TODO: propagate trigger() failures to callers once an error-reporting mechanism is in place
            //  (currently logged as warnings via safeLaunch)
            chat.entrails.threading.coroutineScope.safeLaunch(childScope("triggerRecoverThread")) {
                chat.events().trigger(RecoverThreadEvent(thread.id))
            }
        } else {
            debug("skipped - thread is in Pending state")
        }
    }

    override suspend fun archive(): Boolean = false

    private fun updateFromEvent(event: EventThreadRecovered) {
        val messages = event.messages
            .filterNot { message ->
                // Filter out unsupported messages type answers
                ((message as? MessageText)?.parameters as? Parameters.Object)?.isUnsupportedMessageTypeAnswer == true
            }
            .sortedBy(Message::createdAt)
        thread += thread.asCopyable().copy(
            threadName = event.thread.threadName,
            messages = thread.messages.updateWith(messages),
            scrollToken = event.scrollToken,
            /*
             * Maintain any existing agent if no new agent since ThreadRecovered
             * doesn't seem to reliably send the agent details.
             */
            threadAgent = event.agent ?: thread.threadAgent,
            fields = thread.fields.updateWith(
                event.thread.fields
            ),
            threadState = Ready,
        )
        chat.fields = chat.fields.updateWith(
            event.customerCustomFields
        )
    }

    override fun setName(name: String): Unit = scope("setName") {
        if (thread.threadState === Pending) {
            thread += thread.asCopyable().copy(threadName = name)
            sendThreadUpdated()
        } else {
            // TODO: propagate trigger() failures to callers once an error-reporting mechanism is in place
            //  (currently logged as warnings via safeLaunch)
            chat.entrails.threading.coroutineScope.safeLaunch(childScope("triggerUpdateThread")) {
                events().trigger(UpdateThreadEvent(name))
                thread += thread.asCopyable().copy(threadName = name)
            }
        }
    }

    override fun messages(): ChatThreadMessageHandler {
        var handler: ChatThreadMessageHandler
        handler = ChatThreadMessageHandlerImpl(chat, this, this)
        handler = ChatThreadArchivedMessageHandler(handler, this)
        handler = ChatThreadMessageHandlerProxy(handler, thread)
        handler = ChatThreadMessageHandlerAttachmentVerification(handler, chat)
        handler = ChatThreadMessageHandlerThreading(handler, chat)
        return handler
    }

    override fun events(): ChatThreadEventHandler {
        var handler: ChatThreadEventHandler
        handler = ChatThreadEventHandlerImpl(chat, thread)
        handler = ChatThreadEventHandlerReplyEvent(handler, this, chat)
        handler = ChatThreadEventHandlerTimeSlotEvent(handler, this, chat)
        handler = ChatThreadEventHandlerTokenGuard(handler, chat, chat.tokenRefreshCoordinator)
        handler = ChatThreadEventHandlerThreading(handler, chat)
        return handler
    }

    override fun actions(): ChatThreadActionHandler {
        var handler: ChatThreadActionHandler = ChatThreadActionHandlerImpl(chat, thread)
        handler = if (chat.chatMode === ChatMode.LiveChat) {
            ChatThreadActionHandlerLiveChat(ChatThreadActionHandlerImpl(chat, thread), chat, thread)
        } else {
            handler
        }
        handler = ChatThreadActionHandlerThreading(handler, chat)
        return handler
    }

    override fun endContact() {
        throw InvalidStateException("endContact is only valid for live chat channels")
    }

    override fun customFields(): ChatFieldHandler = ChatFieldHandlerThread(this, thread, chat.entrails.threading.coroutineScope, this)

    private fun sendThreadUpdated() {
        // send thread updated to host application
        chat.socket?.let { socket ->
            chat.socketListener.onMessage(
                socket,
                Default.serializer.encodeToString(EventThreadUpdated(thread))
            )
        }
    }
}
