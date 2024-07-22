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

@file:Suppress("FunctionMaxLength")

package com.nice.cxonechat

import com.nice.cxonechat.ChatMode.LiveChat
import com.nice.cxonechat.ChatMode.MultiThread
import com.nice.cxonechat.ChatMode.SingleThread
import com.nice.cxonechat.enums.ErrorType.ConsumerReconnectionFailed
import com.nice.cxonechat.enums.ErrorType.TokenRefreshingFailed
import com.nice.cxonechat.exceptions.RuntimeChatException
import com.nice.cxonechat.internal.ChatImpl
import com.nice.cxonechat.internal.model.ChannelConfiguration
import com.nice.cxonechat.internal.model.ConfigurationInternal
import com.nice.cxonechat.internal.model.Visitor
import com.nice.cxonechat.internal.socket.WebSocketSpec
import com.nice.cxonechat.server.ServerResponse
import com.nice.cxonechat.tool.nextString
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

internal class ChatTest : AbstractChatTest() {

    private var isAuthorizationEnabled = true

    override val config: ChannelConfiguration?
        get() = super.config?.copy(
            isAuthorizationEnabled = isAuthorizationEnabled
        )

    @Test
    fun setDeviceToken_sendsExpectedMessage() {
        val token = nextString()

        chat.setDeviceToken(token)

        verifyOrder {
            service.getChannel(any(), any())
            service.createOrUpdateVisitor(
                brandId = connection.brandId,
                visitorId = connection.visitorId.toString(),
                visitor = Visitor(connection)
            )
            storage.deviceToken = token
            service.createOrUpdateVisitor(
                brandId = connection.brandId,
                visitorId = connection.visitorId.toString(),
                visitor = Visitor(connection, token)
            )
        }

        confirmVerified(service)
    }

    @Test
    fun setDeviceToken_ignoresKnownToken() {
        val token = nextString()
        // Let's pretend that stored value equals to value which will be set
        every { storage.deviceToken } returns token

        chat.setDeviceToken(token)

        verifyOrder {
            service.getChannel(any(), any())
            service.createOrUpdateVisitor(
                brandId = connection.brandId,
                visitorId = connection.visitorId.toString(),
                visitor = Visitor(connection)
            )
        }

        verify(exactly = 0) { storage.deviceToken = any() }

        confirmVerified(service)
    }

    @Test
    fun signOut_clearsStorage() {
        every { socket.close(any(), any()) } returns true

        chat.signOut()

        verify { storage.clearStorage() }
    }

    @Test
    fun signOut_closesConnection() {
        every { socket.close(any(), any()) } returns true

        chat.signOut()

        verify { socket.close(WebSocketSpec.CLOSE_NORMAL_CODE, null) }
    }

    @Test
    fun close_performsActions() {
        every { socket.close(any(), any()) } returns true

        chat.close()

        verify {
            socket.close(WebSocketSpec.CLOSE_NORMAL_CODE, null)
        }
    }

    @Test
    fun build_authorization_updatesStorage_token() {
        val token = nextString()
        this serverResponds ServerResponse.TokenRefreshed(accessToken = token)
        verify { storage.authToken = token }
    }

    @Test
    fun build_authorization_updatesStorage_tokenExpDate() {
        this serverResponds ServerResponse.TokenRefreshed()
        verify { storage.authTokenExpDate = any() }
    }

    @Test
    fun build_authorization_notifies_about_token_refresh_failure() {
        assertTrue(chatStateListener.onChatRuntimeExceptions.isEmpty())
        this serverResponds ServerResponse.ErrorResponse(TokenRefreshingFailed.value)
        assertEquals(1, chatStateListener.onChatRuntimeExceptions.size)
        assertTrue(chatStateListener.onChatRuntimeExceptions.last() is RuntimeChatException.AuthorizationError)
    }

    @Test
    fun build_authorization_notifies_about_consumer_reconnect_failure() {
        assertTrue(chatStateListener.onChatRuntimeExceptions.isEmpty())
        this serverResponds ServerResponse.ErrorResponse(ConsumerReconnectionFailed.value)
        assertEquals(1, chatStateListener.onChatRuntimeExceptions.size)
        assertTrue(chatStateListener.onChatRuntimeExceptions.last() is RuntimeChatException.AuthorizationError)
    }

    @Test
    fun setUserName_updates_connection_in_no_auth_mode() {
        isAuthorizationEnabled = false
        prepare()
        val firstName = "testFirstName"
        val lastName = "testLastName"
        assertNotEquals(firstName, connection.firstName)
        assertNotEquals(lastName, connection.lastName)
        chat.setUserName(firstName, lastName)
        assertEquals(firstName, connection.firstName)
        assertEquals(lastName, connection.lastName)
    }

    @Test
    fun setUserName_is_ignored_in_OAuth_mode() {
        isAuthorizationEnabled = true
        prepare()
        val firstName = "testFirstName"
        val lastName = "testLastName"
        val originalFirstName = connection.firstName
        val originalLastName = connection.lastName
        assertNotEquals(firstName, originalFirstName)
        assertNotEquals(lastName, originalLastName)
        chat.setUserName(firstName, lastName)
        assertEquals(originalFirstName, connection.firstName)
        assertEquals(originalLastName, connection.lastName)
    }

    @Test
    fun chatMode_multithreaded() {
        val mockConfiguration: ConfigurationInternal = mockk {
            every { hasMultipleThreadsPerEndUser } returns true
            every { isLiveChat } returns false
            every { isOnline } returns true
        }
        val mockChat: ChatImpl = mockk {
            every { configuration } returns mockConfiguration
            every { chatMode } answers { callOriginal() }
        }

        assertEquals(mockChat.chatMode, MultiThread)
    }

    @Test
    fun chatMode_singlethreaded() {
        val mockConfiguration: ConfigurationInternal = mockk {
            every { hasMultipleThreadsPerEndUser } returns false
            every { isLiveChat } returns false
            every { isOnline } returns true
        }
        val mockChat: ChatImpl = mockk {
            every { configuration } returns mockConfiguration
            every { chatMode } answers { callOriginal() }
        }

        assertEquals(mockChat.chatMode, SingleThread)
    }

    @Test
    fun chatMode_liveChat() {
        val mockConfiguration: ConfigurationInternal = mockk {
            every { hasMultipleThreadsPerEndUser } returns false
            every { isLiveChat } returns true
            every { isOnline } returns true
        }
        val mockChat: ChatImpl = mockk {
            every { configuration } returns mockConfiguration
            every { chatMode } answers { callOriginal() }
        }

        assertEquals(mockChat.chatMode, LiveChat)
    }
}
