package com.nice.cxonechat.internal.socket

import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * Class which is delegating incoming method calls to registered child listeners.
 */
internal class ProxyWebSocketListener : WebSocketListener() {

    private val listeners: MutableCollection<WebSocketListener> = Collections.newSetFromMap(ConcurrentHashMap())

    fun addListener(listener: WebSocketListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: WebSocketListener) {
        listeners.remove(listener)
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        for (listener in listeners) {
            listener.onClosed(webSocket, code, reason)
        }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        for (listener in listeners) {
            listener.onClosing(webSocket, code, reason)
        }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        for (listener in listeners) {
            listener.onFailure(webSocket, t, response)
        }
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        for (listener in listeners) {
            listener.onMessage(webSocket, text)
        }
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        for (listener in listeners) {
            listener.onMessage(webSocket, bytes)
        }
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        for (listener in listeners) {
            listener.onOpen(webSocket, response)
        }
    }
}
