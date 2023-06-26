package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.enums.EventAction
import com.nice.cxonechat.enums.EventAction.ChatWindowEvent
import com.nice.cxonechat.enums.EventType.StoreVisitor
import com.nice.cxonechat.internal.model.Visitor
import com.nice.cxonechat.state.Connection
import java.util.UUID

internal data class ActionStoreVisitor(
    @SerializedName("action")
    val action: EventAction = ChatWindowEvent,
    @SerializedName("eventId")
    val eventId: UUID = UUID.randomUUID(),
    @SerializedName("payload")
    val payload: LegacyPayload<Visitor>,
) {

    constructor(
        connection: Connection,
        visitor: UUID,
        deviceToken: String?,
    ) : this(
        payload = LegacyPayload(
            eventType = StoreVisitor,
            connection = connection,
            data = deviceToken.orEmpty().let(::Visitor),
            visitor = visitor
        )
    )
}
