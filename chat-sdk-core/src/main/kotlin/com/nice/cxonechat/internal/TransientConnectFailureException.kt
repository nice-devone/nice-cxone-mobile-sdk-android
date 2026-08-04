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

/**
 * Thrown by [ChatImpl.connect] when a connection attempt fails transiently — either the WebSocket
 * socket failed to open (e.g. network unreachable, connection refused before the handshake
 * completes) or the token API returned an error (e.g. a 5xx server outage).
 *
 * Unlike [PermanentConnectionFailureException], this failure is retriable —
 * [ReconnectingListener] catches it and schedules a reconnect with exponential backoff.
 *
 * Unlike [kotlinx.coroutines.CancellationException], this carries no cooperative-cancellation
 * semantics: when caught by [ReconnectingListener], the caller's coroutine scope remains active
 * and the failure is treated as retriable rather than as a cancellation signal.
 */
internal class TransientConnectFailureException(
    message: String,
    cause: Throwable,
) : Exception(message, cause)
