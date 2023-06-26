package com.nice.cxonechat.internal.model

import com.nice.cxonechat.exceptions.MissingCustomerId
import com.nice.cxonechat.state.Connection

internal fun Connection.requireCustomerId() =
    customerId ?: throw MissingCustomerId()

internal fun Connection.asBrand() =
    Brand(brandId)

internal fun Connection.asChannelId() =
    ChannelIdentifier(channelId)

internal fun Connection.asCustomerIdentity() = CustomerIdentityModel(
    idOnExternalPlatform = requireCustomerId(),
    firstName = firstName,
    lastName = lastName
)
