/*
 * Copyright (c) 2021-2025. NICE Ltd. All rights reserved.
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
     * Message that has been presented to the SDK, but not yet full
     * transmitted to the backend.
     *
     * Note: This message is currently not reported by the SDK, but is
     * used internally by the UI.
     */
    Sending,

    /**
     * Message has been fully sent to the backend but not yet acknowledged.
     *
     * Note: This message is currently not reported by the SDK, but is
     * used internally by the UI.
     */
    Sent,

    /** Message has been acknowledged by the backend. */
    Delivered,

    /** Message to customer has been reported as seen by the UI. */
    Seen,

    /** Message to agent has been reported as read by the backend. */
    Read,

    /**
     * Message transmission failed to send or was reported as unacceptable
     * by the backend.
     *
     * Note: This message is currently not reported by the SDK, but is
     * used internally by the UI.
     */
    FailedToDeliver,
}
