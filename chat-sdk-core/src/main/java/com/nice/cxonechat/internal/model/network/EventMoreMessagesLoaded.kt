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

internal data class EventMoreMessagesLoaded(
    @SerializedName("postback")
    val postback: Postback<Data>,
) {

    val scrollToken get() = postback.data.scrollToken
    val messages get() = postback.data.messages.mapNotNull(MessageModel::toMessage)

    fun inThread(thread: ChatThread) = messages.all { it.threadId == thread.id }

    data class Data(
        @SerializedName("messages")
        val messages: List<MessageModel>,
        @SerializedName("scrollToken")
        val scrollToken: String,
    )
}
