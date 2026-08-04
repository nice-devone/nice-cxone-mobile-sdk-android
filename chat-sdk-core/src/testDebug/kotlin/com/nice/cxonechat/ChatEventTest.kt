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

@file:Suppress("FunctionMaxLength")

package com.nice.cxonechat

import com.nice.cxonechat.ChatEventHandlerActions.customVisitor
import com.nice.cxonechat.ChatEventHandlerActions.event
import com.nice.cxonechat.server.ServerRequest
import com.nice.cxonechat.server.ServerRequest.StoreVisitorEvents
import org.junit.Test
import java.util.UUID

internal class ChatEventTest : AbstractMultiThreadChatTest() {

    private lateinit var events: ChatEventHandler

    override fun prepare() {
        super.prepare()
        events = chat.events()
    }

    // ---

    @Test
    fun trigger_CustomVisitorEvent_sendsExpectedMessage() {
        val data = "foo bar"
        val eventData = StoreVisitorEvents.CustomVisitorEvent(data)
        assertSendText(ServerRequest.StoreVisitorEvent(connection, eventData), replaceDate = true) {
            events.customVisitor(data)
        }
    }

    @Test
    fun trigger_TriggerEvent_sendsExpectedMessage() {
        val id = UUID.randomUUID()
        assertSendText(ServerRequest.ExecuteTrigger(connection, id), id.toString()) {
            events.event(id)
        }
    }
}
