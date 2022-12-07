package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.enums.EventAction
import com.nice.cxonechat.enums.EventAction.ChatWindowEvent
import com.nice.cxonechat.enums.EventType.UpdateThread
import com.nice.cxonechat.internal.model.Thread
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.thread.ChatThread
import java.util.UUID

internal data class ActionUpdateThread(
    @SerializedName("action")
    val action: EventAction = ChatWindowEvent,
    @SerializedName("eventId")
    val eventId: UUID = UUID.randomUUID(),
    @SerializedName("payload")
    val payload: Payload<ThreadEventData>,
) {

    constructor(
        connection: Connection,
        thread: ChatThread,
    ) : this(
        payload = Payload(
            eventType = UpdateThread,
            connection = connection,
            data = ThreadEventData(
                thread = Thread(thread)
            )
        )
    )

}
