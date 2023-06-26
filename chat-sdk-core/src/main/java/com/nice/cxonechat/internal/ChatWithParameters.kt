package com.nice.cxonechat.internal

import com.nice.cxonechat.Chat
import com.nice.cxonechat.internal.socket.ProxyWebSocketListener
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.thread.CustomField
import okhttp3.WebSocket

internal interface ChatWithParameters : Chat {

    val entrails: ChatEntrails
    val socket: WebSocket
    val socketListener: ProxyWebSocketListener
    var connection: Connection
    override var fields: List<CustomField>

    val storage get() = entrails.storage
    val service get() = entrails.service
}
