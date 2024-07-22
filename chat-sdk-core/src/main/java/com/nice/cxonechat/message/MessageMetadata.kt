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
import java.util.Date

/**
 * Otherwise uncategorizable properties of a message.
 * */
@Public
interface MessageMetadata {
    /**
     * The date at which the message was seen (delivered) on backend.
     * Default to null if the message is freshly sent by the SDK, always non-null
     * if the message is delivered.
     */
    val seenAt: Date?

    /**
     * The date at which the message was read.
     * Defaults to null if the message is freshly sent or delivered.
     * */
    val readAt: Date?

    /** Inferred status of message. */
    val status: MessageStatus
}
