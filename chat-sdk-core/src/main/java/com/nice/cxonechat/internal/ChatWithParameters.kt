package com.nice.cxonechat.internal

import com.neovisionaries.ws.client.WebSocket
import com.nice.cxonechat.Chat
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.thread.CustomField

internal interface ChatWithParameters : Chat {

    val entrails: ChatEntrails
    val socket: WebSocket
    var connection: Connection
    override var fields: List<CustomField>

    val storage get() = entrails.storage
    val service get() = entrails.service

}
