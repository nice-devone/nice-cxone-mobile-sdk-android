package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName

internal data class Referrer constructor(
    @SerializedName("url")
    val url: String,
)
