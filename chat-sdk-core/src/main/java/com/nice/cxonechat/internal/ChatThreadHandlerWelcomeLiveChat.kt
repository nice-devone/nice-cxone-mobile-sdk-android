/*
 * Copyright (c) 2021-2025. NICE Ltd. All rights reserved.
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
import com.nice.cxonechat.ChatThreadEventHandler
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadMessageHandler
import com.nice.cxonechat.ChatThreadMessageHandler.OnMessageTransferListener
import com.nice.cxonechat.enums.ActionType
import com.nice.cxonechat.event.thread.SendOutboundEvent
import com.nice.cxonechat.internal.ChatThreadHandlerLiveChat.Companion.BEGIN_CONVERSATION_MESSAGE
import com.nice.cxonechat.internal.model.ChatThreadMutable
import com.nice.cxonechat.internal.model.WelcomeMessage
import com.nice.cxonechat.internal.model.network.EventLiveChatThreadRecovered
import com.nice.cxonechat.internal.model.network.EventProactiveAction
import com.nice.cxonechat.internal.socket.EventCallback.Companion.addCallback
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.verbose
import com.nice.cxonechat.log.warning
import com.nice.cxonechat.message.ContentDescriptor
import com.nice.cxonechat.message.Message
import com.nice.cxonechat.message.Message.Text
import com.nice.cxonechat.message.OutboundMessage
import com.nice.cxonechat.thread.CustomField
import com.nice.cxonechat.util.UUIDProvider
import java.lang.Thread.sleep
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Alternative simplified version of welcome message handling.
 * Since the live chat always starts with a [ChatThreadHandlerLiveChat.BEGIN_CONVERSATION_MESSAGE] message, this handler
 * can skip adding of a temporary [WelcomeMessage] to the thread and just directly send the welcome message as an event.
 *
 * This implementation handles several cases:
 * 1. The thread is started when the Welcome Message is already stored - the event is sent together with the first message.
 * 2. The thread is started when the Welcome Message is not stored - the event is sent when welcome message event is received, if the
 * conversation start message is present.
 * 3. The thread is started when the Welcome Message is not initially stored, but it is recovered later, but not yet appended,
 * when user sends a message. Welcome message will be appended in this case before user message is sent if:
 * - The thread is recovered and contains only conversation start message and event wasn't already sent.
 * - The thread was updated and contains only conversation start message and event wasn't already sent.
 */
