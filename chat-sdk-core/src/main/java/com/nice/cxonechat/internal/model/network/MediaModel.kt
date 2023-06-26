package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName

internal data class MediaModel(
    @SerializedName("fileName")
    val fileName: String,
    @SerializedName("url")
    val url: String,
    @SerializedName("mimeType")
    val mimeType: String
)
