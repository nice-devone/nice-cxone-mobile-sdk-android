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

package com.nice.cxonechat.internal.model.network

import com.google.gson.annotations.SerializedName
import com.nice.cxonechat.util.plus
import java.util.Date
import kotlin.time.Duration.Companion.seconds

/**
 * An access token used by the customer for sending messages if OAuth authorization is on for the
 * channel.
 */
internal data class AccessToken(
    @SerializedName("token")
    val token: String,
    @SerializedName("expiresIn")
    private val expiresIn: Long,
) {

    private val createdAt = Date()
    val expiresAt = createdAt + expiresIn.seconds.inWholeMilliseconds

    /** Whether the token has expired or not. */
    val isExpired
        get() = Date().after(expiresAt)
}
