package com.nice.cxonechat

import com.nice.cxonechat.internal.ChatWithParameters

internal class ChatThreadingImpl(
    private val origin: ChatWithParameters
) : ChatWithParameters by origin {
    override fun reconnect(): Cancellable = origin.entrails.threading.background {
        origin.reconnect()
    }
}
