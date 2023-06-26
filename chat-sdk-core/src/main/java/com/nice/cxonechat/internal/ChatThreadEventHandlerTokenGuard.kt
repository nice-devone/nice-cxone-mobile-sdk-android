package com.nice.cxonechat.internal

import com.nice.cxonechat.ChatThreadEventHandler
import com.nice.cxonechat.ChatThreadEventHandler.OnEventSentListener
import com.nice.cxonechat.event.RefreshToken
import com.nice.cxonechat.event.thread.ChatThreadEvent
import com.nice.cxonechat.util.expiresWithin
import java.util.Date
import kotlin.time.Duration.Companion.seconds

internal class ChatThreadEventHandlerTokenGuard(
    private val origin: ChatThreadEventHandler,
    private val chat: ChatWithParameters,
) : ChatThreadEventHandler by origin {

    override fun trigger(event: ChatThreadEvent, listener: OnEventSentListener?) {
        val expiresAt = chat.storage.authTokenExpDate ?: Date(Long.MAX_VALUE)
        if (expiresAt.expiresWithin(10.seconds)) {
            chat.events().trigger(RefreshToken)
        }
        origin.trigger(event, listener)
    }
}
