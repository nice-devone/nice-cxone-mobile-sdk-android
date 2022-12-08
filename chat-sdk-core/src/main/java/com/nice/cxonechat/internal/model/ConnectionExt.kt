package com.nice.cxonechat.internal.model

import com.nice.cxonechat.enums.CXOneChatError
import com.nice.cxonechat.state.Connection

internal fun Connection.requireConsumerId() =
    consumerId ?: throw CXOneChatError.InvalidCustomerId.value

internal fun Connection.asBrand() =
    Brand(brandId)

internal fun Connection.asChannelId() =
    ChannelIdentifier(channelId)

internal fun Connection.asCustomerIdentity() = CustomerIdentityModel(
    idOnExternalPlatform = requireConsumerId(),
    firstName = firstName,
    lastName = lastName
)