@Suppress(
    "TooManyFunctions" // Complex functionality requires splitting into multiple functions
)
internal class ChatThreadHandlerWelcomeLiveChat(
    private val origin: ChatThreadHandler,
    private val chat: ChatWithParameters,
    private val mutableThread: ChatThreadMutable,
) : ChatThreadHandler by origin, LoggerScope by LoggerScope("ChatThreadHandlerWelcomeLiveChat", chat.entrails.logger) {

    @get:Synchronized
    @set:Synchronized
    private var lateAppend: Cancellable = Cancellable.noop

    /**
     * Flag to prevent sending welcome message event multiple times for a given thread.
     */
    val eventSendEnabled = eventSendEnabledMap.getOrPut(mutableThread.id) { AtomicBoolean(mutableThread.messages.size < 2) }

    override fun get(listener: ChatThreadHandler.OnThreadUpdatedListener): Cancellable = Cancellable(
        lateWelcomeMessageAppend(),
        appendMissingWelcomeMessage(),
        if (canSendWelcomeEvent(mutableThread.messages.size)) {
            origin.get {
                scope("ThreadUpdate") {
                    verbose("On thread update")
                    if (canSendWelcomeEvent(it.messages.size)) {
                        val storedMessage = storedMessage()
                        if (storedMessage.isNotBlank() && it.messages.hasConversationStart() && eventSendEnabled.getAndSet(false)) {
                            verbose("Adding welcome message on thread update")
                            triggerWelcomeMessageEvent(storedMessage)
                            lateAppend.cancel()
                        }
                    }
                    listener.onUpdated(it)
                }
            }
        } else {
            origin.get(listener)
        }
    )

    /** Handle thread in odd state where it only contains conversation start. */
    private fun appendMissingWelcomeMessage(): Cancellable = scope("appendMissingWelcomeMessage") {
        val storedMessage = storedMessage()
        val skipAddingWelcomeMessage = !canSendWelcomeEvent(mutableThread.messages.size)
        return if (skipAddingWelcomeMessage || lateAppend != Cancellable.noop || storedMessage.isBlank()) {
            // We can't add welcome message using this route
            Cancellable.noop
        } else if (mutableThread.messages.hasConversationStart() && eventSendEnabled.getAndSet(false)) {
            // Thread has only one message and it's a conversation start, send welcome message event
            verbose("Thread is missing welcome message, sending it as event")
            triggerWelcomeMessageEvent(storedMessage)
            Cancellable.noop
        } else {
            // Thread is empty, wait for thread recovery to finish in case it will contain only conversation start message
            prepareThreadRecoverAppend(storedMessage)
        }
    }

    private fun prepareThreadRecoverAppend(storedMessage: String): Cancellable =
        chat.socketListener.addCallback(EventLiveChatThreadRecovered) { event ->
            scope("EventLiveChatThreadRecovered") {
                if (
                    event.messages.size == 1 &&
                    event.messages.hasConversationStart()
                ) {
                    if (eventSendEnabled.getAndSet(false)) {
                        verbose("Thread recovered with only conversation start message, sending welcome message event")
                        triggerWelcomeMessageEvent(storedMessage) {
                            lateAppend.cancel()
                        }
                    } else {
                        lateAppend.cancel()
                    }
                } else {
                    if (event.messages.size > 1) {
                        lateAppend.cancel()
                    }
                }
            }
        }.also {
            lateAppend = it
        }

    private fun lateWelcomeMessageAppend(): Cancellable = scope("lateWelcomeMessageAppend") {
        verbose("Checking if late welcome message append is possible")
        return if (mutableThread.messages.size < 2 && storedMessage().isBlank() && lateAppend == Cancellable.noop) {
            verbose("Registering late welcome message append task")
            registerWelcomeMessageCallback {
                lateAppend.cancel()
            }.also {
                lateAppend = it
            }
        } else {
            Cancellable.noop
        }
    }

    private fun registerWelcomeMessageCallback(onCallback: () -> Unit): Cancellable = scope("WelcomeMessageCallback") {
        verbose("Registering EventProactiveAction/WelcomeMessage callback")
        chat.socketListener.addCallback(EventProactiveAction) { model ->
            if (model.type == ActionType.WelcomeMessage) {
                if (mutableThread.messages.size < 2 && eventSendEnabled.getAndSet(false)) {
                    if (!mutableThread.messages.hasConversationStart()) warning("Thread is missing conversation start message")
                    triggerWelcomeMessageEvent(
                        message = model.bodyText,
                        listener = onCallback,
                    )
                } else {
                    onCallback()
                }
            }
        }
    }

    override fun messages(): ChatThreadMessageHandler = scope("messages") {
        var handler = origin.messages()
        if (canSendWelcomeEvent(mutableThread.messages.size)) {
            handler = WelcomeThreadMessageHandlerLiveChat(handler)
        }
        return handler
    }

    private inner class WelcomeThreadMessageHandlerLiveChat(
        private val originHandler: ChatThreadMessageHandler,
    ) : ChatThreadMessageHandler by originHandler, LoggerScope by LoggerScope("WelcomeThreadMessageHandlerLiveChat", this) {
        override fun send(
            attachments: Iterable<ContentDescriptor>,
            message: String,
            postback: String?,
            listener: OnMessageTransferListener?,
        ) {
            send(
                message = OutboundMessage(attachments = attachments, message = message, postback = postback),
                listener = listener
            )
        }

        override fun send(message: String, postback: String?, listener: OnMessageTransferListener?) {
            send(message = OutboundMessage(message = message, postback = postback), listener = listener)
        }

        override fun send(message: OutboundMessage, listener: OnMessageTransferListener?): Unit = scope("send") {
            chat.entrails.threading.background {
                val messageCount = mutableThread.messages.size
                val canSendWelcome = canSendWelcomeEvent(messageCount)
                val storedMessage = storedMessage()
                if (canSendWelcome && storedMessage.isNotBlank()) {
                    verbose("Attempting to send welcome message")
                    addWelcomeMessageAndSend(message = message, listener = listener, storedMessage = storedMessage)
                } else {
                    verbose("Sending message without welcome message")
                    originHandler.send(message = message, listener = listener)
                }
            }
        }

        /**
         * Trigger welcome message and sends outbound message based on current state.
         * If the outbound message is LiveChat conversation start, the welcome message event is triggered after the message is sent,
         * otherwise the welcome message event is triggered before the message is sent.
         * In both cases the method also checks if the event wasn't already triggered and if so, it won't trigger it again.
         * In all cases the message is always sent.
         */
        private fun addWelcomeMessageAndSend(message: OutboundMessage, listener: OnMessageTransferListener?, storedMessage: String) =
            scope("addWelcomeMessageAndSend") {
                if (message.message == BEGIN_CONVERSATION_MESSAGE) {
                    // Send begin conversation first
                    verbose("Sending conversation start message and then welcome message event")
                    originHandler.send(
                        message = message,
                        listener = OnMessageTransferListener(
                            onProcessed = { listener?.onProcessed(it) },
                        ) {
                            listener?.onSent(it)
                            if (eventSendEnabled.getAndSet(false)) {
                                sleep(EVENT_PROCESSING_DELAY)
                                triggerWelcomeMessageEvent(storedMessage)
                            }
                        }
                    )
                } else if (eventSendEnabled.getAndSet(false)) {
                    // User is sending message, send welcome message as event first
                    verbose("Sending welcome message event and then user message")
                    triggerWelcomeMessageEvent(
                        message = storedMessage,
                        listener = {
                            // Artificial delay to ensure that the welcome message is processed first
                            sleep(EVENT_PROCESSING_DELAY)
                            verbose("Sending user message")
                            originHandler.send(message, listener)
                        },
                    )
                } else {
                    verbose("Sending just the user message")
                    // Welcome message outbound event was already sent, thread wasn't updated, resume with regular send
                    originHandler.send(message, listener)
                }
            }
    }

    private fun triggerWelcomeMessageEvent(
        message: String,
        listener: ChatThreadEventHandler.OnEventSentListener? = null,
    ) = scope("triggerWelcomeMessageEvent") {
        verbose("Sending welcome message event")
        events().trigger(prepareEvent(message), listener)
    }

    private fun canSendWelcomeEvent(messageCount: Int): Boolean = when (messageCount) {
        0 -> eventSendEnabled.get() // This should be always true
        1 -> eventSendEnabled.get() && mutableThread.messages.hasConversationStart()
        else -> false
    }

    private fun storedMessage(): String = chat.storage.welcomeMessage

    private fun prepareEvent(storedMessage: String): SendOutboundEvent {
        val message = templateToFinalMessage(storedMessage)
        val messageId = UUIDProvider.next()
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
        /** A temporary workaround until backend event received confirmation is implemented in the SDK. */
        private const val EVENT_PROCESSING_DELAY = 1500L

        private val eventSendEnabledMap = mutableMapOf<UUID, AtomicBoolean>()

        @JvmStatic
        private fun customFieldAsPair(customField: CustomField): Pair<String, String> = customField.id to customField.value

        @JvmStatic
        private fun List<CustomField>.toMap() = associate(::customFieldAsPair)

        @JvmStatic
        private fun List<Message>.hasConversationStart() =
            BEGIN_CONVERSATION_MESSAGE == (firstOrNull() as? Text)?.text
    }
}
