package com.nice.cxonechat.internal

import com.nice.cxonechat.ChatEventHandler
import com.nice.cxonechat.ChatEventHandler.OnEventSentListener
import com.nice.cxonechat.event.ChatEvent
import com.nice.cxonechat.event.RefreshToken
import com.nice.cxonechat.util.expiresWithin
import java.util.Date
import kotlin.time.Duration.Companion.seconds

internal class ChatEventHandlerTokenGuard(
    private val origin: ChatEventHandler,
    private val chat: ChatWithParameters,
) : ChatEventHandler by origin {

    override fun trigger(event: ChatEvent, listener: OnEventSentListener?) {
        val expiresAt = chat.storage.authTokenExpDate ?: Date(Long.MAX_VALUE)
        if (expiresAt.expiresWithin(10.seconds) && event !is RefreshToken) {
            origin.trigger(RefreshToken)
        }
        origin.trigger(event, listener)
    }

}
