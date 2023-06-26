package com.nice.cxonechat.internal.model

import com.nice.cxonechat.internal.model.network.MessagePolyElement
import com.nice.cxonechat.message.PluginElement.QuickReplies

internal data class PluginElementQuickReplies(
    private val element: MessagePolyElement.QuickReplies,
) : QuickReplies() {

    private val pluginElements = element.elements
        .asSequence()
        .mapNotNull(::PluginElement)

    override val text: Text
        get() = pluginElements
            .filterIsInstance<Text>()
            .first()
    override val buttons: Iterable<Button>
        get() = pluginElements
            .filterIsInstance<Button>()
            .asIterable()

    override fun toString() = buildString {
        append("PluginElement.QuickReplies(text=")
        append(text)
        append(", buttons=")
        append(buttons.toList())
        append(")")
    }
}
