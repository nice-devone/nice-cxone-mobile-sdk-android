package com.nice.cxonechat.api.model

import com.google.gson.annotations.SerializedName

internal data class AttachmentUploadResponse(
    @SerializedName("fileUrl")
    val fileUrl: String? = null,
)
