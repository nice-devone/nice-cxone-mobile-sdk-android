package com.nice.cxonechat.internal.socket

import androidx.annotation.WorkerThread
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.storage.ValueStorage
import okhttp3.WebSocket
import okhttp3.WebSocketListener

internal interface SocketFactory {

    @WorkerThread
    fun create(listener: WebSocketListener): WebSocket

    fun createProxyListener(): ProxyWebSocketListener
    fun getConfiguration(storage: ValueStorage): Connection
}
