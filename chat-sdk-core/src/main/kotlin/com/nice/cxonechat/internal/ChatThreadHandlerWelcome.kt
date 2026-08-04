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

import androidx.annotation.VisibleForTesting
import com.nice.cxonechat.Cancellable.Companion.asCancellable
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.ChatThreadMessageHandler
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
import com.nice.cxonechat.internal.socket.EventCallback.Companion.awaitEventSuspend
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.verbose
import com.nice.cxonechat.log.warning
import com.nice.cxonechat.message.OutboundMessage
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState
import com.nice.cxonechat.thread.CustomField
import com.nice.cxonechat.util.UUIDProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.merge
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Clock

/**
 * ChatThreadHandler implementation that adds the welcome message, but waits with sending of event until the first
 * message by the user is sent.
 *
 * This implementation handles two cases:
 * 1. On init - fetch the welcome message from the storage and add its placeholder to the thread if possible
 * 2. On init - if the welcome message is not stored locally, the initialisation coroutine suspends waiting
 *    for an EventProactiveAction socket event with type WelcomeMessage, then adds the placeholder once received.
 * The coroutine is canceled when the welcome message is added or if user sends a message before the welcome message is added
 * Both cases are followed by following scenario:
 * On send message - if the welcome message placeholder is present in the thread,
 * send the welcome message event & remove the placeholder, then send the message.
 *
 * `prepareWelcomeMessageJob` completes normally once the placeholder is appended.
 * It is cancelled only if the user sends a message before it finishes, or if a
 * real inbound message arrives first (making a welcome placeholder redundant).
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
    private val welcomeNotification = MutableSharedFlow<ChatThread>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    // Launched immediately on construction if there is room to append a welcome message.
    // The job is registered in resultCallbacks so it can be cancelled externally (e.g. on thread close).
    // If there is no room (thread already has messages or is closed), a pre-cancelled sentinel Job is used
    // so isCancelled/isCompleted checks throughout this class work uniformly without null-safety.
    @VisibleForTesting
    internal val prepareWelcomeMessageJob: Job = if (canAppendToThread()) {
        chat.entrails.threading.coroutineScope.safeLaunch(childScope("prepareWelcomeMessage")) {
            addWelcomeMessageTask()
        }.also { job ->
            mutableThread.resultCallbacks[taskUUID] = job.asCancellable()
        }
    } else {
        Job().apply { cancel() }
    }

    override val threadFlow: Flow<ChatThread> = merge(origin.threadFlow, welcomeNotification)

    init {
        if (canAppendToThread()) {
            chat.entrails.threading.coroutineScope.safeLaunch(childScope("welcomeTaskCancellation")) {
                if (!prepareWelcomeMessageJob.isCompleted) {
                    origin.threadFlow.filter { it.messages.size > 1 }.first()
                    if (!prepareWelcomeMessageJob.isCompleted) prepareWelcomeMessageJob.cancel()
                }
            }
            chat.entrails.threading.coroutineScope.safeLaunch(childScope("welcomeNotify")) {
                try {
                    if (!prepareWelcomeMessageJob.isCancelled) {
                        prepareWelcomeMessageJob.join()
                        // join() does not throw when the joined job is cancelled — only when the calling
                        // coroutine is cancelled — so the explicit re-check below is required.
                        if (!prepareWelcomeMessageJob.isCancelled &&
                            mutableThread.messages.any { it is WelcomeMessage }
                        ) {
                            welcomeNotification.tryEmit(get())
                        }
                    }
                } finally {
                    mutableThread.resultCallbacks.remove(taskUUID)
                }
            }
        }
        if (!prepareWelcomeMessageJob.isCompleted || mutableThread.messages.filterIsInstance<WelcomeMessage>().isNotEmpty()) {
            chat.entrails.threading.coroutineScope.safeLaunch(childScope("welcomeDeletePlaceholder")) {
                // Must use `any { it !is WelcomeMessage }` rather than `isNotEmpty()` — the initial
                // onStart emission already contains only the WelcomeMessage placeholder, so isNotEmpty()
                // would fire immediately and delete the placeholder before it can be displayed.
                origin.threadFlow.filter { thread -> thread.messages.any { it !is WelcomeMessage } }.first()
                verbose("Removing temporary message")
                mutableThread += mutableThread.asCopyable().copy(
                    messages = mutableThread.messages.dropWhile { it is WelcomeMessage }
                )
                welcomeNotification.tryEmit(get())
            }
        }
    }

    override fun get(): ChatThread = origin.get()

    override fun messages(): ChatThreadMessageHandler = scope("messages") {
        var handler = origin.messages()
        if (sendOutboundEvent.get()) {
            handler = WelcomeThreadMessageHandler(handler)
        }
        return handler
    }

    private fun canAppendToThread(): Boolean =
        mutableThread.messages.isEmpty() && mutableThread.threadState !== ChatThreadState.Closed

    private suspend fun getWelcomeMessage(): String? = scope("getWelcomeMessage") {
        val storedMessage = chat.storage.welcomeMessage
        return if (storedMessage.isNotBlank()) {
            templateToFinalMessage(storedMessage)
        } else {
            val model = chat.socketListener.awaitEventSuspend(EventProactiveAction) { model ->
                model.type === ActionType.WelcomeMessage
            }
            val template = model.bodyText
            val customFields = model.customFields.map(CustomFieldModel::toCustomField)
            chat.fields = (customFields + chat.fields).distinctBy { it.id }
            templateToFinalMessage(template)
        }
    }

    private suspend fun addWelcomeMessageTask() = scope("AddWelcomeMessageTask") {
        val welcomeMessage = getWelcomeMessage()
        if (welcomeMessage != null) {
            val welcomeMessage = WelcomeMessage(
                MessageModel(
                    idOnExternalPlatform = taskUUID,
                    threadIdOnExternalPlatform = mutableThread.id,
                    attachments = emptyList(),
                    createdAt = Clock.System.now(),
                    direction = ToClient,
                    messageContent = Text(Payload(welcomeMessage)),
                    customerStatistics = CustomerStatistics(null),
                    userStatistics = UserStatistics(null, null),
                )
            )
            // Read-only check: sendOutboundEvent is only set to false by WelcomeThreadMessageHandler.send()
            // once the user actually sends a message. If it's already false the user got ahead of us
            // and we must not append the placeholder (the welcome event was already sent via send()).
            if (canAppendToThread() && sendOutboundEvent.get()) {
                mutableThread += mutableThread.asCopyable().copy(
                    messages = mutableThread.messages.toMutableList().apply { add(0, welcomeMessage) }
                )
            }
        }
    }

    private suspend fun sendWelcomeMessage() {
        val welcomeMessage = mutableThread.messages
            .asSequence()
            .filterIsInstance<WelcomeMessage>()
            .firstOrNull()
        if (welcomeMessage != null) {
            verbose("Sending welcome message event")
            events().trigger(welcomeMessage.toOutboundEvent(chat.storage.authToken))
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
        override suspend fun send(message: OutboundMessage): String = scope("send") {
            if (sendOutboundEvent.getAndSet(false)) {
                try {
                    sendWelcomeMessage()
                    // Small delay helps backend to process the event before the message sent by user
                    delay(EVENT_PROCESSING_DELAY)
                } catch (expected: CancellationException) {
                    // CancellationException is a subclass of Exception — it must be re-thrown first
                    // to preserve structured concurrency. Without this explicit clause the catch below
                    // would swallow it and permanently hang the coroutine.
                    throw expected
                } catch (expected: Exception) {
                    warning("Welcome message failed before user message", expected)
                }
            }
            val id = originHandler.send(message)
            // Cleanup the welcome message task, we can't append the message now
            if (!prepareWelcomeMessageJob.isCompleted) {
                prepareWelcomeMessageJob.cancel()
            }
            return id
        }
    }

    internal companion object {
        private const val EVENT_PROCESSING_DELAY = 1500L

        @JvmStatic
        private fun customFieldAsPair(customField: CustomField): Pair<String, String> = customField.id to customField.value

        @JvmStatic
        private fun List<CustomField>.toMap() = associate(::customFieldAsPair)

        private fun WelcomeMessage.toOutboundEvent(authToken: String?): SendOutboundEvent =
            SendOutboundEvent(text, authToken, id)
    }
}
