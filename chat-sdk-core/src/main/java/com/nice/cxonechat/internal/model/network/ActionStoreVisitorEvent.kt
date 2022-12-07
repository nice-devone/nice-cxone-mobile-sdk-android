package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.enums.EventAction
import com.nice.cxonechat.enums.EventAction.ChatWindowEvent
import com.nice.cxonechat.enums.EventType.StoreVisitorEvents
import com.nice.cxonechat.enums.VisitorEventType
import com.nice.cxonechat.state.Connection
import java.util.Date
import java.util.UUID

internal data class ActionStoreVisitorEvent(
    @SerializedName("action")
    val action: EventAction = ChatWindowEvent,
    @SerializedName("eventId")
    val eventId: UUID = UUID.randomUUID(),
    @SerializedName("payload")
    val payload: Payload<Data>,
) {

    constructor(
        connection: Connection,
        visitor: UUID,
        destination: UUID,
        vararg events: VisitorEvent,
    ) : this(
        payload = Payload(
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
        vararg events: Pair<VisitorEventType, Any?>,
        createdAt: Date = Date(),
    ) : this(
        payload = Payload(
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

    data class Data constructor(
        @SerializedName("visitorEvents")
        val visitorEvents: List<VisitorEvent>,
    )

}
