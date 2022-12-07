package com.nice.cxonechat.event

import com.nice.cxonechat.internal.model.network.ActionReconnectCustomer
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.storage.ValueStorage

internal object ReconnectCustomerEvent : ChatEvent() {

    override fun getModel(
        connection: Connection,
        storage: ValueStorage,
    ) = ActionReconnectCustomer(
        connection = connection,
        token = storage.authToken.let(::requireNotNull),
        visitor = storage.visitorId
    )

}
