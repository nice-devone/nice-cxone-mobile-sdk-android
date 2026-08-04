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

import com.nice.cxonechat.enums.CXoneEnvironment
import com.nice.cxonechat.internal.model.TransactionTokenModel
import com.nice.cxonechat.model.makeCustomerIdentity
import com.nice.cxonechat.server.ServerRequest
import com.nice.cxonechat.tool.ChatEntrailsMock
import com.nice.cxonechat.tool.nextString
import com.nice.cxonechat.tool.nextStringPair
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import org.junit.Test
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

internal class ChatThreadHandlerCreateThreadTest : AbstractMultiThreadChatTest() {

    val customWelcomeMessage = "Custom Welcome"

    override fun prepare() {
        storage = mockk(relaxUnitFun = true) {
            every { visitorId } returns UUID.fromString(TestUUID)
            every { customerId } returns TestUUID
            every { destinationId } returns UUID.fromString(TestUUID)
            every { welcomeMessage } returns customWelcomeMessage
            every { authToken } returns "token"
            every { authTokenExpDate } returns Clock.System.now() + 1.days
            every { deviceToken } returns null
            every { transactionTokenModel } returns TransactionTokenModel(
                transactionToken = TestUUID,
                expiresIn = 3600L,
                customerIdentity = makeCustomerIdentity(idOnExternalPlatform = UUID.fromString(TestUUID))
            )
        }
        entrails = ChatEntrailsMock(
            sharedClient = httpClient,
            storage = storage,
            service = service,
            authService = authService,
            logger = mockLogger(),
            environment = CXoneEnvironment.EU1.value,
            cookieJar = cookieJar,
            coroutineScope = TestScope(dispatcher)
        )
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
            ServerRequest.SendOutbound(
                connection = connection,
                thread = thread,
                storage = storage,
                message = customWelcomeMessage
            ),
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
}
