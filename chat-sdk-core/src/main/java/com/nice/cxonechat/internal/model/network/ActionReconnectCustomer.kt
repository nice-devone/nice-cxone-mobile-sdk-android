package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.enums.EventAction
import com.nice.cxonechat.enums.EventType.ReconnectCustomer
import com.nice.cxonechat.internal.model.network.ActionRefreshToken.Data
import com.nice.cxonechat.state.Connection
import java.util.UUID

internal data class ActionReconnectCustomer(
    @SerializedName("action")
    val action: EventAction = EventAction.ChatWindowEvent,
    @SerializedName("eventId")
    val eventId: UUID = UUID.randomUUID(),
    @SerializedName("payload")
    val payload: LegacyPayload<Data>,
) {

    constructor(
        connection: Connection,
        visitor: UUID,
        token: String,
    ) : this(
        payload = LegacyPayload(
            eventType = ReconnectCustomer,
            connection = connection,
            data = Data(token),
            visitor = visitor
        )
    )
}
