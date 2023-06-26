package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName

internal data class WrappedText(
    @SerializedName("content")
    val content: String
)
