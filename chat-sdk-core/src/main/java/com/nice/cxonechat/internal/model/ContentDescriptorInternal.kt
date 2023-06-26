package com.nice.cxonechat.internal.model

import com.nice.cxonechat.message.ContentDescriptor

internal data class ContentDescriptorInternal(
    override val content: DataSource,
    override val mimeType: String?,
    override val fileName: String?,
    override val friendlyName: String?
) : ContentDescriptor() {
    override fun toString(): String =
        "ContentDescriptor(content=$content, mimeType=$mimeType, fileName=$fileName, friendlyName=$friendlyName)"
}
