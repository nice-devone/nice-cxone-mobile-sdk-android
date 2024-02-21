package com.nice.cxonechat.model

import com.nice.cxonechat.enums.CXOneEnvironment
import com.nice.cxonechat.internal.model.ConnectionInternal
import com.nice.cxonechat.state.Environment
import com.nice.cxonechat.tool.nextString
import java.util.UUID
import kotlin.random.Random.Default.nextInt

@Suppress("LongParameterList")
internal fun makeConnection(
    brandId: Int = nextInt(0, 9999),
    channelId: String = nextString(),
    firstName: String = nextString(),
    lastName: String = nextString(),
    customerId: String = UUID.randomUUID().toString(),
    environment: Environment = CXOneEnvironment.entries.random().value,
    visitorId: UUID = UUID.randomUUID(),
) = ConnectionInternal(
    brandId = brandId,
    channelId = channelId,
    firstName = firstName,
    lastName = lastName,
    customerId = customerId,
    environment = environment,
    visitorId = visitorId,
)
