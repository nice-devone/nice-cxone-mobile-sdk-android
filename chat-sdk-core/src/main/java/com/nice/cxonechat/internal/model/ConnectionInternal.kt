package com.nice.cxonechat.internal.model

import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.state.Environment
import java.util.UUID

internal data class ConnectionInternal(
    override val brandId: Int,
    override val channelId: String,
    override val firstName: String,
    override val lastName: String,
    override val consumerId: UUID?,
    override val environment: Environment,
) : Connection() {

    override fun toString() = buildString {
        append("Connection(brandId=")
        append(brandId)
        append(", channelId='")
        append(channelId)
        append("', firstName='")
        append(firstName)
        append("', lastName='")
        append(lastName)
        append("', consumerId=")
        append(consumerId)
        append(", environment=")
        append(environment)
        append(")")
    }

}
