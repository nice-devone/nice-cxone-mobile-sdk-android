package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName

internal data class MessagePayload(
    @SerializedName("text")
    val text: String,
    @SerializedName("elements")
    val elements: List<MessageElement> = emptyList(),
)
