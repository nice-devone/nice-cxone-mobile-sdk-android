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

package com.nice.cxonechat.internal

import com.nice.cxonechat.AbstractChatTest
import com.nice.cxonechat.enums.ErrorType
import com.nice.cxonechat.enums.EventType.CustomerAuthorized
import com.nice.cxonechat.exceptions.RuntimeChatException.ServerCommunicationError
import com.nice.cxonechat.server.ServerResponse
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.Call
import okhttp3.Response
import org.junit.Test

internal class ChatS3EventsTest : AbstractChatTest() {
    private fun commonSetup(setup: Response.() -> Unit): Call {
        val url = "https://some.not/some/path"
        val call = mockk<Call> {
            every { execute() } returns mockk {
                setup()
            }
        }

        every { httpClient.newCall(any()) } returns call

        serverResponds(ServerResponse.EventInS3(url = url, eventType = CustomerAuthorized))

        verify {
            httpClient.newCall(match { it.url.toString() == url })
            call.execute()
        }

        return call
    }

    @Test
    fun testFailure() {
        commonSetup {
            every { isSuccessful } returns false
            every { code } returns 404
        }

        chatStateListener.onChatRuntimeExceptions shouldBe listOf(ServerCommunicationError(ErrorType.S3EventLoadFailed.value))
    }

    @Test
    fun testSuccess() {
        commonSetup {
            every { isSuccessful } returns true
            every { body } returns mockk {
                every { string() } returns ServerResponse.ConsumerAuthorized()
            }
        }

        verify {
            storage.customerId = any()
        }
    }
}
