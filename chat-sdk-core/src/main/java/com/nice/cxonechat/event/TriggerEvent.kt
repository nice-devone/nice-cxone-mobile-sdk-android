package com.nice.cxonechat.event

import com.nice.cxonechat.Public
import com.nice.cxonechat.internal.model.network.ActionExecuteTrigger
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.storage.ValueStorage
import java.util.UUID

/**
 * Event that triggers event specified in agent console or elsewhere as per your
 * representative instructions. This event is not mandatory though, consult your
 * representative for more information.
 * */
@Public
class TriggerEvent(
    private val id: UUID,
) : ChatEvent() {

    override fun getModel(
        connection: Connection,
        storage: ValueStorage,
    ): Any = ActionExecuteTrigger(
        connection = connection,
        destination = storage.destinationId,
        visitor = storage.visitorId,
        id = id
    )
}
