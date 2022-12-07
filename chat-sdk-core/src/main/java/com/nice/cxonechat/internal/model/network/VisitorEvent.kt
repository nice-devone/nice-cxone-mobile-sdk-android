package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.enums.VisitorEventType
import java.util.Date
import java.util.UUID

internal data class VisitorEvent(
    @SerializedName("type")
    val type: VisitorEventType,
    @SerializedName("id")
    val id: UUID = UUID.randomUUID(),
    @SerializedName("createdAtWithMilliseconds")
    val createdAt: Date = Date(),
    @SerializedName("data")
    val data: Any? = null,
)
