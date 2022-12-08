package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName

internal data class AccessTokenPayload constructor(
    @SerializedName("token")
    val token: String,
)
