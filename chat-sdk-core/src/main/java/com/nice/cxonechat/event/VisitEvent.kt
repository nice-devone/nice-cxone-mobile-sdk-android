package com.nice.cxonechat.event

import com.nice.cxonechat.Public
import com.nice.cxonechat.enums.VisitorEventType.VisitorVisit
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.storage.ValueStorage
import java.util.Date
import com.nice.cxonechat.internal.model.network.ActionStoreVisitorEvent as StoreVisitorEventsModel

/**
 * Event notifying the backend that user has entered the chat area
 * of the given application. Session length is capped at 30 minutes
 * thereafter you need to resend the visit event if the user
 * performs any action.
 * */
@Public
object VisitEvent : ChatEvent() {

    override fun getModel(
        connection: Connection,
        storage: ValueStorage,
    ): Any = StoreVisitorEventsModel(
        connection = connection,
        visitor = storage.visitorId,
        destination = storage.destinationId,
        VisitorVisit to null,
        createdAt = Date()
    )

}
