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

import com.nice.cxonechat.internal.TokenRefreshCoordinator.Companion.EXPIRY_BUFFER
import com.nice.cxonechat.util.expiresWithin
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Ensures that at most one implicit-flow token refresh is in flight at any given time.
 *
 * When multiple events arrive concurrently and all find the token expired:
 * - The **first** thread acquires the lock, re-checks expiry, and performs the refresh.
 * - All **subsequent** threads block on [withLock] until the first finishes, then re-check
 *   expiry. If the first thread already refreshed the token the re-check passes and they
 *   proceed without triggering another refresh.
 *
 * This prevents thundering-herd token refreshes while keeping the caller API simple: call
 * [refreshIfExpired] before every event send.
 */
internal class TokenRefreshCoordinator {

    /**
     * If the stored [com.nice.cxonechat.storage.ValueStorage.authTokenExpDate] is within [EXPIRY_BUFFER] of now,
     * acquire the lock and refresh.
     * If another thread is already refreshing, this call blocks until it completes,
     * then re-checks the expiry — if the token is now valid it returns without refreshing again.
     *
     * Concurrent callers are serialized via [Threading.Guards.tokenRefreshMutex]: only one HTTP
     * refresh is performed; callers that arrive while a refresh is in progress wait on the mutex
     * and then skip the refresh if the token is no longer expired.
     */
    suspend fun refreshIfExpired(chat: ChatWithParameters) {
        val expiresAt = chat.storage.authTokenExpDate ?: Instant.DISTANT_FUTURE
        if (!expiresAt.expiresWithin(EXPIRY_BUFFER)) return
        chat.guards.tokenRefreshMutex.withLock {
            // Re-check under the lock: a concurrent caller may have just completed a refresh.
            val expiresAt = chat.storage.authTokenExpDate ?: Instant.DISTANT_FUTURE
            if (expiresAt.expiresWithin(10.seconds)) TokenRefreshHelper.refreshAccessToken(chat)
        }
    }

    private companion object {
        val EXPIRY_BUFFER = 10.seconds
    }
}
