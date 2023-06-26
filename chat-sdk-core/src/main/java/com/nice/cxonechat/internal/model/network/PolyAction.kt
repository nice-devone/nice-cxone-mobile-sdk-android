package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName

internal sealed class PolyAction {
    data class ReplyButton(
        @SerializedName("text")
        val text: String,
        @SerializedName("postback")
        val postback: String?,
        @SerializedName("icon")
        val media: MediaModel?,
        @SerializedName("description")
        val description: String?
    ) : PolyAction()
}
