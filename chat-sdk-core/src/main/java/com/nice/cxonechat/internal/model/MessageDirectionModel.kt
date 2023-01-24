package com.nice.cxonechat.internal.model

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.message.MessageDirection

/**
 * Message direction from backend POV.
 */
internal enum class MessageDirectionModel(val value: String) {

    /**
     * Message inbound to the backend - usually from client.
     */
    @SerializedName("inbound")
    ToAgent("inbound"),

    /**
     * Message outbound from backend - either from agent or bot.
     */
    @SerializedName("outbound")
    ToClient("outbound");

    fun toMessageDirection() = when (this) {
        ToAgent -> MessageDirection.ToAgent
        ToClient -> MessageDirection.ToClient
    }

}
