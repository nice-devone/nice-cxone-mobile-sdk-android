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

package com.nice.cxonechat.internal.model.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * An access token used by the customer for sending messages if OAuth authorization is on for the
 * channel.
 */
@Serializable
internal data class AccessToken(
    @SerialName("token")
    val token: String,
    @SerialName("expiresIn")
    private val expiresIn: Long,
) {

    @Transient
    private val createdAt: Instant = Clock.System.now()

    @Transient
    val expiresAt: Instant = createdAt + expiresIn.seconds

    /** Whether the token has expired or not. */
    val isExpired
        get() = Clock.System.now() > expiresAt
}
