package com.nice.cxonechat.internal.model

import com.google.gson.annotations.SerializedName

internal data class NodeModel(
    @SerializedName("name")
    val name: String,
    @SerializedName("value")
    val value: String,
    @SerializedName("parentId")
    val parentId: String?,
)
