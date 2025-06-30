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

package com.nice.cxonechat

import com.nice.cxonechat.enums.CXoneEnvironment
import com.nice.cxonechat.server.ServerRequest
import com.nice.cxonechat.tool.ChatEntrailsMock
import com.nice.cxonechat.tool.nextString
import com.nice.cxonechat.tool.nextStringPair
import com.nice.cxonechat.util.plus
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import java.util.Date
import java.util.UUID
import kotlin.time.Duration.Companion.days

internal class ChatThreadHandlerCreateThreadTest : AbstractChatTest() {

    val customWelcomeMessage = "Custom Welcome"

    override fun prepare() {
        storage = mockk(relaxUnitFun = true) {
            every { visitorId } returns UUID.fromString(TestUUID)
            every { customerId } returns TestUUID
            every { destinationId } returns UUID.fromString(TestUUID)
            every { welcomeMessage } returns customWelcomeMessage
            every { authToken } returns "token"
            every { authTokenExpDate } returns Date().plus(1.days.inWholeMilliseconds)
            every { deviceToken } returns null
        }
        entrails = ChatEntrailsMock(httpClient, storage, service, mockLogger(), CXoneEnvironment.EU1.value)
        super.prepare()
    }

    @Test
    fun thread_handler_creates_thread_with_customFields() {
        val customFields = mapOf(
            nextStringPair(),
            nextStringPair(),
        )
        val handler = chat.threads().create(customFields)
        val thread = handler.get()
        val message = nextString()
        assertSendTexts(
            ServerRequest.SendOutbound(connection, thread, storage, customWelcomeMessage),
            ServerRequest.SendMessage(
                connection = connection,
                thread = thread,
                storage = storage,
                message = message,
            )
        ) {
            handler.messages().send(message)
        }
    }


    // ---

}
