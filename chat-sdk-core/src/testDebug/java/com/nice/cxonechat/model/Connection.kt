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
