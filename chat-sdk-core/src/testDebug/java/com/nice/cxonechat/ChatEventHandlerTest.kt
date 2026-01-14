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
import com.nice.cxonechat.server.ServerRequest
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.util.plus
import io.mockk.every
import kotlinx.serialization.Serializable
import org.junit.Test
import java.util.Date
import kotlin.time.Duration.Companion.seconds

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
        // Expiration must be greater than 1 second, otherwise the event will be delayed until authorization is confirmed
        every { storage.authTokenExpDate } returns Date().plus(5.seconds.inWholeMilliseconds)
        assertSendTexts(
            ServerRequest.RefreshToken(connection),
            """
            {
                "field": 104
            }
            """.trimIndent(),
            except = emptyArray<String>(),
        ) {
            events.trigger(ChatEvent.Custom { _, _ -> TestValue() })
        }
    }

    @Serializable
    data class TestValue(
        val field: Int = 104,
    )
}
