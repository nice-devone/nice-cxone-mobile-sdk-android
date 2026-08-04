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

package com.nice.cxonechat

/**
 * Represents an OAuth access token and its expiry for the implicit OAuth flow.
 *
 * Returned from [TokenDelegateListener.onNewTokenRequested] so the SDK can both use the token
 * and track when it expires to trigger proactive refresh via the delegate.
 *
 * ## Creating an instance
 *
 * Use the companion factory:
 * ```kotlin
 * val token = OAuthToken(accessToken = "...", expiryDateMillis = System.currentTimeMillis() + 3600_000L)
 * ```
 */
@Public
interface OAuthToken {

    /**
     * The raw JWT / access token string.
     */
    val accessToken: String

    /**
     * Absolute expiry time in milliseconds since the Unix epoch (e.g. [System.currentTimeMillis]
     * + token lifetime), or `null` if the expiry is unknown.
     *
     * When non-null the SDK uses this to start a proactive refresh before the token becomes
     * invalid. When `null` the SDK treats the token as never expiring by time and only refreshes
     * on backend rejection.
     */
    val expiryDateMillis: Long?

    /**
     * Factory for creating [OAuthToken] instances.
     */
    @Public
    companion object {
        /**
         * Creates an [OAuthToken] with the given [accessToken] and optional [expiryDateMillis].
         */
        operator fun invoke(accessToken: String, expiryDateMillis: Long? = null): OAuthToken =
            object : OAuthToken {
                override val accessToken = accessToken
                override val expiryDateMillis = expiryDateMillis
                override fun toString() = "OAuthToken(accessToken=[redacted], expiryDateMillis=$expiryDateMillis)"
            }
    }
}
