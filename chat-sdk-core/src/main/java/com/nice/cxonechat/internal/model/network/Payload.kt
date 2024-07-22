/*
 * Copyright (c) 2021-2024. NICE Ltd. All rights reserved.
 *
 * Licensed under the NICE License;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://github.com/nice-devone/nice-cxone-mobile-sdk-android/blob/main/LICENSE
 *
 * TO THE EXTENT PERMITTED BY APPLICABLE LAW, THE CXONE MOBILE SDK IS PROVIDED ON
 * AN “AS IS” BASIS. NICE HEREBY DISCLAIMS ALL WARRANTIES AND CONDITIONS, EXPRESS
 * OR IMPLIED, INCLUDING (WITHOUT LIMITATION) WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT, AND TITLE.
 */

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
