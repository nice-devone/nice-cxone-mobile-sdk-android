package com.nice.cxonechat.tool

import com.nice.cxonechat.enums.CXOneEnvironment.EU1
import com.nice.cxonechat.internal.model.ConnectionInternal
import com.nice.cxonechat.internal.socket.ProxyWebSocketListener
import com.nice.cxonechat.internal.socket.SocketFactory
import com.nice.cxonechat.storage.ValueStorage
import okhttp3.WebSocket
import okhttp3.WebSocketListener

internal class SocketFactoryMock(
    private val socket: WebSocket,
    private val proxyListener: ProxyWebSocketListener,
) : SocketFactory {

    override fun create(listener: WebSocketListener): WebSocket = socket

    override fun createProxyListener(): ProxyWebSocketListener = proxyListener

    override fun getConfiguration(storage: ValueStorage) = ConnectionInternal(
        brandId = 0,
        channelId = channelId,
        firstName = firstName,
        lastName = lastName,
        customerId = storage.customerId,
        environment = EU1.value,
        visitorId = storage.visitorId,
    )

    companion object {
        const val firstName = "first"
        const val lastName = "last"
        const val channelId = "channelId"
    }
}
