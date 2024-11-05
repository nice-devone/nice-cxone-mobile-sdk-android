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

import com.nice.cxonechat.ChatEventHandler
import com.nice.cxonechat.ChatEventHandler.OnEventErrorListener
import com.nice.cxonechat.ChatEventHandler.OnEventSentListener
import com.nice.cxonechat.event.ChatEvent
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.duration
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.verbose
import com.nice.cxonechat.log.warning

internal class ChatEventHandlerLogging(
    private val origin: ChatEventHandler,
    private val logger: Logger,
) : ChatEventHandler, LoggerScope by LoggerScope<ChatEventHandler>(logger) {

    override fun trigger(
        event: ChatEvent<*>,
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
                },
                errorListener = { exception ->
                    scope("onError") {
                        warning("Failed to dispatch (event=$event)", exception)
                        errorListener?.onError(exception)
                    }
                }
            )
        }
    }
}
