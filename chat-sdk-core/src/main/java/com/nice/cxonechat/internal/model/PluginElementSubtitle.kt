package com.nice.cxonechat.internal.model

import com.nice.cxonechat.internal.model.network.MessagePolyElement
import com.nice.cxonechat.message.PluginElement.Subtitle

internal data class PluginElementSubtitle(
    private val element: MessagePolyElement.Subtitle,
) : Subtitle() {

    override val text: String
        get() = element.text

    override fun toString() = buildString {
        append("PluginElement.Subtitle(text='")
        append(text)
        append("')")
    }
}
