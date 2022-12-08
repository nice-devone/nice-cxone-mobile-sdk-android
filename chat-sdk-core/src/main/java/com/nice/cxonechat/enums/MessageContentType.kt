package com.nice.cxonechat.enums

import com.google.gson.annotations.SerializedName

/**
 * The different types of messages that can be sent to the WebSocket.
 */
enum class MessageContentType(val value: String) {
    /** The message is only sending text. */
    @SerializedName("TEXT")
    Text("TEXT"),

    /** The message is sending a custom plugin to be displayed. */
    @SerializedName("PLUGIN")
    Plugin("PLUGIN")
}
