package com.nice.cxonechat.event

import com.nice.cxonechat.Public
import com.nice.cxonechat.enums.VisitorEventType.Conversion
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.storage.ValueStorage
import java.util.Date
import com.nice.cxonechat.internal.model.network.ActionStoreVisitorEvent as StoreVisitorEventsModel
import com.nice.cxonechat.internal.model.network.Conversion as ConversionModel

/**
 * Event notifying the backend that a conversion has been made.
 *
 * Conversions are understood as a completed activities that are important
 * to your business.
 * */
@Public
class ConversionEvent(
    private val type: String,
    private val value: Number,
) : ChatEvent() {

    override fun getModel(
        connection: Connection,
        storage: ValueStorage,
    ): Any {
        val conversion = ConversionModel(
            type = type,
            value = value,
            timestamp = Date()
        )
        return StoreVisitorEventsModel(
            connection = connection,
            visitor = storage.visitorId,
            destination = storage.destinationId,
            Conversion to conversion,
            createdAt = Date()
        )
    }
}
