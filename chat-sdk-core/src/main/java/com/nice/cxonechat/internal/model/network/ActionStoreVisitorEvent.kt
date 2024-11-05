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
import com.nice.cxonechat.enums.EventAction.ChatWindowEvent
import com.nice.cxonechat.enums.EventType.StoreVisitorEvents
import com.nice.cxonechat.enums.VisitorEventType
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.util.UUIDProvider
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import java.util.Date
import java.util.UUID

@Serializable
internal data class ActionStoreVisitorEvent(
    @SerialName("action")
    val action: EventAction = ChatWindowEvent,
    @SerialName("eventId")
    @Contextual
    val eventId: UUID = UUIDProvider.next(),
    @SerialName("payload")
    val payload: LegacyPayload<Data>,
) {

    constructor(
        connection: Connection,
        visitor: UUID,
        destination: UUID,
        vararg events: VisitorEvent,
    ) : this(
        payload = LegacyPayload(
            eventType = StoreVisitorEvents,
            connection = connection,
            data = Data(events.toList()),
            visitor = visitor,
            destination = destination
        )
    )

    constructor(
        connection: Connection,
        visitor: UUID,
        destination: UUID,
        vararg events: Pair<VisitorEventType, JsonElement?>,
        createdAt: Date = Date(),
    ) : this(
        payload = LegacyPayload(
            eventType = StoreVisitorEvents,
            connection = connection,
            data = events
                .map { (event, data) ->
                    VisitorEvent(
                        type = event,
                        data = data,
                        createdAt = createdAt
                    )
                }
                .toList()
                .let(ActionStoreVisitorEvent::Data),
            visitor = visitor,
            destination = destination
        )
    )

    @Serializable
    data class Data(
        @SerialName("visitorEvents")
        val visitorEvents: List<VisitorEvent>,
    )
}
