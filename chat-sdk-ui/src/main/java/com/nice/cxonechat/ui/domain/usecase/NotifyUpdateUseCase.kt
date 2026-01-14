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

package com.nice.cxonechat.ui.domain.usecase

import android.app.PendingIntent
import android.content.Context
import android.net.Uri
import androidx.core.app.NotificationCompat.MessagingStyle
import androidx.core.app.Person
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.debug
import com.nice.cxonechat.log.timedScope
import com.nice.cxonechat.log.verbose
import com.nice.cxonechat.message.Message
import com.nice.cxonechat.message.MessageDirection
import com.nice.cxonechat.ui.R
import com.nice.cxonechat.ui.R.plurals
import com.nice.cxonechat.ui.R.string
import com.nice.cxonechat.ui.UiModule.Companion.LOGGER_NAME
import com.nice.cxonechat.ui.data.ChatNotificationManager
import com.nice.cxonechat.ui.domain.model.Thread
import com.nice.cxonechat.ui.screen.ChatActivity.Companion.buildOpenThreadIntent
import com.nice.cxonechat.ui.services.LocalNotificationService.Companion.buildDismissIntent
import com.nice.cxonechat.ui.util.addThreadDeeplink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Factory
import org.koin.core.annotation.Named

/**
 * Use case for notifying updates to a chat thread.
 *
 * @param context The application context.
 * @param notificationManager The manager responsible for handling chat notifications.
 * @param logger The logger instance for logging events.
 */
@Factory
internal class NotifyUpdateUseCase(
    private val context: Context,
    private val notificationManager: ChatNotificationManager,
    @Named(LOGGER_NAME) logger: Logger,
) : LoggerScope by LoggerScope(TAG, logger) {

    /**
     * Notifies the user about updates to a specific chat thread.
     *
     * @param thread The chat thread to notify updates for.
     */
    suspend fun notifyUpdate(thread: Thread) = timedScope("notifyUpdate") {
        if (!thread.chatThread.canAddMoreMessages) {
            verbose("Thread is archived, ignoring update")
            return
        }
        withContext(Dispatchers.Default) {
            debug("Notifying update")
            notificationManager.sendMessageNotification(
                notificationId = thread.id.toString(),
                lastMessageId = thread.lastMessageId
            ) {
                val person = Person.Builder()
                    .setName(getThreadNameOrFallback(thread))
                    .build()
                val messagingStyle = MessagingStyle(person)
                val unreadMessages = thread.getUnreadMessages()
                if (unreadMessages.isEmpty()) {
                    verbose("No unread messages")
                } else {
                    verbose("Unread messages: ${unreadMessages.size}")
                }
                for (message in unreadMessages) {
                    verbose(message.metadata.toString())
                    messagingStyle.addMessage(message)
                }
                setSmallIcon(R.drawable.ic_chat_push_service)
                setStyle(messagingStyle)
                setContentIntent(createContentIntent(thread, context))
                setDeleteIntent(createDeleteIntent(thread, context))
            }
        }
    }

    private fun MessagingStyle.addMessage(message: Message) {
        addMessage(
            getNotificationText(message),
            message.createdAt.time,
            null as Person?
        )
    }

    private fun getThreadNameOrFallback(thread: Thread): String =
        thread.chatThread.threadName?.takeIf { it.isNotEmpty() } ?: context.getString(string.notification_person_name)

    private fun getNotificationText(message: Message) = when (message) {
        is Message.ListPicker,
        is Message.QuickReplies,
        is Message.RichLink,
            -> message.fallbackText.orEmpty()

        is Message.Text -> message.text.let { text ->
            val attachments = message.attachments.toList().size
            val hasAttachments = attachments > 0
            buildString {
                append(text)
                if (hasAttachments) {
                    if (text.isNotEmpty()) append(",\n")
                    append(
                        context.resources.getQuantityText(plurals.notification_attachment, attachments)
                    )
                }
            }
        }

        is Message.Unsupported -> message.text
    }

    private companion object {
        private const val TAG = "NotifyUpdateUseCase"
        private const val PENDING_INTENT_FLAGS = PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE

        private fun Thread.getUnreadMessages(): List<Message> = messages
            .filter { message ->
                message.direction == MessageDirection.ToClient
            }
            .takeWhile { message ->
                message.metadata.seenByCustomerAt == null
            }
            .take(MessagingStyle.MAXIMUM_RETAINED_MESSAGES)
            .sortedBy(Message::createdAt)
            .toList()

        private fun createDeleteIntent(thread: Thread, context: Context): PendingIntent? = PendingIntent.getService(
            context,
            0,
            buildDismissIntent(context = context, dismissId = "${thread.id}:${thread.lastMessageId}"),
            PENDING_INTENT_FLAGS
        )

        private fun createContentIntent(thread: Thread, context: Context): PendingIntent? = PendingIntent.getActivity(
            context,
            0,
            buildOpenThreadIntent(
                context,
                createChatUri(context).addThreadDeeplink(thread.chatThread.id)
            ),
            PENDING_INTENT_FLAGS
        )

        private fun createChatUri(context: Context) = Uri.Builder()
            .scheme(context.getString(string.local_notification_data_scheme))
            .build()
    }
}
