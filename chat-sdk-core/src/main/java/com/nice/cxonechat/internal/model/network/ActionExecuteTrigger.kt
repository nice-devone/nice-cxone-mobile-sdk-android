package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.enums.EventAction
import com.nice.cxonechat.enums.EventType.ExecuteTrigger
import com.nice.cxonechat.state.Connection
import java.util.UUID

internal data class ActionExecuteTrigger(
    @SerializedName("action")
    val action: EventAction = EventAction.ChatWindowEvent,
    @SerializedName("eventId")
    val eventId: UUID = UUID.randomUUID(),
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
