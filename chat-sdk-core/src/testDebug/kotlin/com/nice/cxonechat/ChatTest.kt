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

import com.nice.cxonechat.ChatMode.LiveChat
import com.nice.cxonechat.ChatMode.MultiThread
import com.nice.cxonechat.ChatMode.SingleThread
import com.nice.cxonechat.internal.ChatImpl
import com.nice.cxonechat.internal.model.AvailabilityStatus
import com.nice.cxonechat.internal.model.ChannelConfiguration
import com.nice.cxonechat.internal.model.ConfigurationInternal
import com.nice.cxonechat.internal.model.Visitor
import com.nice.cxonechat.internal.socket.WebSocketSpec
import com.nice.cxonechat.tool.nextString
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatTest : AbstractMultiThreadChatTest() {

    private var isAuthorizationEnabled = true

    override val config: ChannelConfiguration?
        get() = super.config?.copy(
            isAuthorizationEnabled = isAuthorizationEnabled
        )

    @Test
    fun setDeviceToken_sendsExpectedMessage() = runTest(dispatcher) {
        val token = nextString()

        chat.setDeviceToken(token)
        testScheduler.advanceUntilIdle() // Await for background tasks to finish before verification
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
                visitor = Visitor(connection, deviceToken = token)
            )
        }

        confirmVerified(service)
    }

    @Test
    fun setDeviceToken_ignoresKnownToken() = runTest(dispatcher) {
        val token = nextString()
        // Let's pretend that stored value equals to value which will be set
        every { storage.deviceToken } returns token

        chat.setDeviceToken(token)
        testScheduler.advanceUntilIdle() // Await for background tasks to finish before verification
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
    fun signOut_clearsStorage() = runTest(UnconfinedTestDispatcher()) {
        every { socket.close(any(), any()) } returns true
        coEvery { entrails.cookieJar.clearAllCookies() } returns Unit

        chat.signOut()

        coVerify { storage.clearStorage() }
    }

    @Test
    fun signOut_closesConnection() = runTest(UnconfinedTestDispatcher()) {
        every { socket.close(any(), any()) } returns true
        coEvery { entrails.cookieJar.clearAllCookies() } returns Unit

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

    @Test
    fun getChannelAvailability_returns_true_in_Messaging_mode() = runTest(dispatcher) {
        isAuthorizationEnabled = false
        chatAvailability = AvailabilityStatus.Offline
        prepare()
        val result = chat.getChannelAvailability()
        assertTrue("Messaging mode should report channel available when in messaging mode", result)
    }

}
