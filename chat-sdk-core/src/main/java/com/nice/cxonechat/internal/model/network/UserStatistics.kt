package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.internal.model.MessageMetadataInternal
import com.nice.cxonechat.message.MessageMetadata
import java.util.Date

internal data class UserStatistics(
    @SerializedName("seenAt")
    val seenAt: Date?,

    @SerializedName("readAt")
    val readAt: Date?,
) {

    fun toMessageMetadata(): MessageMetadata = MessageMetadataInternal(
        readAt = readAt
    )
}
