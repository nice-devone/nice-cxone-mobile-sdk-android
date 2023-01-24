package com.nice.cxonechat.internal.model

import com.nice.cxonechat.internal.model.network.MessagePolyElement
import com.nice.cxonechat.internal.model.network.MessagePolyElement.DeeplinkButton
import com.nice.cxonechat.internal.model.network.MessagePolyElement.IFrameButton
import com.nice.cxonechat.message.PluginElement.Button

internal data class PluginElementButton(
    private val element: MessagePolyElement.Button,
) : Button() {

    override val text: String
        get() = element.text
    override val postback: String?
        get() = element.postback
    override val deepLink: String?
        get() = when (element) {
            is DeeplinkButton -> element.url?.takeIf { it.isNotBlank() } ?: element.deepLink
            is IFrameButton -> element.url
        }
    override val displayInApp: Boolean
        get() = element is IFrameButton && deepLink != null

    override fun toString() = buildString {
        append("PluginElement.Button(text='")
        append(text)
        append("', postback='")
        append(postback)
        append("', deepLink=")
        append(deepLink)
        append(")")
    }
}
