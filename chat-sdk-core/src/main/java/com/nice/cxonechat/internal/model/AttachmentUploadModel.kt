package com.nice.cxonechat.internal.model

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.message.ContentDescriptor

internal data class AttachmentUploadModel(
    @SerializedName("content")
    val content: String? = null,

    @SerializedName("mimeType")
    val mimeType: String? = null,

    @SerializedName("fileName")
    val fileName: String? = null,
) {

    constructor(
        upload: ContentDescriptor,
    ) : this(
        content = upload.content,
        mimeType = upload.mimeType,
        fileName = upload.fileName
    )

}
