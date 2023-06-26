package com.nice.cxonechat.internal.model

import com.nice.cxonechat.message.Attachment

internal data class AttachmentInternal(
    override val url: String,
    override val friendlyName: String,
    override val mimeType: String?,
) : Attachment() {

    override fun toString() = buildString {
        append("Attachment(url='")
        append(url)
        append("', friendlyName='")
        append(friendlyName)
        append("', mimeType=")
        append(mimeType)
        append(")")
    }
}
