package com.nice.cxonechat.internal.copy

import com.nice.cxonechat.internal.model.ConnectionInternal
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.state.Environment
import java.util.UUID

@Suppress("LongParameterList")
internal class ConnectionCopyable(
    private val connection: Connection,
) {

    fun copy(
        brandId: Int = connection.brandId,
        channelId: String = connection.channelId,
        firstName: String = connection.firstName,
        lastName: String = connection.lastName,
        customerId: UUID? = connection.customerId,
        environment: Environment = connection.environment,
        visitorId: UUID = connection.visitorId,
    ) = ConnectionInternal(
        brandId = brandId,
        channelId = channelId,
        firstName = firstName,
        lastName = lastName,
        customerId = customerId,
        environment = environment,
        visitorId = visitorId,
    )

    companion object {

        fun Connection.asCopyable() =
            ConnectionCopyable(this)
    }
}
