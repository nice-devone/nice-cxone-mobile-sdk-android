/*
 * Copyright (c) 2021-2023. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.internal.model.MessageModel
import com.nice.cxonechat.thread.ChatThread

/**
 * Event received when an agent has read a message.
 */
internal data class EventMessageReadByAgent(
    @SerializedName("data")
    val data: Data,
) {

    val threadId get() = data.message.threadIdOnExternalPlatform

    val messageId get() = data.message.idOnExternalPlatform
    val message get() = data.message.toMessage()

    /**
     * Returns `true` iff [threadId] matches the one of supplied [thread] and the [thread.messages] contain element
     * with matching id.
     */
    fun inThread(thread: ChatThread): Boolean =
        thread.id == threadId &&
                thread.messages.any { threadMessage ->
                    threadMessage.id == messageId
                }

    data class Data(
        @SerializedName("message")
        val message: MessageModel,
    )
}
