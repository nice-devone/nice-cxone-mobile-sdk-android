package com.nice.cxonechat.event

import com.nice.cxonechat.Public
import com.nice.cxonechat.analytics.ActionMetadata
import com.nice.cxonechat.enums.VisitorEventType.ProactiveActionDisplayed
import com.nice.cxonechat.internal.model.network.ProactiveActionInfo
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.storage.ValueStorage
import java.util.Date
import com.nice.cxonechat.internal.model.network.ActionStoreVisitorEvent as StoreVisitorEventsModel

/**
 * Event notifying the backend that a proactive action has been displayed to the user.
 * */
@Public
class ProactiveActionDisplayEvent(
    data: ActionMetadata,
) : ChatEvent() {

    private val data = ProactiveActionInfo(data)

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
