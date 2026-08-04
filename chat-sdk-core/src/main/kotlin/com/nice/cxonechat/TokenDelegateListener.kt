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

import com.nice.cxonechat.exceptions.RuntimeChatException

/**
 * Reason why the SDK is requesting a new token from the integrator.
 *
 * Passed to [TokenDelegateListener.onNewTokenRequested] so that integrators can log or
 * handle token acquisition differently based on context (e.g. silent refresh vs. full
 * re-login). Implementations are free to ignore the reason and always perform the same
 * token-fetch action.
 */
@Public
enum class TokenRequestReason {
    /** No specific reason; used as a safe default for forward-compatible SDK versions. */
    UNSPECIFIED,

    /** The backend rejected the stored JWT via HTTP 401; a fresh token is required. */
    TOKEN_INVALID,

    /** The current token expired during an active session before an outgoing event was sent. */
    TOKEN_EXPIRED,
}

/**
 * Provider for implicit OAuth token delegation.
 *
 * When registered via [ChatBuilder.setTokenDelegateListener], the SDK delegates all token
 * acquisition to the integrator instead of managing OAuth internally. The integrator is
 * responsible for obtaining JWT access tokens from their OAuth provider (e.g. Auth0, Apple,
 * Google) and returning them directly from [onNewTokenRequested].
 *
 * This interface is strictly for the **implicit OAuth flow**. In this flow the integrator
 * performs the full OAuth exchange with their provider and hands the finished JWT — together
 * with its expiry — to the SDK via [OAuthToken]. The SDK uses the expiry to schedule proactive
 * token refresh before events are blocked by an expired credential.
 *
 * The **explicit flow** (auth code + code verifier via [ChatBuilder.setAuthorization]) does
 * not use this callback — the backend handles token exchange internally and no delegate
 * is required.
 *
 * ## Expected usage
 *
 * 1. Implement this interface and pass it to [ChatBuilder.setTokenDelegateListener].
 * 2. When [onNewTokenRequested] fires on a background thread, perform a blocking OAuth token
 *    fetch and return an [OAuthToken] containing the JWT and its expiry timestamp. If the
 *    token cannot be obtained, throw any exception — the SDK will catch it and report a
 *    [com.nice.cxonechat.exceptions.RuntimeChatException.TokenDelegationFailedException],
 *    terminating the Chat scope.
 */
@Public
fun interface TokenDelegateListener {

    /**
     * Called on a background thread when the SDK needs a new access token from the integrator.
     *
     * The implementation must block until the token is available from the OAuth provider (or
     * until the attempt fails). [reason] indicates why the token is needed and can be used for
     * logging or to decide between a silent refresh and a full re-login, but can otherwise be
     * ignored — the expected response is always a fresh [OAuthToken].
     *
     * Return an [OAuthToken] with the JWT string and its expiry to let the SDK proceed. To
     * abort, throw any exception — the SDK catches all throwables, wraps
     * non-[RuntimeChatException.TokenDelegationFailedException] causes into one (preserving the
     * original as [Throwable.cause]), and reports the result via
     * [com.nice.cxonechat.ChatStateListener.onChatRuntimeException].
     *
     * @param reason Why the SDK is requesting a new token.
     * @return A valid [OAuthToken] containing the access token and its expiry millis.
     * @throws RuntimeChatException.TokenDelegationFailedException if the token cannot be obtained
     *   (any other throwable is also accepted and will be wrapped).
     */
    @Throws(RuntimeChatException.TokenDelegationFailedException::class)
    fun onNewTokenRequested(reason: TokenRequestReason): OAuthToken
}
