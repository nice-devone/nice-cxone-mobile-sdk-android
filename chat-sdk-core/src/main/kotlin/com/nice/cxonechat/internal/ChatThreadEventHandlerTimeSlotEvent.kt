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

import com.nice.cxonechat.ChatThreadEventHandler
import com.nice.cxonechat.ChatThreadHandler
import com.nice.cxonechat.EventResponse
import com.nice.cxonechat.event.thread.ChatThreadEvent
import com.nice.cxonechat.event.thread.TimeSlotEvent

/**
 * This class is responsible for handling time slot events, which
 * should be sent as a message in the chat thread.
 */
internal class ChatThreadEventHandlerTimeSlotEvent(
    private val handler: ChatThreadEventHandler,
    private val thread: ChatThreadHandler,
    private val chat: ChatWithParameters,
) : ChatThreadEventHandler by handler {

    override suspend fun trigger(
        event: ChatThreadEvent,
    ): EventResponse? {
        if (event is TimeSlotEvent) {
            thread.messages().send(
                message = event.getModel(thread.get(), chat.connection)
            )
            return EventResponse.Success
        } else {
            return handler.trigger(event)
        }
    }
}
