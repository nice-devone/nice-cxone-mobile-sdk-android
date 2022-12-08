package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName

internal data class CustomVariable constructor(
    @SerializedName("identifier")
    val identifier: String,
    @SerializedName("value")
    val value: String,
)
