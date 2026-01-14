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

package com.nice.cxonechat.ui.domain.model

import androidx.compose.runtime.Immutable
import com.nice.cxonechat.message.Message.ListPicker
import com.nice.cxonechat.message.Message.QuickReplies
import com.nice.cxonechat.message.Message.RichLink
import com.nice.cxonechat.message.Message.Text
import com.nice.cxonechat.message.Message.Unsupported
import com.nice.cxonechat.thread.ChatThread
import java.util.Date
import java.util.UUID

/**
 * Represents a chat thread with its messages and metadata.
 *
 * @property chatThread The underlying chat thread data.
 * @property name Optional name for the thread, can be null.
 */
@Immutable
internal data class Thread(
    val chatThread: ChatThread,
    val name: String?,
) {

    val id: UUID = chatThread.id

    /**
     * [chatThread] messages sorted in descending order.
     */
    val messages by lazy {
        chatThread.messages
            .asSequence()
            .sortedByDescending { it.createdAt }
    }

    private val lastMessageObject by lazy {
        messages.firstOrNull()
    }

    /**
     * The last message converted to a text (if possible).
     */
    val lastMessage: String by lazy {
        lastMessageObject
            ?.run {
                when (this) {
                    is Text -> text
                    is RichLink -> fallbackText
                    is QuickReplies -> fallbackText
                    is ListPicker -> fallbackText
                    is Unsupported -> text
                }
            }
            .orEmpty()
    }

    /**
     * The last message created date.
     */
    val lastMessageTime: Date? by lazy {
        lastMessageObject?.createdAt
    }

    /**
     * The last message id.
     */
    val lastMessageId: String? by lazy {
        lastMessageObject?.id.toString()
    }

    /**
     * The agent associated with the thread, if any.
     */
    val agent = chatThread.threadAgent?.asPerson
}
