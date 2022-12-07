@file:Suppress("LongParameterList")

package com.nice.cxonechat.model

import com.nice.cxonechat.internal.model.AgentModel
import com.nice.cxonechat.internal.model.AttachmentModel
import com.nice.cxonechat.internal.model.CustomerIdentityModel
import com.nice.cxonechat.internal.model.MessageDirectionModel
import com.nice.cxonechat.internal.model.MessageDirectionModel.FromApp
import com.nice.cxonechat.internal.model.MessageModel
import com.nice.cxonechat.internal.model.network.MessagePolyContent
import com.nice.cxonechat.internal.model.network.UserStatistics
import java.util.Date
import java.util.UUID

internal fun makeMessageModel(
    idOnExternalPlatform: UUID = UUID.randomUUID(),
    threadIdOnExternalPlatform: UUID = UUID.randomUUID(),
    messageContent: MessagePolyContent = makeMessageContent(),
    createdAt: Date = Date(),
    attachments: List<AttachmentModel> = emptyList(),
    direction: MessageDirectionModel = FromApp,
    userStatistics: UserStatistics = makeUserStatistics(),
    authorUser: AgentModel? = makeAgent(),
    authorEndUserIdentity: CustomerIdentityModel? = makeCustomerIdentity(),
) = MessageModel(
    idOnExternalPlatform = idOnExternalPlatform,
    threadIdOnExternalPlatform = threadIdOnExternalPlatform,
    messageContent = messageContent,
    createdAt = createdAt,
    attachments = attachments,
    direction = direction,
    userStatistics = userStatistics,
    authorUser = authorUser,
    authorEndUserIdentity = authorEndUserIdentity
)
