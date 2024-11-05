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

import com.nice.cxonechat.enums.EventType.CaseStatusChanged
import com.nice.cxonechat.internal.socket.EventCallback.ReceivedEvent
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.util.DateTime
import com.nice.cxonechat.util.UUIDProvider
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
internal data class EventCaseStatusChanged(
    @SerialName("eventId")
    @Contextual
    val eventId: UUID = UUIDProvider.next(),
    @SerialName("createdAt")
    @Contextual
    val createdAt: DateTime,
    @SerialName("data")
    @Contextual
    val data: Data,
) {

    val status
        get() = data.case.status

    fun inThread(thread: ChatThread) =
        data.case.threadIdOnExternalPlatform == thread.id.toString()

    @Serializable
    internal data class Data(
        @SerialName("case")
        val case: Case,
    )

    @Serializable
    internal data class Case(
        @SerialName("threadIdOnExternalPlatform")
        val threadIdOnExternalPlatform: String,
        @SerialName("status")
        val status: CaseStatus,
        @SerialName("statusUpdatedAt")
        @Contextual
        val statusUpdatedAt: DateTime,
    )

    @Serializable
    internal enum class CaseStatus {
        @SerialName("new")
        New,

        @SerialName("open")
        Open,

        @SerialName("pending")
        Pending,

        @SerialName("escalated")
        Escalated,

        @SerialName("resolved")
        Resolved,

        /**
         * This state is terminal.
         */
        @SerialName("closed")
        Closed,

        @SerialName("trashed")
        Trashed
    }

    companion object : ReceivedEvent<EventCaseStatusChanged> {
        override val type = CaseStatusChanged
    }
}
