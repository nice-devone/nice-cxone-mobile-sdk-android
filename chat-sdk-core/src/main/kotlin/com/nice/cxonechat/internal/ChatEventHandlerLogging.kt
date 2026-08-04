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

package com.nice.cxonechat.internal

import com.nice.cxonechat.ChatEventHandler
import com.nice.cxonechat.event.ChatEvent
import com.nice.cxonechat.log.Logger
import com.nice.cxonechat.log.LoggerScope
import com.nice.cxonechat.log.duration
import com.nice.cxonechat.log.scope
import com.nice.cxonechat.log.verbose
import com.nice.cxonechat.log.warning
import kotlinx.coroutines.CancellationException

internal class ChatEventHandlerLogging(
    private val origin: ChatEventHandler,
    private val logger: Logger,
) : ChatEventHandler, LoggerScope by LoggerScope<ChatEventHandler>(logger) {

    override suspend fun trigger(event: ChatEvent<*>) = scope("trigger") {
        verbose("Dispatching (event=$event)")
        duration {
            try {
                origin.trigger(event)
            } catch (cancelled: CancellationException) {
                verbose("Dispatch cancelled (event=$event)")
                throw cancelled
            } catch (logged: Exception) {
                warning("Failed to dispatch (event=$event)", logged)
                throw logged
            }
        }
    }
}
