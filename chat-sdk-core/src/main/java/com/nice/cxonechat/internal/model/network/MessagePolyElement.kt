package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName
import java.util.*

internal sealed class MessagePolyElement {

    // contains FILE, TITLE, TEXT or BUTTON
    data class Menu(
        @SerializedName("elements")
        val elements: List<MessagePolyElement>,
    ) : MessagePolyElement()

    data class File(
        @SerializedName("url")
        val url: String,
        @SerializedName("filename")
        val fileName: String,
        @SerializedName("mimeType")
        val mimeType: String,
    ) : MessagePolyElement()

    data class Title(
        @SerializedName("text")
        val text: String,
    ) : MessagePolyElement()

    data class Subtitle(
        @SerializedName("text")
        val text: String,
    ) : MessagePolyElement()

    data class Text(
        @SerializedName("text")
        val text: String,
        // can be of text/markdown
        @SerializedName("mimeType")
        val mimeType: String?,
    ) : MessagePolyElement()

    sealed class Button : MessagePolyElement() {
        abstract val text: String
        abstract val postback: String?
    }

    data class DeeplinkButton(
        @SerializedName("text")
        override val text: String,
        // postback represents something like an action identifier
        // it probably should be reported with some analytic action
        @SerializedName("postback")
        override val postback: String?,
        /**
         * URL can be present as an alternative source of deeplink.
         */
        @SerializedName("url")
        val url: String?,
    ) : Button() {

        val deepLink
            get() = if (postback != null) deepLinkRegex.find(postback)?.groups?.get(1)?.value else null

        private companion object {
            private val deepLinkRegex = Regex("""deepLink['":\\ ]+([a-z]+:\/\/[a-zA-Z\-_\/0-9%]+)""")
        }
    }

    data class IFrameButton(
        @SerializedName("text")
        override val text: String,
        @SerializedName("postback")
        override val postback: String?,
        @SerializedName("url")
        val url: String?
    ) : Button()

    // contains TEXT and BUTTON
    data class TextAndButtons(
        @SerializedName("elements")
        val elements: List<MessagePolyElement>,
    ) : MessagePolyElement()

    // contains TEXT and BUTTON
    data class QuickReplies(
        @SerializedName("elements")
        val elements: List<MessagePolyElement>,
    ) : MessagePolyElement()

    // contains TITLE, TEXT, BUTTON and COUNTDOWN
    data class InactivityPopup(
        @SerializedName("elements")
        val elements: List<MessagePolyElement>,
    ) : MessagePolyElement()

    data class Countdown(
        @SerializedName("variables")
        val variables: Variables,
    ) : MessagePolyElement() {

        data class Variables(
            @SerializedName("startedAt")
            val startedAt: Date,
            @SerializedName("numberOfSeconds")
            val seconds: Long,
        )
    }

    data class Custom(
        @SerializedName("variables")
        val variables: Map<String, Any?>,
        @SerializedName("text")
        val fallbackText: String?,
    ) : MessagePolyElement()

    object Noop : MessagePolyElement()

    data class SatisfactionSurvey(
        @SerializedName("elements")
        val elements: List<MessagePolyElement>,
        @SerializedName("postback")
        val postback: String?,
    ) : MessagePolyElement()
}
