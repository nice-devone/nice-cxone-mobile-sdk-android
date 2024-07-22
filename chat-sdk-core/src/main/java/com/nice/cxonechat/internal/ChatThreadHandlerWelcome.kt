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
import com.nice.cxonechat.Cancellable.Companion.asCancellable
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadHandler.OnThreadUpdatedListener
import com.nice.cxonechat.ChatThreadMessageHandler
import com.nice.cxonechat.ChatThreadMessageHandler.OnMessageTransferListener
import com.nice.cxonechat.event.thread.SendOutboundEvent
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.ChatThreadMutable
import com.nice.cxonechat.internal.model.MessageDirectionModel.ToClient
import com.nice.cxonechat.internal.model.MessageModel
import com.nice.cxonechat.internal.model.MessageText
import com.nice.cxonechat.internal.model.network.MessagePolyContent.Text
import com.nice.cxonechat.internal.model.network.MessagePolyContent.Text.Payload
import com.nice.cxonechat.internal.model.network.UserStatistics
import com.nice.cxonechat.message.OutboundMessage
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.CustomField
import com.nice.cxonechat.util.UUIDProvider
import java.util.Date
import java.util.concurrent.FutureTask
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

internal class ChatThreadHandlerWelcome(
    private val origin: ChatThreadHandler,
    private val chat: ChatWithParameters,
    private val mutableThread: ChatThreadMutable,
) : ChatThreadHandler by origin {

    private val sendOutboundEvent = AtomicBoolean(mutableThread.messages.isEmpty())
    private val removeTemporaryMessage = AtomicReference<SendOutboundEvent?>(null)

    private val prepareWelcomeMessageTask: FutureTask<SendOutboundEvent?> =
        FutureTask(::addMessageAndPrepareEvent)

    init {
        if (sendOutboundEvent.get()) {
            chat.entrails.threading.background(prepareWelcomeMessageTask)
        } else {
            prepareWelcomeMessageTask.cancel(true)
        }
    }

    override fun get(): ChatThread {
        runCatching { prepareWelcomeMessageTask.get() } // Await completion
        return origin.get()
    }

    override fun get(listener: OnThreadUpdatedListener): Cancellable {
        val notifyListenerWithWelcomeMessage = chat.entrails.threading.background {
            listener.onUpdated(get())
        }
        val duplicateRemover = OnThreadUpdatedListener { thread ->
            val event = removeTemporaryMessage.get()
            if (event != null) {
                val containsMessage = thread.messages
                    .filterIsInstance<MessageText>()
                    .any { msg -> msg.id != event.id && msg.text == event.message }
                if (containsMessage) {
                    mutableThread += mutableThread.asCopyable().copy(
                        messages = mutableThread.messages.dropWhile { msg -> msg.id == event.id }
                    )
                    removeTemporaryMessage.set(null)
                    listener.onUpdated(mutableThread.snapshot())
                } else {
                    listener.onUpdated(thread)
                }
            } else {
                listener.onUpdated(thread)
            }
        }
        return Cancellable(
            notifyListenerWithWelcomeMessage,
            prepareWelcomeMessageTask.asCancellable(),
            origin.get(duplicateRemover)
        )
    }

    override fun messages(): ChatThreadMessageHandler {
        var handler = origin.messages()
        if (sendOutboundEvent.get()) {
            handler = WelcomeThreadMessageHandler(handler)
        }
        return handler
    }

    private inner class WelcomeThreadMessageHandler(
        private val originHandler: ChatThreadMessageHandler,
    ) : ChatThreadMessageHandler by originHandler {
        override fun send(message: OutboundMessage, listener: OnMessageTransferListener?) {
            chat.entrails.threading.background {
                if (sendOutboundEvent.getAndSet(false)) {
                    val welcomeMessageEvent = prepareWelcomeMessageTask.get()
                    welcomeMessageEvent?.let(events()::trigger)
                    originHandler.send(message = message, listener = listener)
                    welcomeMessageEvent?.let { event -> removeTemporaryMessage.set(event) }
                } else {
                    originHandler.send(message = message, listener = listener)
                }
            }
        }
    }

    private fun addMessageAndPrepareEvent(): SendOutboundEvent? {
        val storedMessage = chat.storage.welcomeMessage
        if (storedMessage.isBlank()) {
            sendOutboundEvent.set(false)
            return null
        }
        val message = templateToFinalMessage(storedMessage)
        val messageId = UUIDProvider.next()
        val welcomeMessage = MessageText(
            MessageModel(
                idOnExternalPlatform = messageId,
                threadIdOnExternalPlatform = mutableThread.id,
                attachments = emptyList(),
                createdAt = Date(),
                direction = ToClient,
                messageContent = Text(Payload(message)),
                userStatistics = UserStatistics(null, null),
            )
        )
        mutableThread += mutableThread.asCopyable().copy(
            messages = mutableThread.messages.toMutableList().apply { add(0, welcomeMessage) }
        )
        val token = chat.storage.authToken
        return SendOutboundEvent(message, token, messageId)
    }

    private fun templateToFinalMessage(storedMessage: String): String {
        val connection = chat.connection
        val parameters = mapOf(
            "firstName" to connection.firstName,
            "lastName" to connection.lastName,
        )
        val customerFieldMap = chat.fields.toMap()
        val contactFieldMap = origin.get().fields.toMap()
        return VariableMessageParser.parse(
            storedMessage,
            parameters,
            customerFieldMap,
            contactFieldMap
        )
    }

    private companion object {
        @JvmStatic
        private fun customFieldAsPair(customField: CustomField): Pair<String, String> = customField.id to customField.value

        @JvmStatic
        private fun List<CustomField>.toMap() = associate(::customFieldAsPair)
    }
}
