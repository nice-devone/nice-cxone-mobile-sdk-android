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

import com.nice.cxonechat.internal.model.MessageModel
import com.nice.cxonechat.internal.model.MessageText
import com.nice.cxonechat.internal.model.network.MessagePolyContent
import com.nice.cxonechat.internal.model.network.UserStatistics
import com.nice.cxonechat.tool.nextString
import java.util.Date

internal fun makeMessage(
    model: MessageModel = makeMessageModel(),
) = MessageText(model)

internal fun makeMessageContent(
    text: String = nextString(),
) = MessagePolyContent.Text(
    payload = MessagePolyContent.Text.Payload(text)
)

internal fun makeUserStatistics(
    seenAt: Date? = null,
    readAt: Date? = null,
) = UserStatistics(
    seenAt = seenAt,
    readAt = readAt
)
