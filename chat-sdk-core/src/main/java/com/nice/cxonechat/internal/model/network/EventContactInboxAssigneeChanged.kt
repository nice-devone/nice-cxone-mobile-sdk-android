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

import com.nice.cxonechat.enums.EventType.CaseInboxAssigneeChanged
import com.nice.cxonechat.internal.model.AgentModel
import com.nice.cxonechat.internal.model.Brand
import com.nice.cxonechat.internal.model.ChannelIdentifier
import com.nice.cxonechat.internal.model.Contact
import com.nice.cxonechat.internal.socket.EventCallback.ReceivedEvent
import com.nice.cxonechat.thread.ChatThread
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class EventContactInboxAssigneeChanged(
    @SerialName("data")
    val data: Data,
) {

    val agent get() = data.inboxAssignee?.toAgent()
    val formerAgent get() = data.previousInboxAssignee?.toAgent()
    val case get() = data.case

    fun inThread(thread: ChatThread) = case.threadIdOnExternalPlatform == thread.id

    @Serializable
    data class Data(
        @SerialName("brand")
        val brand: Brand,
        @SerialName("channel")
        val channel: ChannelIdentifier,
        @SerialName("case")
        val case: Contact,
        @SerialName("inboxAssignee")
        val inboxAssignee: AgentModel?,
        @SerialName("previousInboxAssignee")
        val previousInboxAssignee: AgentModel?,
    )

    companion object : ReceivedEvent<EventContactInboxAssigneeChanged> {
        override val type = CaseInboxAssigneeChanged
    }
}
