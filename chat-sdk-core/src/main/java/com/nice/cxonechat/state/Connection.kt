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

package com.nice.cxonechat.state

import com.nice.cxonechat.Public
import java.util.UUID

/**
 * Definition of a data object, which is holding information relevant to
 * the lifecycle of SDK connection to the backend.
 */
@Public
interface Connection {
    /**
     * The id of the brand currently active in the instance of a chat, defined as integer.
     */
    val brandId: Int

    /**
     * The id of the channel currently connected to this instance.
     */
    val channelId: String

    /**
     * First name of the customer connected to this instance.
     * It can be empty if the customer is not yet authorized.
     */
    val firstName: String

    /**
     * The last name of the customer connected to this instance.
     * It can be empty if the customer is not yet authorized.
     */
    val lastName: String

    /**
     * The id of the customer connected to this instance.
     * It's automatically generated and not empty once connected
     * to the supporting socket for the first time.
     *
     * It may be changed by value originating from backend, e.g. in case of OAuth Authorization.
     */
    val customerId: String?

    /**
     * The environment through which this instance connected.
     */
    val environment: Environment

    /**
     * The internal unique id of installation instance.
     */
    val visitorId: UUID
}
