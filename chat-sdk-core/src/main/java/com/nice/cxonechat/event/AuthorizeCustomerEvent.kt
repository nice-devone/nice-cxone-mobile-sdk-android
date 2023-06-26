package com.nice.cxonechat.event

import com.nice.cxonechat.internal.model.network.ActionAuthorizeCustomer
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.storage.ValueStorage

internal class AuthorizeCustomerEvent(
    private val code: String,
    private val verifier: String,
) : ChatEvent() {

    override fun getModel(
        connection: Connection,
        storage: ValueStorage,
    ) = ActionAuthorizeCustomer(
        connection = connection,
        code = code,
        verifier = verifier
    )
}
