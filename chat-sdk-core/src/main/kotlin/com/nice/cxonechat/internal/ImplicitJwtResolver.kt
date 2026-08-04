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

package com.nice.cxonechat.internal

import com.nice.cxonechat.OAuthToken
import com.nice.cxonechat.TokenRequestReason
import com.nice.cxonechat.exceptions.RuntimeChatException.TokenDelegationFailedException

/**
 * Requests a fresh [OAuthToken] from the integrator's token delegate.
 *
 * Returns [Result.success] with the token on success, or [Result.failure] with a
 * [TokenDelegationFailedException] when the delegate is missing or throws.
 */
internal fun ChatWithParameters.requestImplicitToken(
    reason: TokenRequestReason = TokenRequestReason.TOKEN_EXPIRED,
): Result<OAuthToken> {
    val delegate = tokenDelegateListener
        ?: return Result.failure(
            TokenDelegationFailedException("Expired/missing transaction token and no token delegate configured")
        )
    return runCatching { delegate.onNewTokenRequested(reason) }.fold(
        onSuccess = { Result.success(it) },
        onFailure = { cause ->
            Result.failure(
                cause as? TokenDelegationFailedException
                    ?: TokenDelegationFailedException(cause.message ?: "Token delegation failed", cause)
            )
        }
    )
}
