package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.enums.EventAction
import com.nice.cxonechat.enums.EventAction.ChatWindowEvent
import com.nice.cxonechat.enums.EventType
import com.nice.cxonechat.enums.EventType.SenderTypingEnded
import com.nice.cxonechat.enums.EventType.SenderTypingStarted
import com.nice.cxonechat.internal.model.Thread
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.thread.ChatThread
import java.util.UUID

internal data class ActionCustomerTyping(
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
        type: EventType,
    ) : this(
        payload = Payload(
            eventType = type,
            connection = connection,
            data = Data(
                thread = Thread(thread)
            )
        )
    )

    data class Data(
        @SerializedName("thread")
        val thread: Thread,
    )

    companion object {

        fun started(
            connection: Connection,
            thread: ChatThread,
        ) = ActionCustomerTyping(
            connection = connection,
            thread = thread,
            type = SenderTypingStarted
        )

        fun ended(
            connection: Connection,
            thread: ChatThread,
        ) = ActionCustomerTyping(
            connection = connection,
            thread = thread,
            type = SenderTypingEnded
        )
    }
}
