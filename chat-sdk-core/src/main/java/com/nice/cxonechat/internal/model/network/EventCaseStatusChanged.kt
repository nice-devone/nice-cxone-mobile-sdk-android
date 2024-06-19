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
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.util.DateTime
import java.util.UUID

internal data class EventCaseStatusChanged(
    @SerializedName("eventId")
    val eventId: UUID = UUID.randomUUID(),
    @SerializedName("createdAt")
    val createdAt: DateTime,
    @SerializedName("data")
    val data: Data,
) {

    val status
        get() = data.case.status

    fun inThread(thread: ChatThread) =
        data.case.threadIdOnExternalPlatform == thread.id.toString()

    internal data class Data(
        @SerializedName("case")
        val case: Case,
    )

    internal data class Case(
        @SerializedName("threadIdOnExternalPlatform")
        val threadIdOnExternalPlatform: String,
        @SerializedName("status")
        val status: CaseStatus,
        @SerializedName("statusUpdatedAt")
        val statusUpdatedAt: DateTime,
    )

    internal enum class CaseStatus {
        @SerializedName("new")
        New,

        @SerializedName("open")
        Open,

        @SerializedName("pending")
        Pending,

        @SerializedName("escalated")
        Escalated,

        @SerializedName("resolved")
        Resolved,

        /**
         * This state is terminal.
         */
        @SerializedName("closed")
        Closed,

        @SerializedName("trashed")
        Trashed
    }
}
