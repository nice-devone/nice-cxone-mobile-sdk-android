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
