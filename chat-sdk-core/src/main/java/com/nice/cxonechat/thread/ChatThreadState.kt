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

package com.nice.cxonechat.thread

import com.nice.cxonechat.Public

/**
 * Current state of a [ChatThread].
 */
@Public
enum class ChatThreadState {
    /** A locally created thread that has not yet been verified by the server. */
    Pending,

    /**
     * A thread that has been received from the server, probably via [ThreadListRecievedEvent],
     * but is not yet ready for use.
     */
    Received,

    /** A thread that has been received from the server and has had its metadata loaded. */
    Loaded,

    /**
     * A thread that is completely ready for use, either because it was locally created or
     * because both the metadata and thread details have been recovered.
     */
    Ready,

    /**
     * The thread was closed, no more messages or events should be sent via its handler.
     */
    Closed
}
