package com.nice.cxonechat.internal.model

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.message.MessageDirection

internal enum class MessageDirectionModel(val value: String) {
    @SerializedName("inbound")
    FromApp("inbound"),

    @SerializedName("outbound")
    ToApp("outbound");

    fun toMessageDirection() = when (this) {
        FromApp -> MessageDirection.FromApp
        ToApp -> MessageDirection.ToApp
    }

}
