package com.nice.cxonechat.internal.model

import com.nice.cxonechat.internal.model.network.MessagePolyElement
import com.nice.cxonechat.message.PluginElement.Custom

internal data class PluginElementCustom(
    private val element: MessagePolyElement.Custom,
) : Custom() {

    override val fallbackText: String?
        get() = element.fallbackText
    override val variables: Map<String, Any?>
        get() = element.variables

    override fun toString() = buildString {
        append("PluginElement.Custom(fallbackText=")
        append(fallbackText)
        append(", variables=")
        append(variables)
        append(")")
    }
}
