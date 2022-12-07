package com.nice.cxonechat.socket

import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketAdapter
import com.neovisionaries.ws.client.WebSocketFrame
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.finest
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.util.ellipsize

internal class EventLogger(
    logger: Logger,
) : WebSocketAdapter(), LoggerScope by LoggerScope<WebSocket>(logger) {

    init {
        finest("Registered dispatch listener")
    }

    override fun onTextMessage(
        websocket: WebSocket?,
        text: String?,
    ) = scope("onTextMessage") {
        finest(text ?: return@scope)
    }

    override fun onFrameSent(
        websocket: WebSocket?,
        frame: WebSocketFrame?,
    ) = scope("onFrameSent") {
        finest(frame?.payloadText?.ellipsize() ?: return@scope)
    }
}
