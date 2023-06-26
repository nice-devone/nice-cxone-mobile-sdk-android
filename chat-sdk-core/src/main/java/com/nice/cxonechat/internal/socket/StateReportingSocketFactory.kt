package com.nice.cxonechat.internal.socket

import com.nice.cxonechat.ChatStateListener
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/**
 * This class wraps provided [SocketFactory] implementation and attaches an extra [WebSocketListener] implementation,
 * which is providing state reporting functionality, to the instance of [ProxyWebSocketListener] provided by the
 * [createProxyListener] call.
 */
internal class StateReportingSocketFactory(
    chatStateListener: ChatStateListener,
    private val socketFactory: SocketFactory,
) : SocketFactory by socketFactory {

    private val webSocketListener = StateReportingWebSocketListener(chatStateListener)

    override fun createProxyListener() = socketFactory.createProxyListener().apply {
        addListener(webSocketListener)
    }

    private class StateReportingWebSocketListener(
        private val chatStateListener: ChatStateListener,
    ) : WebSocketListener() {
        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            chatStateListener.onUnexpectedDisconnect()
        }

        override fun onOpen(webSocket: WebSocket, response: Response) {
            chatStateListener.onConnected()
        }
    }
}
