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

package com.nice.cxonechat.internal.model.network

import com.nice.cxonechat.enums.EventType.ThreadMetadataLoaded
import com.nice.cxonechat.internal.model.AgentModel
import com.nice.cxonechat.internal.model.MessageModel
import com.nice.cxonechat.internal.socket.EventCallback.ReceivedEvent
import com.nice.cxonechat.thread.ChatThread
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class EventThreadMetadataLoaded(
    @SerialName("postback")
    val postback: Postback<Data>,
) {

    val agent get() = postback.data.ownerAssignee?.toAgent()
    val message get() = postback.data.lastMessage.toMessage()

    fun inThread(thread: ChatThread) = message?.threadId == thread.id

    @Serializable
    data class Data(
        @SerialName("ownerAssignee")
        val ownerAssignee: AgentModel? = null,
        @SerialName("lastMessage")
        val lastMessage: MessageModel,
    )

    companion object : ReceivedEvent<EventThreadMetadataLoaded> {
        override val type = ThreadMetadataLoaded
    }
}
