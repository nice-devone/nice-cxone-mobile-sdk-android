package com.nice.cxonechat.internal.model

import com.nice.cxonechat.internal.model.network.MessagePolyElement
import com.nice.cxonechat.message.PluginElement.Countdown
import com.nice.cxonechat.util.plus
import java.util.Date
import kotlin.time.Duration.Companion.seconds as duration

internal data class PluginElementCountdown(
    private val element: MessagePolyElement.Countdown,
) : Countdown() {

    override val endsAt: Date
        get() = element.variables.startedAt + element.variables.seconds.duration.inWholeMilliseconds

    override val isExpired: Boolean
        get() = Date().after(endsAt)

    override fun toString() = buildString {
        append("PluginElement.CountDown(endsAt=")
        append(endsAt)
        append(", isExpired=")
        append(isExpired)
        append(")")
    }
}
