package com.nice.cxonechat.internal.model

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.message.Attachment

internal data class AttachmentModel(
    @SerializedName("url")
    val url: String,

    @SerializedName("friendlyName")
    val friendlyName: String,

    @SerializedName("mimeType")
    val mimeType: String?,
) {

    fun toAttachment(): Attachment = AttachmentInternal(
        url = url,
        friendlyName = friendlyName,
        mimeType = mimeType
    )
}
