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

    data class QuickReplies(
        @SerializedName("fallbackText")
        val fallbackText: String,
        @SerializedName("payload")
        val payload: Payload
    ) : MessagePolyContent() {
        data class Payload(
            @SerializedName("text")
            val text: WrappedText,
            @SerializedName("actions")
            val actions: List<PolyAction>
        )
    }

    data class ListPicker(
        @SerializedName("fallbackText")
        val fallbackText: String?,
        @SerializedName("payload")
        val payload: Payload
    ) : MessagePolyContent() {
        data class Payload(
            @SerializedName("title")
            val title: WrappedText,
            @SerializedName("text")
            val text: WrappedText,
            @SerializedName("actions")
            val actions: List<PolyAction>
        )
    }

    data class RichLink(
        @SerializedName("fallbackText")
        val fallbackText: String,
        @SerializedName("payload")
        val payload: Payload
    ) : MessagePolyContent() {
        data class Payload(
            @SerializedName("media")
            val media: MediaModel,
            @SerializedName("title")
            val title: WrappedText,
            @SerializedName("url")
            val url: String
        )
    }

    object Noop : MessagePolyContent()
}
