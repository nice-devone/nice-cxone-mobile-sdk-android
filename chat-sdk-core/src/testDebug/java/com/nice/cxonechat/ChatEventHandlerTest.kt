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

import com.nice.cxonechat.event.ChatEvent
import com.nice.cxonechat.server.ServerRequest
import com.nice.cxonechat.server.ServerResponse
import io.mockk.every
import kotlinx.serialization.Serializable
import org.junit.Test
import java.util.Date

internal class ChatEventHandlerTest : AbstractChatTest() {

    private lateinit var events: ChatEventHandler

    override fun prepare() {
        super.prepare()
        events = chat.events()
        this serverResponds ServerResponse.ConsumerAuthorized()
    }

    // ---

    @Test
    fun trigger_sendExpectedMessage() {
        assertSendText(
            """
            {
                "field": 104
            }
            """.trimIndent()
        ) {
            events.trigger(ChatEvent.Custom { _, _ -> TestValue() })
        }
    }

    @Test
    fun trigger_refreshesToken() {
        every { storage.authTokenExpDate } returns Date()
        assertSendTexts(
            ServerRequest.RefreshToken(connection),
            """
            {
                "field": 104
            }
            """.trimIndent()
        ) {
            events.trigger(ChatEvent.Custom { _, _ -> TestValue() })
        }
    }

    @Serializable
    data class TestValue(
        val field: Int = 104,
    )
}
