package com.nice.cxonechat.tool

import com.nice.cxonechat.internal.socket.ProxyWebSocketListener
import okhttp3.WebSocket
import org.mockito.kotlin.mock

internal class MockServer {

    val socket: WebSocket = mock()
    val proxyListener: ProxyWebSocketListener = ProxyWebSocketListener()

    fun sendServerMessage(text: String) {
        proxyListener.onMessage(socket, text)
    }
}
