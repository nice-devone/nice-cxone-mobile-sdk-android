package com.nice.cxonechat.socket

import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketAdapter
import com.neovisionaries.ws.client.WebSocketFrame
import com.nice.cxonechat.socket.SocketDefaults.serializer

internal abstract class SendSelfRemovingCallback : WebSocketAdapter() {

    final override fun onFrameSent(websocket: WebSocket?, frame: WebSocketFrame?) {
        websocket?.removeListener(this)
        onSent()
    }

    abstract fun onSent()

    companion object {

        inline operator fun invoke(
            crossinline callback: () -> Unit,
        ) = object : SendSelfRemovingCallback() {
            override fun onSent() = callback()
        }

        fun WebSocket.send(model: Any) {
            val text = serializer.toJson(model)
            sendText(text)
        }

        fun WebSocket.send(model: Any, callback: () -> Unit) {
            val text = serializer.toJson(model)
            addListener(invoke(callback))
            sendText(text)
        }

    }

}
