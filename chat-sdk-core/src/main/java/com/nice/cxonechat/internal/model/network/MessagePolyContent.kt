package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName

internal sealed class MessagePolyContent {

    data class Text(
        @SerializedName("payload")
        val payload: Payload,
    ) : MessagePolyContent() {

        data class Payload(
            @SerializedName("text")
            val text: String,
        )

    }

    data class Plugin(
        @SerializedName("payload")
        val payload: Payload,
    ) : MessagePolyContent() {

        data class Payload(
            @SerializedName("postback")
            val postback: String?,
            @SerializedName("elements")
            val elements: List<MessagePolyElement>,
        )

    }

    object Noop : MessagePolyContent()

}
