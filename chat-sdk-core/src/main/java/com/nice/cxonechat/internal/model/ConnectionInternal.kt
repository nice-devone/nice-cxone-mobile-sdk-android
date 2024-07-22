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

package com.nice.cxonechat.internal.model

import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.state.Environment
import java.util.UUID

internal data class ConnectionInternal(
    override val brandId: Int,
    override val channelId: String,
    override val firstName: String,
    override val lastName: String,
    override val customerId: String?,
    override val environment: Environment,
    override val visitorId: UUID,
) : Connection {

    override fun toString() = buildString {
        append("Connection(brandId=")
        append(brandId)
        append(", channelId='")
        append(channelId)
        append("', firstName='")
        append(firstName)
        append("', lastName='")
        append(lastName)
        append("', customerId=")
        append(customerId)
        append(", environment=")
        append(environment)
        append("', visitorId=")
        append(visitorId)
        append(")")
    }
}
