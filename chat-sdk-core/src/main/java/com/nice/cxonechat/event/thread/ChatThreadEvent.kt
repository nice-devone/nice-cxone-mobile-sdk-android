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

package com.nice.cxonechat.event.thread

import com.nice.cxonechat.ChatThreadEventHandler
import com.nice.cxonechat.Public
import com.nice.cxonechat.state.Connection
import com.nice.cxonechat.thread.ChatThread

/**
 * Base class for chat events. It's responsible for generating thread specific
 * models in order to dispatch them to the server.
 *
 * Clients are not permitted to extend this class, though, you might freely use
 * its implementations.
 *
 * @see ChatThreadEventHandler.trigger
 * */
@Public
sealed class ChatThreadEvent {

    internal abstract fun getModel(
        thread: ChatThread,
        connection: Connection,
    ): Any

    internal class Custom(
        private val factory: (thread: ChatThread) -> Any,
    ) : ChatThreadEvent() {
        override fun getModel(thread: ChatThread, connection: Connection): Any = factory(thread)
    }
}
