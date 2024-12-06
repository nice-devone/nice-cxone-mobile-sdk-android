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

import com.nice.cxonechat.internal.model.ChatThreadInternal
import com.nice.cxonechat.internal.model.Thread
import com.nice.cxonechat.thread.ChatThreadState.Received
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date
import java.util.UUID

@Serializable
@SerialName("thread") // Not required, but it hides internal class name
internal data class ReceivedThreadData(
    @SerialName("id")
    internal val id: String,
    @SerialName("idOnExternalPlatform")
    @Contextual
    val idOnExternalPlatform: UUID,
    @SerialName("channelId")
    val channelId: String,
    @SerialName("threadName")
    val threadName: String,
    @SerialName("createdAt")
    @Contextual
    val createdAt: Date?,
    @SerialName("updatedAt")
    @Contextual
    val updatedAt: Date?,
    @SerialName("canAddMoreMessages")
    val canAddMoreMessages: Boolean,
    @SerialName("thread")
    val thread: Thread? = null,
) {

    fun toChatThread() = ChatThreadInternal(
        id = idOnExternalPlatform,
        messages = mutableListOf(),
        canAddMoreMessages = canAddMoreMessages,
        threadName = threadName,
        threadState = Received,
    )
}
