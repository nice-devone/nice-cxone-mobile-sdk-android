package com.nice.cxonechat.tool

import com.neovisionaries.ws.client.WebSocket
import com.nice.cxonechat.enums.CXOneEnvironment.EU1
import com.nice.cxonechat.internal.SocketFactory
import com.nice.cxonechat.internal.model.ConnectionInternal
import com.nice.cxonechat.storage.ValueStorage

internal class SocketFactoryMock(
    private val socket: WebSocket,
) : SocketFactory {

    override fun create(): WebSocket {
        return socket
    }

    override fun getConfiguration(storage: ValueStorage) = ConnectionInternal(
        brandId = 0,
        channelId = "channelId",
        firstName = firstName,
        lastName = lastName,
        consumerId = storage.consumerId,
        environment = EU1.value
    )

    companion object {
        const val firstName = "first"
        const val lastName = "last"
    }
}
