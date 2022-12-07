package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.enums.MessageContentType
import com.nice.cxonechat.enums.MessageContentType.Text

internal data class MessageContent(
    /**
     * This message's type. It can have various types on
     * which [MessagePayload] content depends.
     *
     * @see MessageContentType
     * */
    @SerializedName("type")
    val type: MessageContentType,
    /**
     * Message contents sent to the remote agent/server.
     * It can have various elements depending on [MessageContentType]
     * and its supported parameters.
     *
     * @see MessageContentType
     * @see MessageElement
     * */
    @SerializedName("payload")
    val payload: MessagePayload,
    /**
     * Text to fall back to when payload is invalid or currently
     * being loaded. The use-cases where this text is displayed
     * is generally undefined.
     * */
    @SerializedName("fallbackText")
    val fallbackText: String? = null,
) {

    constructor(message: String) : this(
        type = Text,
        payload = MessagePayload(text = message)
    )

}
