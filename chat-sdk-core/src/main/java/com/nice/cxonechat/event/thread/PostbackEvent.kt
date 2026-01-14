/*
 * Copyright (c) 2021-2026. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.event.thread

import com.nice.cxonechat.enums.EventType
import com.nice.cxonechat.enums.MessageContentType
import com.nice.cxonechat.internal.model.CustomFieldModel
import com.nice.cxonechat.internal.model.Thread
import com.nice.cxonechat.internal.model.network.ActionMessage
import com.nice.cxonechat.internal.model.network.CustomFieldsData
import com.nice.cxonechat.internal.model.network.MessageContent
import com.nice.cxonechat.internal.model.network.MessagePayload
import com.nice.cxonechat.internal.model.network.Parameters
import com.nice.cxonechat.internal.model.network.Payload
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.thread.ChatThread
import java.util.UUID

internal data class PostbackEvent(
    val postback: String,
    val text: String? = null,
    private val parameters: Parameters? = null,
) : ChatThreadEvent() {
    val messageId: UUID = UUID.randomUUID()

    override fun getModel(thread: ChatThread, connection: Connection) =
        ActionMessage(
            payload = Payload(
                eventType = EventType.SendMessage,
                connection = connection,
                data = ActionMessage.Data(
                    thread = Thread(thread),
                    messageContent = MessageContent(
                        type = MessageContentType.Postback,
                        payload = MessagePayload(text.orEmpty(), postback),
                        postback = postback,
                        parameters = parameters
                    ),
                    id = messageId,
                    customerContact = thread.fields.takeUnless { it.isEmpty() }
                        ?.map(::CustomFieldModel)
                        ?.let(::CustomFieldsData),
                )
            )
        )
}
