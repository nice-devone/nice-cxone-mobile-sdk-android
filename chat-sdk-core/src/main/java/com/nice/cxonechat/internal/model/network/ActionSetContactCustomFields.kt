package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.enums.EventAction
import com.nice.cxonechat.enums.EventType.SetContactCustomFields
import com.nice.cxonechat.internal.model.CustomFieldModel
import com.nice.cxonechat.internal.model.Thread
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.thread.ChatThread
import java.util.UUID

internal data class ActionSetContactCustomFields(
    @SerializedName("action")
    val action: EventAction = EventAction.ChatWindowEvent,
    @SerializedName("eventId")
    val eventId: UUID = UUID.randomUUID(),
    @SerializedName("payload")
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

    data class Data(
        @SerializedName("thread")
        val thread: Thread,
        @SerializedName("customFields")
        val customFields: List<CustomFieldModel>,
        @SerializedName("contact")
        val contact: Identifier,
    )

}
