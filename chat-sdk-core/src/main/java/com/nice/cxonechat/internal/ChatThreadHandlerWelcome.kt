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

import com.nice.cxonechat.Cancellable
import com.nice.cxonechat.Cancellable.Companion.asCancellable
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadHandler.OnThreadUpdatedListener
import com.nice.cxonechat.ChatThreadMessageHandler
import com.nice.cxonechat.ChatThreadMessageHandler.OnMessageTransferListener
import com.nice.cxonechat.enums.ActionType
import com.nice.cxonechat.event.thread.SendOutboundEvent
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.model.ChatThreadMutable
import com.nice.cxonechat.internal.model.CustomFieldModel
import com.nice.cxonechat.internal.model.MessageDirectionModel.ToClient
import com.nice.cxonechat.internal.model.MessageModel
import com.nice.cxonechat.internal.model.WelcomeMessage
import com.nice.cxonechat.internal.model.network.CustomerStatistics
import com.nice.cxonechat.internal.model.network.EventProactiveAction
import com.nice.cxonechat.internal.model.network.MessagePolyContent.Text
import com.nice.cxonechat.internal.model.network.MessagePolyContent.Text.Payload
import com.nice.cxonechat.internal.model.network.UserStatistics
import com.nice.cxonechat.internal.socket.EventCallback.Companion.awaitEvent
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.verbose
import com.nice.cxonechat.message.ContentDescriptor
import com.nice.cxonechat.message.OutboundMessage
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState
import com.nice.cxonechat.thread.CustomField
import com.nice.cxonechat.util.UUIDProvider
import java.lang.Thread.sleep
import java.util.Date
import java.util.concurrent.FutureTask
import java.util.concurrent.RunnableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ChatThreadHandler implementation that adds the welcome message, but waits with sending of event until the first
 * message by the user is sent.
 *
 * This implementation handles two cases:
 * 1. On init - fetch the welcome message from the storage and add it's placeholder to the thread if possible
 * 2. On init - if the welcome message is not stored, register a callback to add the placeholder later
 * The callback is canceled when the welcome message is added or if user sends a message before the welcome message is added
 * Both cases are followed by following scenario:
 * On send message - if the welcome message placeholder is present in the thread,
 * send the welcome message event & remove the placeholder, then send the message.
 */
