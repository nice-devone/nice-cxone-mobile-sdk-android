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

package com.nice.cxonechat.internal.model

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.internal.model.MessageDirectionModel.ToAgent
import com.nice.cxonechat.internal.model.MessageDirectionModel.ToClient
import com.nice.cxonechat.internal.model.network.MessagePolyContent
import com.nice.cxonechat.internal.model.network.MessagePolyContent.ListPicker
import com.nice.cxonechat.internal.model.network.MessagePolyContent.Noop
import com.nice.cxonechat.internal.model.network.MessagePolyContent.QuickReplies
import com.nice.cxonechat.internal.model.network.MessagePolyContent.RichLink
import com.nice.cxonechat.internal.model.network.MessagePolyContent.Text
import com.nice.cxonechat.internal.model.network.UserStatistics
import com.nice.cxonechat.message.MessageAuthor
import java.util.Date
import java.util.UUID

internal data class MessageModel(
    @SerializedName("idOnExternalPlatform")
    val idOnExternalPlatform: UUID,

    @SerializedName("threadIdOnExternalPlatform")
    val threadIdOnExternalPlatform: UUID,

    @SerializedName("messageContent")
    val messageContent: MessagePolyContent,

    @SerializedName("createdAt")
    val createdAt: Date,

    @SerializedName("attachments")
    val attachments: List<AttachmentModel>,

    @SerializedName("direction")
    val direction: MessageDirectionModel,

    @SerializedName("userStatistics")
    val userStatistics: UserStatistics,

    @SerializedName("authorUser")
    val authorUser: AgentModel? = null,

    @SerializedName("authorEndUserIdentity")
    val authorEndUserIdentity: CustomerIdentityModel? = null,
) {
    val author: MessageAuthor?
        get() = when (direction) {
            ToAgent -> authorEndUserIdentity?.toMessageAuthor()
            ToClient -> authorUser?.toMessageAuthor()
        }

    fun toMessage() = when (messageContent) {
        is Text -> MessageText(this)
        is QuickReplies -> MessageQuickReplies(this)
        is ListPicker -> MessageListPicker(this)
        is RichLink -> MessageRichLink(this)
        Noop -> null
    }
}
