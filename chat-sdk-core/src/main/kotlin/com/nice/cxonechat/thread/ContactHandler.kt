/*
 * Copyright (c) 2026. NICE Ltd. All rights reserved.
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

package com.nice.cxonechat.thread

import com.nice.cxonechat.Public

/**
 * Who currently handles the contact of a [ChatThread].
 *
 * Treat both [Cognigy] and [Unknown] as "not handled by CXone".
 */
@Public
enum class ContactHandler {
    /** CXone routing, i.e. a queue or a human agent. */
    CXone,

    /** An automated Cognigy bot. */
    Cognigy,

    /** A handler this SDK version does not recognise. */
    Unknown;

    internal companion object {
        /** Maps the wire value; `null` stays `null`, an unrecognised value becomes [Unknown]. */
        fun from(value: String?): ContactHandler? = when {
            value == null -> null
            value.equals("cxone", ignoreCase = true) -> CXone
            value.equals("cognigy", ignoreCase = true) -> Cognigy
            else -> Unknown
        }
    }
}
