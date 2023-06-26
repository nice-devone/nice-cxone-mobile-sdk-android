package com.nice.cxonechat.event

import com.nice.cxonechat.Public
import com.nice.cxonechat.enums.VisitorEventType.ProactiveActionDisplayed
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.storage.ValueStorage
import java.util.Date
import com.nice.cxonechat.internal.model.network.ActionStoreVisitorEvent as StoreVisitorEventsModel

/**
 * Event provided by the client, notifying backend of a new activity performed
 * by user. Consult a representative if you need more information.
 *
 * Generally, this can be any form of data or serializable object.
 * */
@Public
class CustomVisitorEvent(
    private val data: Any,
) : ChatEvent() {

    override fun getModel(
        connection: Connection,
        storage: ValueStorage,
    ): Any = StoreVisitorEventsModel(
        connection = connection,
        visitor = storage.visitorId,
        destination = storage.destinationId,
        ProactiveActionDisplayed to data,
        createdAt = Date()
    )
}
