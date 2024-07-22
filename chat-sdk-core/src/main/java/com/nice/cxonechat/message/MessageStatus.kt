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

package com.nice.cxonechat.message

import com.nice.cxonechat.Public

/**
 * Enumeration of possible message states, including those not reported by the SDK.
 */
@Public
enum class MessageStatus {
    /**
     * Status which can be used by UI implementation, for message passed to SDK, but not yet confirmed
     * as [Sent].
     */
    Sending,

    /** Default state of message when it has been processed by the SDK and sent to backend. */
    Sent,

    /** Status which can be used by UI implementation. */
    FailedToDeliver,

    /** Status reported when the message is reported as delivered/seen on backend. */
    Seen,

    /** Status reported when the message is reported as read. */
    Read,
}
