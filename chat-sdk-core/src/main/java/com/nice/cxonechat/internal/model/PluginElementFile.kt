package com.nice.cxonechat.internal.model

import com.nice.cxonechat.internal.model.network.MessagePolyElement
import com.nice.cxonechat.message.PluginElement.File

internal data class PluginElementFile(
    private val element: MessagePolyElement.File,
) : File() {

    override val url: String
        get() = element.url
    override val name: String
        get() = element.fileName
    override val mimeType: String
        get() = element.fileName

    override fun toString() = buildString {
        append("PluginElement.File(url='")
        append(url)
        append("', name='")
        append(name)
        append("', mimeType='")
        append(mimeType)
        append("')")
    }
}
