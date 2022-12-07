package com.nice.cxonechat.internal.model

import com.nice.cxonechat.message.ContentDescriptor
import okio.utf8Size

internal data class ContentDescriptorInternal(
    override val content: String,
    override val mimeType: String?,
    override val fileName: String?,
) : ContentDescriptor() {

    override fun toString() = buildString {
        append("ContentDescriptor(content.utf8Size()='")
        append(content.utf8Size())
        append("', mimeType=")
        append(mimeType)
        append(", fileName=")
        append(fileName)
        append(")")
    }

}
