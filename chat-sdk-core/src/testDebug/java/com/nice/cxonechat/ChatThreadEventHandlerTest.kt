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

package com.nice.cxonechat

import com.nice.cxonechat.event.thread.ChatThreadEvent
import com.nice.cxonechat.model.makeChatThread
import com.nice.cxonechat.server.ServerRequest
import com.nice.cxonechat.thread.ChatThread
import io.mockk.every
import kotlinx.serialization.Serializable
import org.junit.Test
import java.util.Date

internal class ChatThreadEventHandlerTest : AbstractChatTest() {

    private lateinit var thread: ChatThread
    private lateinit var events: ChatThreadEventHandler

    override fun prepare() {
        super.prepare()
        thread = makeChatThread()
        events = chat.threads().thread(thread).events()
    }

    // ---

    @Test
    fun trigger_sendsExpectedMessage() {
        val event = ChatThreadEvent.Custom { TestModel() }
        assertSendText(
        """
            {
                "field": 10
            }
        """.trimIndent()
        ) {
            events.trigger(event)
        }
    }

    @Test
    fun trigger_refreshesToken() {
        every { storage.authTokenExpDate } returns Date()
        assertSendTexts(
            ServerRequest.RefreshToken(connection),
        """
            {
                "field": 10
            }
        """.trimIndent()
        ) {
            events.trigger(ChatThreadEvent.Custom { TestModel() })
        }
    }

    @Serializable
    data class TestModel(
        val field: Int = 10,
    )
}
