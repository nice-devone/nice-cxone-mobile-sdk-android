package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.enums.EventAction
import com.nice.cxonechat.enums.EventAction.ChatWindowEvent
import com.nice.cxonechat.enums.EventType.FetchThreadList
import com.nice.cxonechat.state.Connection
import java.util.UUID

internal data class ActionFetchThread(
    @SerializedName("action")
    val action: EventAction = ChatWindowEvent,
    @SerializedName("eventId")
    val eventId: UUID = UUID.randomUUID(),
    @SerializedName("payload")
    val payload: Payload<Unit>,
) {

    constructor(
        connection: Connection,
    ) : this(
        payload = Payload(
            eventType = FetchThreadList,
            connection = connection,
            data = Unit
        )
    )

}
