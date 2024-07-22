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
import com.nice.cxonechat.internal.model.ChatThreadInternal
import com.nice.cxonechat.internal.model.Thread
import com.nice.cxonechat.thread.ChatThreadState.Received
import java.util.Date
import java.util.UUID

internal data class ReceivedThreadData(
    @SerializedName("id")
    internal val id: String,
    @SerializedName("idOnExternalPlatform")
    val idOnExternalPlatform: UUID,
    @SerializedName("channelId")
    val channelId: String,
    @SerializedName("threadName")
    val threadName: String,
    @SerializedName("createdAt")
    val createdAt: Date,
    @SerializedName("updatedAt")
    val updatedAt: Date,
    @SerializedName("canAddMoreMessages")
    val canAddMoreMessages: Boolean,
    @SerializedName("thread")
    val thread: Thread,
) {

    fun toChatThread() = ChatThreadInternal(
        id = idOnExternalPlatform,
        messages = mutableListOf(),
        canAddMoreMessages = canAddMoreMessages,
        threadName = threadName,
        threadState = Received,
    )
}
