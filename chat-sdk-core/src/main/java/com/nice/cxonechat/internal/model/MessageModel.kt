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
import com.nice.cxonechat.util.IsoDate
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date
import java.util.UUID

@Serializable
internal data class MessageModel(
    @SerialName("idOnExternalPlatform")
    @Contextual
    val idOnExternalPlatform: UUID,

    @SerialName("threadIdOnExternalPlatform")
    @Contextual
    val threadIdOnExternalPlatform: UUID,

    @SerialName("messageContent")
    val messageContent: MessagePolyContent,

    @SerialName("createdAtWithMilliseconds")
    @Contextual
    private val createdAtWithMilliseconds: IsoDate? = null,
    @SerialName("createdAt")
    @Contextual
    private val createdAtWithSeconds: Date,

    @SerialName("attachments")
    val attachments: List<AttachmentModel>,

    @SerialName("direction")
    val direction: MessageDirectionModel,

    @SerialName("userStatistics")
    val userStatistics: UserStatistics,

    @SerialName("authorUser")
    val authorUser: AgentModel? = null,

    @SerialName("authorEndUserIdentity")
    val authorEndUserIdentity: CustomerIdentityModel? = null,
) {
    val createdAt: Date get() = createdAtWithMilliseconds?.date ?: createdAtWithSeconds

    val author: MessageAuthor?
        get() = when (direction) {
            ToAgent -> authorEndUserIdentity?.toMessageAuthor()
            ToClient -> authorUser?.toMessageAuthor()
        }

    internal constructor(
        idOnExternalPlatform: UUID,
        threadIdOnExternalPlatform: UUID,
        messageContent: MessagePolyContent,
        createdAt: Date,
        attachments: List<AttachmentModel>,
        direction: MessageDirectionModel,
        userStatistics: UserStatistics,
        authorUser: AgentModel? = null,
        authorEndUserIdentity: CustomerIdentityModel? = null,
    ) : this(
        idOnExternalPlatform,
        threadIdOnExternalPlatform,
        messageContent,
        IsoDate(createdAt),
        createdAt,
        attachments,
        direction,
        userStatistics,
        authorUser,
        authorEndUserIdentity
    )

    fun toMessage() = when (messageContent) {
        is Text -> MessageText(this)
        is QuickReplies -> MessageQuickReplies(this)
        is ListPicker -> MessageListPicker(this)
        is RichLink -> MessageRichLink(this)
        Noop -> null
    }
}
