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

import com.nice.cxonechat.event.ChatEvent
import com.nice.cxonechat.exceptions.CXoneException

/**
 * Event handler allows for triggering events regarding the overall [Chat]
 * instance.
 * It has no side effects attached to it and can be created again on demand.
 */
@Public
interface ChatEventHandler {

    /**
     * Sends an [event] to the server without further delays.
     *
     * Analytics events are fire-and-forget telemetry: a dispatch failure is logged and swallowed,
     * so this method does not throw for them. Other (WebSocket) events still throw on failure.
     *
     * @param event [ChatEvent] subclass which generates an event model.
     * @throws CXoneException if a non-analytics event fails to send.
     */
    suspend fun trigger(event: ChatEvent<*>)
}
