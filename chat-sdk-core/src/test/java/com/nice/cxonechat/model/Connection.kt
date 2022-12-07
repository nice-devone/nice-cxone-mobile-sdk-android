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
    consumerId: UUID = UUID.randomUUID(),
    environment: Environment = CXOneEnvironment.values().random().value,
) = ConnectionInternal(
    brandId = brandId,
    channelId = channelId,
    firstName = firstName,
    lastName = lastName,
    consumerId = consumerId,
    environment = environment
)
