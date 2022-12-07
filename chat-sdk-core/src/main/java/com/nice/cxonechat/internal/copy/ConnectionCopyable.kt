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
        consumerId: UUID? = connection.consumerId,
        environment: Environment = connection.environment,
    ) = ConnectionInternal(
        brandId = brandId,
        channelId = channelId,
        firstName = firstName,
        lastName = lastName,
        consumerId = consumerId,
        environment = environment
    )

    companion object {

        fun Connection.asCopyable() =
            ConnectionCopyable(this)

    }

}
