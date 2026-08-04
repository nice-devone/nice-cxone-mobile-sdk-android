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


import com.nice.cxonechat.ChatThreadEventHandlerActions.selectTimeSlot
import com.nice.cxonechat.ChatThreadEventHandlerActions.sendTranscript
import com.nice.cxonechat.exceptions.CXoneException
import com.nice.cxonechat.exceptions.RuntimeChatException
import com.nice.cxonechat.internal.model.TimeSlotInternal
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Instant

class ChatThreadEventHandlerActionsTest {

    @Test
    fun `selectTimeSlot triggers event and notifies sent listener`() = runTest {
        val handler = mockk<ChatThreadEventHandler>(relaxed = true)
        val timeSlot = TimeSlotInternal(id = "unique-id", duration = 3600L, startTime = Instant.fromEpochMilliseconds(0))

        coEvery { handler.trigger(any()) } returns EventResponse.Success

        val result = handler.selectTimeSlot(
            timeSlotLocalizedText = "Tuesday 2pm - 1 hour",
            timeSlot = timeSlot,
        )
        assertEquals(EventResponse.Success, result)
    }

    @Test
    fun `selectTimeSlot triggers event and notifies error listener`() = runTest {
        val handler = mockk<ChatThreadEventHandler>(relaxed = true)
        RuntimeChatException.ServerCommunicationError("Failed")
        val timeSlot = TimeSlotInternal(id = "unique-id", duration = 3600L, startTime = Instant.fromEpochMilliseconds(0))

        coEvery { handler.trigger(any()) } throws mockk<CXoneException>(relaxed = true)

        assertFailsWith<CXoneException> {
            handler.selectTimeSlot(
                timeSlotLocalizedText = "Tuesday 3pm - 1 hour",
                timeSlot = timeSlot,
            )
        }
    }

    @Test
    fun `sendTranscript returns success response`() = runTest {
        val handler = mockk<ChatThreadEventHandler>(relaxed = true)

        coEvery { handler.trigger(any()) } returns EventResponse.Success

        val result = handler.sendTranscript(email = "test@example.com")

        assertEquals(EventResponse.Success, result)
    }

    @Test
    fun `sendTranscript returns null response`() = runTest {
        val handler = mockk<ChatThreadEventHandler>(relaxed = true)

        coEvery { handler.trigger(any()) } returns null

        val result = handler.sendTranscript(email = "test@example.com")

        assertNull(result)
    }

    @Test
    fun `sendTranscript throws on error`() = runTest {
        val handler = mockk<ChatThreadEventHandler>(relaxed = true)

        coEvery { handler.trigger(any()) } throws mockk<CXoneException>(relaxed = true)

        assertFailsWith<CXoneException> {
            handler.sendTranscript(email = "test@example.com")
        }
    }
}
