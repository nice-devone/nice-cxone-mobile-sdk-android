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

import com.nice.cxonechat.enums.ContactStatus
import com.nice.cxonechat.enums.EventType.MessageCreated
import com.nice.cxonechat.internal.model.Contact
import com.nice.cxonechat.internal.model.MessageModel
import com.nice.cxonechat.internal.model.Thread
import com.nice.cxonechat.internal.socket.EventCallback.ReceivedEvent
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.thread.ChatThreadState
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Event Received when a message has been successfully sent/created. */
@Serializable
internal data class EventMessageCreated(
    @SerialName("data")
    val data: Data,
) {

    val contactId get() = data.case.id
    val contactStatus get() = data.case.status

    val threadId get() = data.thread.idOnExternalPlatform
    val threadName get() = data.thread.threadName

    val message get() = data.message.toMessage()

    val threadState
        get() = if (contactStatus === ContactStatus.Closed) {
            ChatThreadState.Closed
        } else {
            ChatThreadState.Ready
        }

    fun inThread(thread: ChatThread): Boolean = thread.id == threadId

    @Serializable
    data class Data(
        @SerialName("case")
        val case: Contact,
        @SerialName("thread")
        val thread: Thread,
        @SerialName("message")
        val message: MessageModel,
    )

    companion object : ReceivedEvent<EventMessageCreated> {
        override val type = MessageCreated
    }
}
