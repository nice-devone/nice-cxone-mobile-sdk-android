package com.nice.cxonechat.model

import com.nice.cxonechat.internal.model.CustomerIdentityModel
import com.nice.cxonechat.tool.nextString
import java.util.UUID

internal fun makeCustomerIdentity(
    idOnExternalPlatform: UUID = UUID.randomUUID(),
    firstName: String? = nextString(),
    lastName: String? = nextString(),
) = CustomerIdentityModel(
    idOnExternalPlatform = idOnExternalPlatform,
    firstName = firstName,
    lastName = lastName
)
