package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.enums.EventAction
import com.nice.cxonechat.enums.EventAction.ChatWindowEvent
import com.nice.cxonechat.enums.EventType.RefreshToken
import com.nice.cxonechat.state.Connection
import java.util.UUID

internal data class ActionRefreshToken(
    @SerializedName("action")
    val action: EventAction = ChatWindowEvent,
    @SerializedName("eventId")
    val eventId: UUID = UUID.randomUUID(),
    @SerializedName("payload")
    val payload: Payload<Data>,
) {

    constructor(
        connection: Connection,
        token: String,
    ) : this(
        payload = Payload(
            eventType = RefreshToken,
            connection = connection,
            data = Data(token)
        )
    )

    data class Data(
        @SerializedName("accessToken")
        val accessToken: AccessTokenPayload,
    ) {

        constructor(token: String) : this(AccessTokenPayload(token))

    }

}
