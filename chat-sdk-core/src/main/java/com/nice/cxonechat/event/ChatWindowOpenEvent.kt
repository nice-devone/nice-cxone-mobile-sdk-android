package com.nice.cxonechat.event

import com.nice.cxonechat.Public
import com.nice.cxonechat.enums.VisitorEventType.ChatWindowOpened
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.storage.ValueStorage
import java.util.Date
import com.nice.cxonechat.internal.model.network.ActionStoreVisitorEvent as StoreVisitorEventsModel

/**
 * Event notifying the backend about a chat screen window that's been opened.
 * */
@Public
object ChatWindowOpenEvent : ChatEvent() {

    override fun getModel(
        connection: Connection,
        storage: ValueStorage,
    ): Any = StoreVisitorEventsModel(
        connection = connection,
        visitor = storage.visitorId,
        destination = storage.destinationId,
        ChatWindowOpened to null,
        createdAt = Date(),
    )
}
