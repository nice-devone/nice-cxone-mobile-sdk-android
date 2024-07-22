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

package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.enums.EventType.MessageCreated
import com.nice.cxonechat.internal.model.Contact
import com.nice.cxonechat.internal.model.MessageModel
import com.nice.cxonechat.internal.model.Thread
import com.nice.cxonechat.internal.socket.EventCallback.ReceivedEvent
import com.nice.cxonechat.thread.ChatThread

/** Event Received when a message has been successfully sent/created. */
internal data class EventMessageCreated(
    @SerializedName("data")
    val data: Data,
) {

    val contactId get() = data.case.id
    val contactStatus get() = data.case.status

    val threadId get() = data.thread.idOnExternalPlatform
    val threadName get() = data.thread.threadName

    val message get() = data.message.toMessage()

    fun inThread(thread: ChatThread): Boolean = thread.id == threadId

    data class Data(
        @SerializedName("case")
        val case: Contact,
        @SerializedName("thread")
        val thread: Thread,
        @SerializedName("message")
        val message: MessageModel,
    )

    companion object : ReceivedEvent<EventMessageCreated> {
        override val type = MessageCreated
    }
}
