package com.nice.cxonechat.socket

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.enums.EventType

internal data class EventBlueprint(
    @SerializedName("eventType")
    val type: EventType?,
    @SerializedName("postback")
    val postback: Postback?
) {

    val anyType
        get() = type ?: postback?.type

    data class Postback(
        @SerializedName("eventType")
        val type: EventType?
    )

}
