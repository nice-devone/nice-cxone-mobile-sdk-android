package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.enums.EventType
import com.nice.cxonechat.internal.model.Brand
import com.nice.cxonechat.internal.model.ChannelIdentifier
import com.nice.cxonechat.internal.model.CustomerIdentityModel
import com.nice.cxonechat.internal.model.asBrand
import com.nice.cxonechat.internal.model.asChannelId
import com.nice.cxonechat.internal.model.asCustomerIdentity
import com.nice.cxonechat.state.Connection
import java.util.UUID

internal data class Payload<Data>(
    @SerializedName("brand")
    val brand: Brand,
    @SerializedName("channel")
    val channel: ChannelIdentifier,
    @SerializedName("data")
    val data: Data,
    @SerializedName(value = "customerIdentity", alternate = ["consumerIdentity"])
    val customerIdentity: CustomerIdentityModel,
    @SerializedName("visitor")
    val visitor: Identifier?,
    @SerializedName("destination")
    val destination: Identifier?,
    @SerializedName("eventType")
    val eventType: EventType,
) {

    constructor(
        eventType: EventType,
        connection: Connection,
        data: Data,
        destination: UUID? = null,
    ) : this(
        eventType = eventType,
        brand = connection.asBrand(),
        channel = connection.asChannelId(),
        customerIdentity = connection.asCustomerIdentity(),
        visitor = connection.visitorId.let(::Identifier),
        destination = destination?.let(::Identifier),
        data = data
    )
}
