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

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.enums.EventAction
import com.nice.cxonechat.enums.EventType.ExecuteTrigger
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.util.UUIDProvider
import java.util.UUID

internal data class ActionExecuteTrigger(
    @SerializedName("action")
    val action: EventAction = EventAction.ChatWindowEvent,
    @SerializedName("eventId")
    val eventId: UUID = UUIDProvider.next(),
    @SerializedName("payload")
    val payload: LegacyPayload<Data>,
) {

    constructor(
        connection: Connection,
        destination: UUID,
        visitor: UUID,
        id: UUID,
    ) : this(
        payload = LegacyPayload(
            eventType = ExecuteTrigger,
            connection = connection,
            data = Data(trigger = Identifier(id = id)),
            destination = destination,
            visitor = visitor
        )
    )

    data class Data(
        @SerializedName("trigger")
        val trigger: Identifier,
    )
}
