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
import com.nice.cxonechat.ChatFieldHandler
import com.nice.cxonechat.ChatMode.SingleThread
import com.nice.cxonechat.ChatThreadEventHandler
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadHandler.OnThreadUpdatedListener
import com.nice.cxonechat.ChatThreadMessageHandler
import com.nice.cxonechat.event.RecoverThreadEvent
import com.nice.cxonechat.event.thread.UpdateThreadEvent
import com.nice.cxonechat.exceptions.InvalidStateException
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.updateWith
import com.nice.cxonechat.internal.model.ChatThreadMutable
import com.nice.cxonechat.internal.model.CustomFieldInternal.Companion.updateWith
import com.nice.cxonechat.internal.model.network.EventCaseStatusChanged
import com.nice.cxonechat.internal.model.network.EventThreadRecovered
import com.nice.cxonechat.internal.model.network.EventThreadUpdated
import com.nice.cxonechat.internal.socket.EventCallback.Companion.addCallback
import com.nice.cxonechat.message.Message
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState.Pending
import com.nice.cxonechat.thread.ChatThreadState.Ready

@Suppress("TooManyFunctions")
internal class ChatThreadHandlerImpl(
    private val chat: ChatWithParameters,
    private val thread: ChatThreadMutable,
) : ChatThreadHandler {

    override fun get(): ChatThread = thread.snapshot()

    override fun get(listener: OnThreadUpdatedListener): Cancellable {
        val onRecovered = chat.socketListener.addCallback(EventThreadRecovered) { event ->
            if (event.inThread(thread)) {
                updateFromEvent(event)
                listener.onUpdated(thread)
            }
        }
        val onUpdated = chat.socketListener.addCallback(EventThreadUpdated) {
            listener.onUpdated(thread)
        }
        val onArchived = if (chat.chatMode !== SingleThread) {
            chat.socketListener.addCallback(EventCaseStatusChanged) { event ->
                CaseStatusChangedHandlerActions.handleCaseClosed(thread, event, listener::onUpdated)
            }
        } else {
            Cancellable.noop
        }
        return Cancellable(onRecovered, onUpdated, onArchived)
    }

    override fun refresh() {
        if (thread.threadState != Pending) {
            chat.events().trigger(RecoverThreadEvent(thread.id))
        }
    }

    override fun archive(onComplete: (Boolean) -> Unit) {
        onComplete(false)
    }

    private fun updateFromEvent(event: EventThreadRecovered) {
        val messages = event.messages.sortedBy(Message::createdAt)
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

    override fun setName(name: String) {
        events().trigger(
            event = UpdateThreadEvent(name),
            listener = {
                thread += thread.asCopyable().copy(threadName = name)
            },
        )
    }

    override fun messages(): ChatThreadMessageHandler {
        var handler: ChatThreadMessageHandler
        handler = ChatThreadMessageHandlerImpl(chat, this)
        handler = ChatThreadArchivedMessageHandler(handler, this)
        handler = ChatThreadMessageHandlerProxy(handler, thread)
        handler = ChatThreadMessageHandlerAttachmentVerification(handler, chat)
        handler = ChatThreadMessageHandlerThreading(handler, chat)
        return handler
    }

    override fun events(): ChatThreadEventHandler {
        var handler: ChatThreadEventHandler
        handler = ChatThreadEventHandlerImpl(chat, thread)
        handler = ChatThreadEventHandlerTokenGuard(handler, chat)
        handler = ChatThreadEventHandlerThreading(handler, chat)
        return handler
    }

    override fun endContact() {
        throw InvalidStateException("endContact is only valid for live chat channels")
    }

    override fun customFields(): ChatFieldHandler = ChatFieldHandlerThread(this, thread)
}
