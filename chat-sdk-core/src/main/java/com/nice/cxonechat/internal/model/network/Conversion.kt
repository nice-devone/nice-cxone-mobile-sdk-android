package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName
import java.util.Date

internal data class Conversion constructor(
    @SerializedName("conversionType")
    val type: String,
    @SerializedName("conversionValue")
    val value: Number,
    @SerializedName("conversionTimeWithMilliseconds")
    val timestamp: Date,
)
