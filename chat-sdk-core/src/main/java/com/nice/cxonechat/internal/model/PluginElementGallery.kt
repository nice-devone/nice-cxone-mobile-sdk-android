package com.nice.cxonechat.internal.model

import com.nice.cxonechat.message.PluginElement
import com.nice.cxonechat.message.PluginElement.Gallery

internal data class PluginElementGallery(
    override val elements: Iterable<PluginElement>,
) : Gallery() {
    override fun toString(): String = "PluginElement.Gallery(elements=$elements)"
}
