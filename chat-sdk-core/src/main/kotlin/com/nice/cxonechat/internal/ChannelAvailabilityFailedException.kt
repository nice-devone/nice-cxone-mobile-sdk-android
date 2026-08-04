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
 * Thrown by [ChatLiveChat]'s availability check when the pre-socket `getChannelAvailability()` REST
 * call fails with a [java.io.IOException] (e.g. [java.net.UnknownHostException] while offline).
 *
 * Unlike [TransientConnectFailureException] (socket-open failures) or
 * [PermanentConnectionFailureException] (non-retriable auth failures), this failure happens before a
 * WebSocket is ever opened, so [ReconnectingListener] never sees it. [ChatInstanceProvider] catches
 * this type specifically to schedule an automatic, backoff-limited retry via
 * [PreSocketReconnectScheduler].
 */
internal class ChannelAvailabilityFailedException(
    message: String,
    cause: Throwable,
) : Exception(message, cause)
