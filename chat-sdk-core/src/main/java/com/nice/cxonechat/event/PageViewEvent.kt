package com.nice.cxonechat.event

import com.nice.cxonechat.Public
import com.nice.cxonechat.enums.VisitorEventType.PageView
import com.nice.cxonechat.internal.model.network.PageViewData
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.storage.ValueStorage
import java.util.Date
import com.nice.cxonechat.internal.model.network.ActionStoreVisitorEvent as StoreVisitorEventsModel

/**
 * Event notifying the backend that user has clicked a url in chat or visited other unspecified
 * url withing the chat platform.
 * */
@Public
class PageViewEvent(
    private val title: String,
    private val uri: String,
) : ChatEvent() {

    override fun getModel(
        connection: Connection,
        storage: ValueStorage,
    ): Any {
        val model = PageViewData(
            url = uri,
            title = title
        )
        return StoreVisitorEventsModel(
            connection = connection,
            visitor = storage.visitorId,
            destination = storage.destinationId,
            PageView to model,
            createdAt = Date()
        )
    }
}
