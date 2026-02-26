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

import com.nice.cxonechat.util.plus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.Date
import kotlin.time.Duration.Companion.seconds

@Serializable
internal data class TransactionTokenModel(
    @SerialName("accessToken")
    val transactionToken: String,

    @SerialName("expiresIn")
    val expiresIn: Long,

    @SerialName("customerIdentity")
    val customerIdentity: CustomerIdentityModel? = null,

    @SerialName("thirdParty")
    val thirdParty: ThirdParty? = null,

    @SerialName("createdAt")
    @kotlinx.serialization.Contextual
    val createdAt: Date = Date(),
) {
    @Transient
    val expiresAt = createdAt + expiresIn.seconds.inWholeMilliseconds

    /** Whether the token has expired or not. */
    val isExpired: Boolean
        get() = System.currentTimeMillis() >= expiresAt.time
}
