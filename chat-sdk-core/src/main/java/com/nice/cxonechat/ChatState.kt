/*
 * Copyright (c) 2021-2023. NICE Ltd. All rights reserved.
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

/**
 * Definition of possible Chat states from application point-of-view.
 */
enum class ChatState {
    /** Not yet configured enough to connect. */
    INITIAL,

    /** in the process of connecting. */
    CONNECTING,

    /** a connection has been established. */
    CONNECTED,

    /** the connection was involuntarily lost. */
    CONNECTION_LOST,

    /** the connection was closed by the user. */
    CONNECTION_CLOSED
}
