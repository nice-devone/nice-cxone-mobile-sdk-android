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

package com.nice.cxonechat

import com.nice.cxonechat.exceptions.RuntimeChatException

/**
 * Sealed interface representing all possible chat state events.
 */
@Public
sealed interface ChatStateEvent {
    /** Emitted when an unexpected disconnection occurs. */
    @Public
    data object UnexpectedDisconnect : ChatStateEvent

    /** Emitted when the chat is connecting or reconnecting. */
    @Public
    data object Connecting : ChatStateEvent

    /** Emitted when the chat connection is established. */
    @Public
    data object Connected : ChatStateEvent

    /** Emitted when the chat is ready for use. */
    @Public
    data object Ready : ChatStateEvent

    /** Emitted when a runtime exception occurs. */
    @Public
    interface RuntimeException : ChatStateEvent {
        /** The runtime exception that occurred. */
        val exception: RuntimeChatException
    }
}
