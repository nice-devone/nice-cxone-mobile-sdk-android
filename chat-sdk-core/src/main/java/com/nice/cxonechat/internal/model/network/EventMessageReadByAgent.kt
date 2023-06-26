package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.internal.model.MessageModel

/**
 * Event received when an agent has read a message.
 */
internal data class EventMessageReadByAgent(
    @SerializedName("data")
    val data: Data,
) {

    val threadId get() = data.message.threadIdOnExternalPlatform

    val messageId get() = data.message.idOnExternalPlatform
    val message get() = data.message.toMessage()

    data class Data(
        @SerializedName("message")
        val message: MessageModel,
    )
}
