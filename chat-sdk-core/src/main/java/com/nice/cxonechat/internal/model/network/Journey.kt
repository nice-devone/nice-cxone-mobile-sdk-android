package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName

internal data class Journey constructor(
    @SerializedName("referrer")
    val referrer: Referrer,
    @SerializedName("utm")
    val utm: UTM,
)
