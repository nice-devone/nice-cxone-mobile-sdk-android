package com.nice.cxonechat.event

import com.nice.cxonechat.internal.model.network.ActionFetchThread
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.storage.ValueStorage

internal object FetchThreadEvent : ChatEvent() {

    override fun getModel(
        connection: Connection,
        storage: ValueStorage,
    ) = ActionFetchThread(
        connection = connection
    )
}
