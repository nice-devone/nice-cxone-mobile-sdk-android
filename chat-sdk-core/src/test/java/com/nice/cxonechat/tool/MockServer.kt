package com.nice.cxonechat.tool

import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketFrame
import com.neovisionaries.ws.client.WebSocketListener
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

internal class MockServer {

    val socket: WebSocket = mock()
    private val listeners = mutableSetOf<WebSocketListener>()

    init {
        whenever(socket.addListener(any())).then {
            listeners.add(it.getArgument(0))
            socket
        }
        whenever(socket.removeListener(any())).then {
            listeners.remove(it.getArgument(0))
            socket
        }
    }

    fun sendServerMessage(text: String?) {
        listeners.forEach {
            it.onTextMessage(socket, text)
        }
    }

    fun sendAcknowledgement(frame: WebSocketFrame) {
        listeners.forEach { it.onFrameSent(socket, frame) }
    }

}
