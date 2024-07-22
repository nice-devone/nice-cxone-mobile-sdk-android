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

@file:Suppress("LongParameterList")

package com.nice.cxonechat.model

import com.nice.cxonechat.internal.model.AgentModel
import com.nice.cxonechat.internal.model.AttachmentModel
import com.nice.cxonechat.internal.model.CustomerIdentityModel
import com.nice.cxonechat.internal.model.MessageDirectionModel
import com.nice.cxonechat.internal.model.MessageDirectionModel.ToAgent
import com.nice.cxonechat.internal.model.MessageModel
import com.nice.cxonechat.internal.model.network.MessagePolyContent
import com.nice.cxonechat.internal.model.network.UserStatistics
import java.util.Date
import java.util.UUID

private val linearTime = generateSequence(Date()) { Date(it.time + 1) }.iterator()

internal fun makeMessageModel(
    idOnExternalPlatform: UUID = UUID.randomUUID(),
    threadIdOnExternalPlatform: UUID = UUID.randomUUID(),
    messageContent: MessagePolyContent = makeMessageContent(),
    createdAt: Date = linearTime.next(),
    attachments: List<AttachmentModel> = emptyList(),
    direction: MessageDirectionModel = ToAgent,
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
