package com.nice.cxonechat.event

import com.nice.cxonechat.Public
import com.nice.cxonechat.analytics.ActionMetadata
import com.nice.cxonechat.enums.VisitorEventType.ProactiveActionClicked
import com.nice.cxonechat.internal.model.network.ProactiveActionInfo
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.storage.ValueStorage
import java.util.Date
import com.nice.cxonechat.internal.model.network.ActionStoreVisitorEvent as StoreVisitorEventsModel

/**
 * Event notifying the backend that a proactive action has been clicked by the user.
 * */
@Public
class ProactiveActionClickEvent(
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
        ProactiveActionClicked to data,
        createdAt = Date()
    )

}
