package com.nice.cxonechat.internal.socket

import com.nice.cxonechat.internal.serializer.Default.serializer
import okhttp3.WebSocket

/**
 * Serialize [model] and send it as text via [WebSocket].
 */
internal fun WebSocket.send(model: Any) {
    val text = serializer.toJson(model)
    send(text = text)
}

/**
 * Serialize [model] and send it as text via [WebSocket].
 * [callback] will be invoked if the [send] has reported that message has been enqueued.
 */
internal fun WebSocket.send(model: Any, callback: () -> Unit) {
    val text = serializer.toJson(model)
    if (send(text = text)) {
        callback()
    }
}
