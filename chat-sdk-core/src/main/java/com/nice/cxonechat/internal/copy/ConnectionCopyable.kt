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

package com.nice.cxonechat.internal.copy

import com.nice.cxonechat.internal.model.ConnectionInternal
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.state.Environment
import java.util.UUID

@Suppress("LongParameterList")
internal class ConnectionCopyable(
    private val connection: Connection,
) {

    fun copy(
        brandId: Int = connection.brandId,
        channelId: String = connection.channelId,
        firstName: String = connection.firstName,
        lastName: String = connection.lastName,
        customerId: String? = connection.customerId,
        environment: Environment = connection.environment,
        visitorId: UUID = connection.visitorId,
    ) = ConnectionInternal(
        brandId = brandId,
        channelId = channelId,
        firstName = firstName,
        lastName = lastName,
        customerId = customerId,
        environment = environment,
        visitorId = visitorId,
    )

    companion object {

        fun Connection.asCopyable() =
            ConnectionCopyable(this)
    }
}