@Suppress(
    "TooManyFunctions" // Complex logic, but it's necessary to handle the welcome message
)
internal class ChatThreadHandlerWelcome(
    private val origin: ChatThreadHandler,
    private val chat: ChatWithParameters,
    private val mutableThread: ChatThreadMutable,
) : ChatThreadHandler by origin, LoggerScope by LoggerScope("ChatThreadHandlerWelcome", chat.entrails.logger) {

    private val sendOutboundEvent = AtomicBoolean(canAppendToThread())
    private val taskUUID by lazy { UUIDProvider.next() }

    private var prepareWelcomeMessageTask: RunnableFuture<Unit> =
        FutureTask(::addWelcomeMessageTask)

    init {
        if (canAppendToThread()) {
            chat.entrails.threading.background(prepareWelcomeMessageTask)
            mutableThread.resultCallbacks.put(taskUUID, prepareWelcomeMessageTask.asCancellable())
        } else {
            prepareWelcomeMessageTask.cancel(true)
        }
    }

    override fun get(): ChatThread {
        // Try fast completion of the task
        if (!prepareWelcomeMessageTask.isCancelled) {
            runCatching {
                prepareWelcomeMessageTask.get(FAST_COMPLETION_WAIT, TimeUnit.MILLISECONDS)
            }
        }
        return origin.get()
    }

    override fun get(listener: OnThreadUpdatedListener): Cancellable = scope("get(listener)") {
        Cancellable(
            prepareNotifyingListener(listener),
            prepareTaskCancellation(),
            deletePlaceholderMessage(),
            origin.get(listener)
        )
    }

    /**
     * Cancel welcome message task if user receives a message before the welcome message is added.
     */
    private fun prepareTaskCancellation(): Cancellable {
        var taskCancellationListener: Cancellable = Cancellable.noop
        if (!prepareWelcomeMessageTask.isDone && mutableThread.messages.size <= 1) {
            scope("welcomeMessageTaskCancellation") {
                taskCancellationListener = origin.get {
                    if (it.messages.size > 1) {
                        if (!prepareWelcomeMessageTask.isDone) {
                            prepareWelcomeMessageTask.cancel(true)
                        }
                        taskCancellationListener.cancel()
                    }
                }
            }
        }
        return taskCancellationListener
    }

    /**
     * Notify listener with welcome message once it is ready.
     */
    private fun prepareNotifyingListener(listener: OnThreadUpdatedListener): Cancellable {
        return if (prepareWelcomeMessageTask.isCancelled || mutableThread.messages.size > 1) {
            Cancellable.noop
        } else {
            scope("notifyListenerWithWelcomeMessage") {
                chat.entrails.threading.background {
                    if (!prepareWelcomeMessageTask.isCancelled) {
                        // Suppress possible background exceptions caused by cancellation of the task
                        runCatching {
                            prepareWelcomeMessageTask.get()
                            listener.onUpdated(origin.get())
                        }
                    }
                    // Cleanup
                    mutableThread.resultCallbacks.remove(taskUUID)
                }
            }
        }
    }

    private fun deletePlaceholderMessage(): Cancellable {
        var deletePlaceholder: Cancellable = Cancellable.noop
        if (!prepareWelcomeMessageTask.isDone || mutableThread.messages.filterIsInstance<WelcomeMessage>().isNotEmpty()) {
            deletePlaceholder = origin.get { threadUpdate ->
                if (threadUpdate.messages.isNotEmpty()) {
                    verbose("Removing temporary message")
                    mutableThread += mutableThread.asCopyable().copy(
                        messages = mutableThread.messages.dropWhile { it is WelcomeMessage }
                    )
                    deletePlaceholder.cancel()
                }
            }
        }
        return deletePlaceholder
    }

    override fun messages(): ChatThreadMessageHandler = scope("messages") {
        var handler = origin.messages()
        if (sendOutboundEvent.get()) {
            handler = WelcomeThreadMessageHandler(handler)
        }
        return handler
    }

    private fun canAppendToThread(): Boolean =
        mutableThread.messages.isEmpty() && mutableThread.threadState !== ChatThreadState.Closed

    private fun getWelcomeMessage(): String? = scope("getWelcomeMessage") {
        val storedMessage = chat.storage.welcomeMessage
        return if (storedMessage.isNotBlank()) {
            templateToFinalMessage(storedMessage)
        } else {
            chat.socketListener.awaitEvent(EventProactiveAction) { model ->
                model.type === ActionType.WelcomeMessage
            }
                // This is blocking, which will block the current thread until the event is received, execute on background thread
                .call()
                ?.let { model ->
                    val template = model.bodyText
                    val customFields = model.customFields.map(CustomFieldModel::toCustomField)
                    chat.fields = (customFields + chat.fields).distinctBy { it.id }
                    return templateToFinalMessage(template)
                }
        }
    }

    private fun addWelcomeMessageTask() = scope("AddWelcomeMessageTask") {
        val welcomeMessage = getWelcomeMessage()
        if (welcomeMessage != null) {
            val welcomeMessage = WelcomeMessage(
                MessageModel(
                    idOnExternalPlatform = taskUUID,
                    threadIdOnExternalPlatform = mutableThread.id,
                    attachments = emptyList(),
                    createdAt = Date(),
                    direction = ToClient,
                    messageContent = Text(Payload(welcomeMessage)),
                    customerStatistics = CustomerStatistics(null),
                    userStatistics = UserStatistics(null, null),
                )
            )
            if (canAppendToThread() && sendOutboundEvent.getAndSet(true)) {
                mutableThread += mutableThread.asCopyable().copy(
                    messages = mutableThread.messages.toMutableList().apply { add(0, welcomeMessage) }
                )
            }
        }
    }

    private fun sendWelcomeMessage() {
        val welcomeMessage = mutableThread.messages
            .asSequence()
            .filterIsInstance<WelcomeMessage>()
            .firstOrNull()
        if (welcomeMessage != null) {
            verbose("Sending welcome message event")
            welcomeMessage.toOutboundEvent(chat.storage.authToken).let {
                events().trigger(it)
            }
        }
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

    private inner class WelcomeThreadMessageHandler(
        private val originHandler: ChatThreadMessageHandler,
    ) : ChatThreadMessageHandler by originHandler, LoggerScope by LoggerScope("WelcomeThreadMessageHandler", this) {
        override fun send(
            attachments: Iterable<ContentDescriptor>,
            message: String,
            postback: String?,
            listener: OnMessageTransferListener?,
        ) {
            send(OutboundMessage(attachments, message, postback), listener)
        }

        override fun send(message: String, postback: String?, listener: OnMessageTransferListener?) {
            send(OutboundMessage(message, postback = postback), listener)
        }

        override fun send(message: OutboundMessage, listener: OnMessageTransferListener?) {
            chat.entrails.threading.background {
                scope("send") {
                    if (sendOutboundEvent.getAndSet(false)) {
                        sendWelcomeMessage()
                        // Small delay helps backend to process the event before the message sent by user
                        sleep(EVENT_PROCESSING_DELAY)
                    }
                    originHandler.send(message, listener)
                    // Cleanup the welcome message task, we can't append the message now
                    if (!prepareWelcomeMessageTask.isDone) {
                        prepareWelcomeMessageTask.cancel(true)
                    }
                }
            }
        }
    }

    internal companion object {
        private const val FAST_COMPLETION_WAIT = 5L
        private const val EVENT_PROCESSING_DELAY = 1500L

        @JvmStatic
        private fun customFieldAsPair(customField: CustomField): Pair<String, String> = customField.id to customField.value

        @JvmStatic
        private fun List<CustomField>.toMap() = associate(::customFieldAsPair)

        private fun WelcomeMessage.toOutboundEvent(authToken: String?): SendOutboundEvent =
            SendOutboundEvent(text, authToken, id)
    }
}
