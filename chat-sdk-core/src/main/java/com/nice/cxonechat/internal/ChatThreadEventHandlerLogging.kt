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

package com.nice.cxonechat.internal

import com.nice.cxonechat.ChatThreadEventHandler
import com.nice.cxonechat.ChatThreadEventHandler.OnEventErrorListener
import com.nice.cxonechat.ChatThreadEventHandler.OnEventSentListener
import com.nice.cxonechat.event.thread.ChatThreadEvent
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.duration
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.verbose
import com.nice.cxonechat.log.warning

internal class ChatThreadEventHandlerLogging(
    private val origin: ChatThreadEventHandler,
    private val logger: Logger,
) : ChatThreadEventHandler, LoggerScope by LoggerScope<ChatThreadEventHandler>(logger) {

    init {
        verbose("Initialized")
    }

    override fun trigger(
        event: ChatThreadEvent,
        listener: OnEventSentListener?,
        errorListener: OnEventErrorListener?,
    ) = scope("trigger") {
        verbose("Dispatching (event=$event)")
        duration {
            origin.trigger(
                event = event,
                listener = {
                    scope("onSent") {
                        listener?.onSent()
                    }
                }
            ) { exception ->
                scope("onError") {
                    warning("Failed to dispatch (event=$event)", exception)
                    errorListener?.onError(exception)
                }
            }
        }
    }
}
