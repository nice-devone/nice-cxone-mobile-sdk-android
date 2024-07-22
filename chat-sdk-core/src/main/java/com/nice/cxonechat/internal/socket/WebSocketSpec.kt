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

package com.nice.cxonechat.internal.socket

/**
 * Specification for WebSocket setup.
 */
internal object WebSocketSpec {
    /**
     * Status code as defined by
     *     [Section 7.4 of RFC 6455](http://tools.ietf.org/html/rfc6455#section-7.4).
     * 1000 indicates a normal closure, meaning that the purpose for which the connection
     * was established has been fulfilled.
     */
    const val CLOSE_NORMAL_CODE = 1000
}
