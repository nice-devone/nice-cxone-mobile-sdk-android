package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.enums.ElementType

/** All info about a plugin element in a message. */
internal data class MessageElement(
    @SerializedName("id")
    val id: String,
    @SerializedName("type")
    val type: ElementType,
    @SerializedName("text")
    val text: String,
    @SerializedName("postback")
    val postback: String?,
    @SerializedName("url")
    val url: String?,
    @SerializedName("fileName")
    val fileName: String?,
    @SerializedName("mimeType")
    val mimeType: String?,
    @SerializedName("elements")
    val elements: List<MessageElement>?,
    @SerializedName("variables")
    val variables: Any,
)
