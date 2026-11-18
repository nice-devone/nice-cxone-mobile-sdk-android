/*
 * Copyright (c) 2021-2026. NICE Ltd. All rights reserved.
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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@Serializable
internal data class TransactionTokenModel(
    // Absent on a refresh-token-grant response, which only populates [thirdParty].
    @SerialName("accessToken")
    val transactionToken: String? = null,

    @SerialName("expiresIn")
    val expiresIn: Long? = null,

    @SerialName("customerIdentity")
    val customerIdentity: CustomerIdentityModel? = null,

    @SerialName("thirdParty")
    val thirdParty: ThirdParty? = null,

    @SerialName("createdAt")
    @kotlinx.serialization.Contextual
    val createdAt: Instant = Clock.System.now(),
) {
    @Transient
    val expiresAt: Instant? = expiresIn?.let { createdAt + it.seconds }

    /** Whether the token has expired or not. A response with no transaction token (e.g. a
     * refresh-token-grant response, which only populates [thirdParty]) is treated as expired. */
    val isExpired: Boolean
        get() = transactionToken == null || expiresAt?.let { Clock.System.now() >= it } ?: true
}
