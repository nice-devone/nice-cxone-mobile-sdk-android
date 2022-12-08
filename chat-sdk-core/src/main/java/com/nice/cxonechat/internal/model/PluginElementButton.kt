package com.nice.cxonechat.internal.model

import com.nice.cxonechat.internal.model.network.MessagePolyElement
import com.nice.cxonechat.message.PluginElement.Button

internal data class PluginElementButton(
    private val element: MessagePolyElement.Button,
) : Button() {

    override val text: String
        get() = element.text
    override val postback: String
        get() = element.postback
    override val deepLink: String?
        get() = element.deepLink

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
