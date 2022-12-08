package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.enums.EventType

internal data class Postback<Data>(
    @SerializedName("eventType")
    val eventType: EventType,
    @SerializedName("data")
    val data: Data,
)
