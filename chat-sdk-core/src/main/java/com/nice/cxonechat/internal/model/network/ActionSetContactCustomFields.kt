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

import com.nice.cxonechat.enums.EventAction
import com.nice.cxonechat.enums.EventType.SetContactCustomFields
import com.nice.cxonechat.internal.model.CustomFieldModel
import com.nice.cxonechat.internal.model.Thread
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.util.UUIDProvider
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
internal data class ActionSetContactCustomFields(
    @SerialName("action")
    val action: EventAction = EventAction.ChatWindowEvent,
    @SerialName("eventId")
    @Contextual
    val eventId: UUID = UUIDProvider.next(),
    @SerialName("payload")
    val payload: Payload<Data>,
) {

    constructor(
        connection: Connection,
        thread: ChatThread,
        fields: List<CustomFieldModel>,
    ) : this(
        payload = Payload(
            eventType = SetContactCustomFields,
            connection = connection,
            data = Data(
                thread = Thread(thread),
                customFields = fields,
                contact = thread.threadAgent?.id.toString().let(::Identifier)
            )
        )
    )

    @Serializable
    data class Data(
        @SerialName("thread")
        val thread: Thread,
        @SerialName("customFields")
        val customFields: List<CustomFieldModel>,
        @SerialName("contact")
        val contact: Identifier,
    )
}
