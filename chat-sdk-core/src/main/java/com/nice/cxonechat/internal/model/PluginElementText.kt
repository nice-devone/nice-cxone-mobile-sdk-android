package com.nice.cxonechat.internal.model

import com.nice.cxonechat.internal.model.network.MessagePolyElement
import com.nice.cxonechat.message.PluginElement.Text

internal data class PluginElementText(
    private val element: MessagePolyElement.Text,
) : Text() {

    override val text: String
        get() = element.text

    override val isMarkdown: Boolean
        get() = element.mimeType.equals("text/markdown", ignoreCase = true)

    override val isHtml: Boolean
        get() = element.mimeType.equals("text/html", ignoreCase = true)

    override fun toString() = buildString {
        append("PluginElement.Text(text='")
        append(text)
        append("', isMarkdown=")
        append(isMarkdown)
        append(", isHtml=")
        append(isHtml)
        append(")")
    }

}
