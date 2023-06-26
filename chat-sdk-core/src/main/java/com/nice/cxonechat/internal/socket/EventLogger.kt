package com.nice.cxonechat.internal.socket

import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.finer
import com.nice.cxonechat.log.finest
import com.nice.cxonechat.log.scope
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

internal class EventLogger(
    logger: Logger,
) : WebSocketListener(), LoggerScope by LoggerScope<WebSocket>(logger) {

    init {
        finest("Registered dispatch listener")
    }

    override fun onMessage(
        webSocket: WebSocket,
        text: String,
    ) = scope("onMessage") {
        finest(text)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) = scope("onFailure") {
        finer("Response: $response", t)
    }
}
