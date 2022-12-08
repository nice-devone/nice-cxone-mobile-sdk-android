package com.nice.cxonechat.internal

import androidx.annotation.WorkerThread
import com.neovisionaries.ws.client.WebSocket
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.storage.ValueStorage

internal interface SocketFactory {

    @WorkerThread
    fun create(): WebSocket
    fun getConfiguration(storage: ValueStorage): Connection

}
