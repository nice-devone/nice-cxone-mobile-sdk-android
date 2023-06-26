package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.enums.EventAction
import com.nice.cxonechat.enums.EventAction.ChatWindowEvent
import com.nice.cxonechat.enums.EventType.LoadMoreMessages
import com.nice.cxonechat.internal.model.Thread
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.thread.ChatThread
import com.nice.cxonechat.util.DateTime
import java.util.UUID

internal data class ActionLoadMoreMessages(
    @SerializedName("action")
    val action: EventAction = ChatWindowEvent,
    @SerializedName("eventId")
    val eventId: UUID = UUID.randomUUID(),
    @SerializedName("payload")
    val payload: Payload<Data>,
) {

    constructor(
        connection: Connection,
        thread: ChatThread,
    ) : this(
        payload = Payload(
            eventType = LoadMoreMessages,
            connection = connection,
            data = Data(
                scrollToken = thread.scrollToken,
                thread = Thread(thread),
                oldestMessageDatetime = DateTime(thread.messages.last().createdAt)
            )
        )
    )

    data class Data(
        @SerializedName("scrollToken")
        val scrollToken: String,
        @SerializedName("thread")
        val thread: Thread,
        @SerializedName("oldestMessageDatetime")
        val oldestMessageDatetime: DateTime,
    )
}
