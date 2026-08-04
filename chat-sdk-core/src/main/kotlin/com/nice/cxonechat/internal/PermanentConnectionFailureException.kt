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

import kotlinx.coroutines.CancellationException

/**
 * Thrown when a connection attempt fails permanently — e.g., an expired ThirdPartyOAuth token
 * that requires re-authentication, or a server-sent authorization failure.
 *
 * Unlike [TransientConnectFailureException] (retriable failures such as socket-open errors or
 * token API outages) or a mid-auth [kotlinx.coroutines.CancellationException] (socket drop
 * during authorization), this exception signals that the failure should NOT be retried by
 * [ReconnectingListener].
 *
 * Extends [CancellationException] so that callers of [com.nice.cxonechat.Chat.connect]
 * observe a cancelled coroutine rather than an unexpected exception type.
 */
internal class PermanentConnectionFailureException(
    message: String,
    cause: Throwable,
) : CancellationException(message) {
    init {
        // CancellationException has no (message, cause) constructor, so initCause() is required
        // to preserve the cause chain for debugging. Safe because CancellationException leaves
        // cause null at construction time.
        initCause(cause)
    }
}
