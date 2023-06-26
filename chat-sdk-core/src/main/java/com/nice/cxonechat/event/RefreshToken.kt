package com.nice.cxonechat.event

import com.nice.cxonechat.internal.model.network.ActionRefreshToken
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.storage.ValueStorage

/**
 * Event notifying the backend about a token refresh that ought to be performed.
 *
 * This can be requested at any point, but is generally recommended to at or before
 * expiration of given token.
 * */
internal object RefreshToken : ChatEvent() {

    override fun getModel(
        connection: Connection,
        storage: ValueStorage,
    ) = ActionRefreshToken(
        connection = connection,
        token = storage.authToken.let(::requireNotNull)
    )
}
