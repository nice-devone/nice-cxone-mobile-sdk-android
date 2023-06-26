package com.nice.cxonechat.internal.model

import com.google.gson.annotations.SerializedName

internal data class SelectorModel(
    @SerializedName("name")
    val name: String,
    @SerializedName("value")
    val label: String,
)
