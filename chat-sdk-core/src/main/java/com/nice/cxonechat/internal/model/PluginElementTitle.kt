package com.nice.cxonechat.internal.model

import com.nice.cxonechat.internal.model.network.MessagePolyElement
import com.nice.cxonechat.message.PluginElement.Title

internal data class PluginElementTitle(
    private val element: MessagePolyElement.Title,
) : Title() {

    override val text: String
        get() = element.text

    override fun toString() = buildString {
        append("PluginElement.Title(text='")
        append(text)
        append("')")
    }
}
