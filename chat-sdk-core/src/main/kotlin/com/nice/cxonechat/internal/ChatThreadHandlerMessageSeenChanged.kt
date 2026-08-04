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
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.asCopyable
import com.nice.cxonechat.internal.copy.ChatThreadCopyable.Companion.updateWith
import com.nice.cxonechat.internal.model.ChatThreadMutable
import com.nice.cxonechat.internal.model.network.EventMessageSeen
import com.nice.cxonechat.internal.socket.EventCallback.Companion.eventFlow
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.warning
import com.nice.cxonechat.thread.ChatThread
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge

/**
 * Update thread message with the message seen metadata.
 */
internal class ChatThreadHandlerMessageSeenChanged(
    private val origin: ChatThreadHandler,
    private val chat: ChatWithParameters,
    private val thread: ChatThreadMutable,
) : ChatThreadHandler by origin, LoggerScope by LoggerScope("ChatThreadHandlerMessageSeenChanged", chat.entrails.logger) {

    override val threadFlow: Flow<ChatThread> = merge(
        origin.threadFlow,
        chat.socketListener.eventFlow(EventMessageSeen)
            .mapNotNull { (_, event) ->
                val message = event.message ?: return@mapNotNull null
                if (message.threadId != thread.id) return@mapNotNull null
                try {
                    thread += thread.asCopyable().copy(messages = thread.messages.updateWith(listOf(message)))
                    get()
                } catch (expected: CancellationException) {
                    throw expected
                } catch (expected: Exception) {
                    warning("Failed to handle message-seen event in threadFlow", expected)
                    null
                }
            },
    )
}
