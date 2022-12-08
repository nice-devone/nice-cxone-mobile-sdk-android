package com.nice.cxonechat.internal.model

import com.nice.cxonechat.internal.model.network.MessagePolyElement
import com.nice.cxonechat.message.PluginElement.InactivityPopup

internal data class PluginElementInactivityPopup(
    private val element: MessagePolyElement.InactivityPopup,
) : InactivityPopup() {

    private val pluginElements = element.elements
        .asSequence()
        .mapNotNull(::PluginElement)

    override val title: Title
        get() = pluginElements
            .filterIsInstance<Title>()
            .first()
    override val subtitle: Subtitle?
        get() = pluginElements
            .filterIsInstance<Subtitle>()
            .firstOrNull()
    override val texts: Iterable<Text>
        get() = pluginElements
            .filterIsInstance<Text>()
            .asIterable()
    override val buttons: Iterable<Button>
        get() = pluginElements
            .filterIsInstance<Button>()
            .asIterable()
    override val countdown: Countdown
        get() = pluginElements
            .filterIsInstance<Countdown>()
            .first()

    override fun toString() = buildString {
        append("PluginElement.InactivityPopup(title=")
        append(title)
        append(", texts=")
        append(texts.toList())
        append(", buttons=")
        append(buttons.toList())
        append(", countdown=")
        append(countdown)
        append(")")
    }

}
