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

import com.nice.cxonechat.api.RemoteService
import com.nice.cxonechat.internal.model.ChannelAvailability
import com.nice.cxonechat.state.Connection
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import retrofit2.Call
import java.net.UnknownHostException
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

/**
 * [ChatLiveChat.getAvailability]'s offline [java.io.IOException] must stay internal to the
 * pre-socket auto-retry path and never break the public
 * [ChatLiveChat.getChannelAvailability]/[com.nice.cxonechat.Chat.getChannelAvailability] contract,
 * which documents a Boolean-only, no-throw result.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class ChatLiveChatTest {
    private lateinit var dispatcher: TestDispatcher
    private lateinit var service: RemoteService
    private lateinit var origin: ChatWithParameters
    private lateinit var chatLiveChat: ChatLiveChat

    @Before
    fun setUp() {
        dispatcher = StandardTestDispatcher()
        service = mockk()
        val connection = mockk<Connection> {
            every { brandId } returns 1
            every { channelId } returns "channel"
        }
        origin = mockk(relaxed = true) {
            every { service } returns this@ChatLiveChatTest.service
            every { entrails.threading.ioDispatcher } returns dispatcher
            every { this@mockk.connection } returns connection
        }
        chatLiveChat = ChatLiveChat(origin)
    }

    private fun stubOfflineAvailabilityCall() {
        val call = mockk<Call<ChannelAvailability>> {
            every { execute() } throws UnknownHostException("Unable to resolve host")
        }
        every { service.getChannelAvailability(any(), any()) } returns call
    }

    @Test
    fun `getChannelAvailability returns false instead of throwing when offline`() = runTest(dispatcher) {
        stubOfflineAvailabilityCall()

        val result = chatLiveChat.getChannelAvailability()

        assertFalse(result, "getChannelAvailability() must preserve its documented Boolean-only, no-throw contract")
    }

    @Test
    fun `connect still surfaces ChannelAvailabilityFailedException for the auto-retry path`() = runTest(dispatcher) {
        stubOfflineAvailabilityCall()

        assertFailsWith<ChannelAvailabilityFailedException> {
            chatLiveChat.connect()
        }
    }

    @Test
    fun `offline IOException updates isChatAvailable to false, matching the onFailure branch`() = runTest(dispatcher) {
        stubOfflineAvailabilityCall()

        chatLiveChat.getChannelAvailability()

        // "Last known" isChatAvailable must reflect a failed check, HTTP or IOException alike.
        verify { origin.isChatAvailable = false }
    }
}
