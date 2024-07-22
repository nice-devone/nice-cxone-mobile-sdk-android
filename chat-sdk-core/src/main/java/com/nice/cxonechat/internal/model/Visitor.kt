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

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.internal.model.network.CustomVariable
import com.nice.cxonechat.internal.model.network.DeviceFingerprint
import com.nice.cxonechat.internal.model.network.Journey
import com.nice.cxonechat.state.Connection

// Visitor

/**
 * All information about a visitor.
 */
internal data class Visitor(
    @SerializedName("customerIdentity")
    val customerIdentity: CustomerIdentityModel? = null,
    @SerializedName("browserFingerprint")
    val deviceFingerprint: DeviceFingerprint,
    @SerializedName("journey")
    val journey: Journey? = null,
    @SerializedName("customVariables")
    val customVariables: List<CustomVariable>? = null,
) {
    constructor(
        connection: Connection,
        deviceToken: String? = null,
    ) : this(
        customerIdentity = connection.asCustomerIdentity().redacted(),
        deviceFingerprint = DeviceFingerprint(deviceToken = deviceToken)
    )
}
