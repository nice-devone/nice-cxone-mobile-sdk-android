package com.nice.cxonechat.tool

import com.nice.cxonechat.internal.socket.ProxyWebSocketListener
import io.mockk.every
import io.mockk.mockk
import okhttp3.WebSocket

internal class MockServer {

    val socket: WebSocket = mockk {
        every { send(text = any()) } returns true
    }
    val proxyListener: ProxyWebSocketListener = ProxyWebSocketListener()

    fun sendServerMessage(text: String) {
        proxyListener.onMessage(socket, text)
    }
}
