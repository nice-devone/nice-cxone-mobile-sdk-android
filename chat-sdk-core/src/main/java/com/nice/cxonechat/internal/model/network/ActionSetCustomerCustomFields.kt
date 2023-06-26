package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.enums.EventAction
import com.nice.cxonechat.enums.EventAction.ChatWindowEvent
import com.nice.cxonechat.enums.EventType.SetCustomerCustomFields
import com.nice.cxonechat.internal.model.CustomFieldModel
import com.nice.cxonechat.state.Connection
import java.util.UUID

internal data class ActionSetCustomerCustomFields(
    @SerializedName("action")
    val action: EventAction = ChatWindowEvent,
    @SerializedName("eventId")
    val eventId: UUID = UUID.randomUUID(),
    @SerializedName("payload")
    val payload: Payload<Data>,
) {

    constructor(
        connection: Connection,
        fields: List<CustomFieldModel>,
    ) : this(
        payload = Payload(
            eventType = SetCustomerCustomFields,
            connection = connection,
            data = Data(fields)
        )
    )

    data class Data(
        @SerializedName("customFields")
        val customFields: List<CustomFieldModel>,
    )
}
