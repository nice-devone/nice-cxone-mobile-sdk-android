package com.nice.cxonechat.internal.model

import com.nice.cxonechat.message.MessageMetadata
import java.util.Date

internal data class MessageMetadataInternal(
    override val readAt: Date?,
) : MessageMetadata() {

    override fun toString() = buildString {
        append("MessageMetadata(readAt=")
        append(readAt)
        append(")")
    }
}
